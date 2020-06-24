package com.luxf.rabbitmq.demo.consumer;

import com.luxf.rabbitmq.demo.config.RabbitMqReTryConfig;
import com.luxf.rabbitmq.demo.config.ChannelCache;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author 小66
 * @date 2020-06-22 20:44
 **/
@Component
@Slf4j
public class ListenerOnMethodConsumer {

    private final RabbitProperties properties;
    private final RabbitTemplate rabbitTemplate;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    public ListenerOnMethodConsumer(RabbitTemplate rabbitTemplate, RabbitProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    /**
     * 如果{@link RabbitListener}配置在方法上,则不需要{@link RabbitHandler}
     * 一个Component，就可以配置多个消费者
     */
//    @RabbitListener(bindings = @QueueBinding(
//            value = @Queue(value = "directQueue", durable = "true"),
//            exchange = @Exchange(value = "directExchange", ignoreDeclarationExceptions = "true"),
//            // routingKey
//            key = "direct"
//    ))
    public void listener(String body, Channel channel, Message message) {
        log.info("当前时间：" + LocalDateTime.now() + ", 监听到一条消息 --> " + body);

        /**
         *  TODO: 如果不使用Spring Retry -> 则使用redis或者mongo等第三方存储当前重试次数。
         *   监听到方法时, 判断重试次数, 来决定是否继续重试, 还是放入死信队列、存入数据库
         */
        try {
            int random = (int) (Math.random() * 3) % 2;
            System.out.println("random = " + random);
            if (random % 2 == 0) {
                throw new RuntimeException("处理消息失败：" + message);
            }
            /**
             * TODO: Channel shutdown: reply-code=406, reply-text=PRECONDITION_FAILED - unknown delivery tag 2
             *  引起该错误的原因：因为channel 被关闭了, 但是仍然通过这个channel 回复 ack,
             *  在basicAck的时候，会把当前的channelNumber一起发送给rabbitmq server。
             *  一条消息一定不能被多次确认。
             *  接收方必须验证非零传递标签是否引用了传递的消息，如果不是这种情况，则引发信道异常。
             */
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);

        } catch (Exception e) {

            /**
             * 处理出现异常的消息统一放到另外队列去处理, 建议两种方式：
             *      1、catch异常后，手动发送到指定队列，然后使用channel给rabbitmq确认消息已消费
             *      2、给Queue绑定死信队列，使用nack（requque为false）确认消息消费失败
             */

            // 这样操作会无限重发消息, 造成阻塞、不写这个basicNack,又不会触发重发机制。--> 利用
            try {
                /**
                 * 消息变成死信有以下几种情况：
                 *  1、消息被拒绝（basic.reject/ basic.nack）并且requeue=false
                 *  2、消息TTL过期（参考：RabbitMQ之TTL（Time-To-Live 过期时间））
                 *  3、队列达到最大长度
                 */
                // 第二个参数-> multiple：true，拒绝所有消息，包括提供的传递标签； 如果仅拒绝提供的交付标签，则为false。
                // 第三个参数-> requeue：如果应重新排队被拒绝的消息，而不是丢弃/按死信队列，则为true、 消息重发！
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), true, true);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }


    /**
     * 通过Spring Retry 完成 RabbitMQ 重试功能、
     * <p>
     * 如果{@link RabbitListener}配置在方法上,则不需要{@link RabbitHandler}
     * 一个Component，就可以配置多个消费者、
     * 自定义类型的消息, 参数接收需要标注{@link Payload}, 并且类要实现序列化接口
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "directQueue", durable = "true"),
            exchange = @Exchange(value = "directExchange", ignoreDeclarationExceptions = "true"),
            // routingKey
            key = "direct"
    ))
    public void consumer(String body, Channel channel, Message message, @Headers Map<String, Object> headers) throws IOException {
        // 业务中正常不会传递String、一般是JSON格式对象, JSONObject、通过message.getBody()可以获取具体内容
        log.info("当前时间：" + LocalDateTime.now() + ", 监听到一条消息 --> " + body);
        MessageProperties messageProperties = message.getMessageProperties();

        /**
         * spring_returned_message_correlation：发送消息时,自定义的CorrelationDataID、不传则为null
         * spring_listener_return_correlation：Spring RabbitMQ 自动生成的UUID, 必定不为null、
         *
         * 缓存Channel 推荐使用 spring_listener_return_correlation, 如果业务每个消息都会自定义CorrelationDataID, 使用CorrelationDataID也可以！
         */
        Object correlationId = message.getMessageProperties().getHeaders().get(ChannelCache.MESSAGE_CORRELATION_ID);
        Object listenerCorrelationId = message.getMessageProperties().getHeaders().get(ChannelCache.LISTENER_CORRELATION_ID);

        String successMessageKey = "SUCCESS_MESSAGE_CORRELATION_ID::" + correlationId;
        String successMessage = redisTemplate.opsForValue().get(successMessageKey);
        // 判断是否重复消费已经正常消费过该消息
        if (successMessage != null) {
            channel.basicNack(messageProperties.getDeliveryTag(), true, false);
        }
        // TODO: 将当前的Channel对象缓存、 在Spring Retry重试失败次数耗尽后, 获取该Channel对象, 进行手动ACK、
        // 如果出现ChannelCache.INSTANCE.get()获取到的不是同一个Channel对象, 则使用ChannelCache.INSTANCE.put()方法覆盖、
        ChannelCache.INSTANCE.putIfAbsent(listenerCorrelationId.toString(), channel);

        /**
         * Spring Boot RabbitMQ 无法通过message获取重试次数、原生的RabbitMQ可以通过这种方式获取
         *  // 获取转发重试次数
         *  List<Map<String, ?>> xDeathHeader = messageProperties.getXDeathHeader();
         *  // 重试次数大于3次 ，丢入死信队列
         *  if (xDeathHeader != null) {
         *       Object count = xDeathHeader.get(0).get("count");
         *      int parseInt = Integer.parseInt(count.toString());
         *      int maxAttempts = properties.getListener().getSimple().getRetry().getMaxAttempts();
         *      if (parseInt >= maxAttempts) {
         *          rabbitTemplate.convertAndSend("exchangge_retry", "key", messageBody);
         *          // 无论如何, 都需要进行ACK回复、
         *          channel.basicAck(messageProperties.getDeliveryTag(),true);
         *      }
         *  }
         */

        /**
         * 以 IRetryListener 监听的方式, 重试次数达到上限之后, 无法对消息进行手动ack、消息一直处于'unacked'状态！
         * 利用单例对象缓存Channel对象, 可以解决！
         *
         * 正常手动确认ACK即可、如果业务消费失败, 会被{@link RabbitMqReTryConfig.IRetryListener}监听、进行重试
         * TODO: 使用Spring Retry时, 不要对消费者的消费方法try..catch, 直接抛出异常即可！
         */
        int i = 1 / 0;
        channel.basicAck(messageProperties.getDeliveryTag(), true);
        // 如果正常ACK, 则移除缓存的Channel对象、
        ChannelCache.INSTANCE.remove(listenerCorrelationId.toString());

        /**
         *  下面是通过Redis缓存重试失败次数的方式解决、
         */
//        try {
//            // 消息内容、进行业务消费
//            String messageBody = new String(message.getBody());
//            int i = 1 / 0;
//            channel.basicAck(messageProperties.getDeliveryTag(), true);
//            // 成功消费消息, 则进行存储, 避免重复消费的问题。
//            redisTemplate.opsForValue().setIfAbsent(successMessageKey, new String(message.getBody()));
//        } catch (Exception e) {
//            String failedMessageKey = "FAILED_MESSAGE_CORRELATION_ID::" + correlationId;
//            String failedMessage = redisTemplate.opsForValue().get(failedMessageKey);
//            // 判断是否重复消费异常消息
//            if (failedMessage != null) {
//                channel.basicNack(messageProperties.getDeliveryTag(), true, false);
//            }
//
//            String retryKey = "RETRY_COUNT::" + correlationId;
//            String count = redisTemplate.opsForValue().get(retryKey);
//            int maxAttempts = properties.getListener().getSimple().getRetry().getMaxAttempts();
//            if (count == null || Integer.parseInt(count) < maxAttempts) {
//                redisTemplate.opsForValue().increment(retryKey);
//                // 第三个参数：是否重新放入队列、
//                channel.basicNack(messageProperties.getDeliveryTag(), true, true);
//            }
//            // 这里是直接丢弃、也可以放入死信队列
//            // rabbitTemplate.convertAndSend("deadLetterExchange","deadLetterKey",msg);
//            channel.basicNack(messageProperties.getDeliveryTag(), true, false);
//            redisTemplate.opsForValue().setIfAbsent(failedMessageKey, new String(message.getBody()));
//        }
    }
}
