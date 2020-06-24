package com.luxf.rabbitmq.demo.config;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.config.AbstractRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

/**
 * 直接利用try..catch, 在catch中处理 channel.basicNack()能够让消息回到队列中,这样可以实现重试。
 * 但是没有明确重试次数, 如果当前消息, 消费失败, 一直重试, 堆积起来！--> 致命缺点！
 * <p>
 * 利用Spring Retry 完成对RabbitMQ的重测监听。
 * TODO:如果设置自动ACK、则会循环重试, 如果设置手动ACK, 则重试失败后, 消息处于'unacked'状态、目前没有找到解决办法
 * <p>
 * TODO: 如果不使用Spring Retry -> 则使用redis或者mongo等第三方存储当前重试次数。监听到消息时, 先判断重试次数, 来决定是否放入失信队列、存入数据库
 *
 * @author 小66
 * @date 2020-06-23 10:14
 **/
@Configuration
@Slf4j
public class RabbitMqReTryConfig {
    /**
     * CachingConnectionFactory是具体实现、
     */
    private final ConnectionFactory rabbitConnectionFactory;
    /**
     * 可以获取配置文件中 'spring.rabbitmq.listener.retry' 下的属性
     */
    private final RabbitProperties properties;

    private final RabbitTemplate rabbitTemplate;

    private final StringRedisTemplate redisTemplate;

    @Autowired
    private MessageRecoverer recover;

    @Autowired
    public RabbitMqReTryConfig(ConnectionFactory rabbitConnectionFactory, RabbitProperties properties, RabbitTemplate rabbitTemplate, StringRedisTemplate redisTemplate) {
        this.rabbitConnectionFactory = rabbitConnectionFactory;
        this.properties = properties;
        this.rabbitTemplate = rabbitTemplate;
        this.redisTemplate = redisTemplate;
    }

    /**
     * see {@link AbstractRabbitListenerContainerFactory} 有2个子类：
     * <p>
     * {@link DirectRabbitListenerContainerFactory}, 对应yml配置文件中的spring.rabbitmq.listener.direct.retry
     * {@link SimpleRabbitListenerContainerFactory}, 对应yml配置文件中的spring.rabbitmq.listener.simple.retry
     *
     * @return AbstractRabbitListenerContainerFactory
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory() {
        SimpleRabbitListenerContainerFactory containerFactory = new SimpleRabbitListenerContainerFactory();
        containerFactory.setConnectionFactory(rabbitConnectionFactory);
        // 并发消费者数量
        containerFactory.setConcurrentConsumers(1);
        // 最大消费者数量
        containerFactory.setMaxConcurrentConsumers(20);

        // TODO: 设置自动ACK、并且设置default-requeue-rejected=false、可以解决Spring Retry 重试失败后, 消息处于'unacked'状态的问题、
        // 设置手动ACK时, 则需要利用单例对象缓存Channel, 以便进行手动ACK,NACK回复、
        containerFactory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        // containerFactory.setDefaultRequeueRejected(false);

        // 设置自定义的消息转换器、 将消息转换为Json对象、
        // containerFactory.setMessageConverter(new Jackson2JsonMessageConverter())

        // true：开启事务, MQ一般不会使用事务
        containerFactory.setChannelTransacted(false);

        // 设置重试
        containerFactory.setAdviceChain(
                RetryInterceptorBuilder
                        // 创建无状态的拦截器、
                        .stateless()
                        // 绑定重试操作对象、
                        .retryOperations(rabbitRetryTemplate())
                        .build()
        );
        return containerFactory;
    }

    @Bean
    public RetryTemplate rabbitRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // 设置监听
        retryTemplate.registerListener(new IRetryListener());

        /**
         * 设置重试策略 和 回退策略
         * {@link RetryPolicy}和{@link BackOffPolicy}有多种实现方式, 可根据不同的需求配置
         */
        retryTemplate.setBackOffPolicy(backOffPolicyByProperties());
        retryTemplate.setRetryPolicy(retryPolicy());
        return retryTemplate;
    }

    @Bean
    public ExponentialBackOffPolicy backOffPolicyByProperties() {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        long maxInterval = properties.getListener().getSimple().getRetry().getMaxInterval().getSeconds();
        long initialInterval = properties.getListener().getSimple().getRetry().getInitialInterval().getSeconds();
        double multiplier = properties.getListener().getSimple().getRetry().getMultiplier();
        // 重试间隔
        backOffPolicy.setInitialInterval(initialInterval * 1000);
        // 重试最大间隔
        backOffPolicy.setMaxInterval(maxInterval * 1000);
        // 重试间隔乘法策略
        backOffPolicy.setMultiplier(multiplier);
        return backOffPolicy;
    }

    @Bean
    public SimpleRetryPolicy retryPolicy() {
        // 构造方法可以添加其他属性、retryableExceptions(可触发重试的异常)
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        int maxAttempts = properties.getListener().getSimple().getRetry().getMaxAttempts();
        retryPolicy.setMaxAttempts(maxAttempts);
        return retryPolicy;
    }

    /**
     * 自定义重试监听、 TODO: 目前无法对消息手动ACK, 还是使用Redis的方式存储重试失败次数
     * <p>
     * 1、设置自动ACK、并且设置default-requeue-rejected=false、可以解决Spring Retry 重试失败后, 消息处于'unacked'状态的问题、
     * <p>
     * 2、利用单例对象{@link ChannelCache}缓存Channel对象, 然后在close()方法中, 进行手动ACK、
     */
    private class IRetryListener implements RetryListener {
        @Override
        public <T, E extends Throwable> boolean open(RetryContext retryContext, RetryCallback<T, E> retryCallback) {
            // 执行之前调用、(返回false时会终止执行)
            return true;
        }

        @Override
        public <T, E extends Throwable> void close(RetryContext retryContext, RetryCallback<T, E> retryCallback, Throwable throwable) {
            // 重试结束的时候调用、(最后一次重试,如果最后重试失败, 可以配置丢入死信队列或Redis、MySQL数据库)
            Throwable lastThrowable = retryContext.getLastThrowable();
            if (lastThrowable != null && ListenerExecutionFailedException.class.isAssignableFrom(lastThrowable.getClass())) {
                ListenerExecutionFailedException exception = (ListenerExecutionFailedException) lastThrowable;
                Collection<Message> failedMessages = exception.getFailedMessages();
                //TODO: 没有对错误的消息进行ACK, 消息一直存在(unacked状态)。如何解决？--> 消费端消费时, 通过单例对象缓存Channel、
                failedMessages.forEach(message -> {
                    Object listenerCorrelationId = message.getMessageProperties().getHeaders().get(ChannelCache.LISTENER_CORRELATION_ID);
                    Channel channel = ChannelCache.INSTANCE.get(listenerCorrelationId.toString());
                    if (channel != null) {
                        try {
                            channel.basicNack(1, true, false);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        ChannelCache.INSTANCE.remove(listenerCorrelationId.toString());
                    }
                    // 方案1、丢入死信队列。
                    CorrelationData correlationData = new CorrelationData();
                    String id = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
                    correlationData.setId(id);
                    rabbitTemplate.convertAndSend("deadLetterExchange", "deadLetterKey",
                            new String(message.getBody()), correlationData);

                    // 方案2、Redis缓存、MySQL持久化。
                    // 此处的correlationId是发送消息时, 传递的唯一值, 推荐使用spring_listener_return_correlation, 会自动生成UUID、
                    Object correlationId = message.getMessageProperties().getHeaders().get(ChannelCache.MESSAGE_CORRELATION_ID);
                    redisTemplate.opsForValue().setIfAbsent("FAILED_MESSAGE_CORRELATION_ID::" + correlationId, new String(message.getBody()));
                });
            }
        }

        @Override
        public <T, E extends Throwable> void onError(RetryContext retryContext, RetryCallback<T, E> retryCallback, Throwable throwable) {
            //  异常 都会调用
            log.error("-----第{}次调用", retryContext.getRetryCount());
        }
    }

}
