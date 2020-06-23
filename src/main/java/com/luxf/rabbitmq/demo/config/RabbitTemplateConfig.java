package com.luxf.rabbitmq.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 小66
 * @date 2020-06-23 15:29
 **/
@Configuration
public class RabbitTemplateConfig {
    private static Logger log = LoggerFactory.getLogger(RabbitTemplateConfig.class);

    private final CachingConnectionFactory connectionFactory;

    @Autowired
    public RabbitTemplateConfig(CachingConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        //若使用confirm-callback或return-callback，必须要配置publisherConfirms或publisherReturns为true
        // 已过时：--> connectionFactory.setPublisherConfirms(true)
        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);

        // 使用return-callback时必须设置mandatory为true，或者在配置中设置mandatory-expression的值为true
        connectionFactory.setPublisherReturns(true);

        // channelCacheSize 一定要大于等于目前的 consumer 个数
        connectionFactory.setChannelCacheSize(30);

        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        // 发送消息时, 设置强制标志, 仅适用于已提供ReturnCallback的情况、
        rabbitTemplate.setMandatory(true);

        /**
         *  如果消息没有到exchange, 则confirm回调, ack=false
         *  如果消息到达exchange, 则confirm回调, ack=true
         *  exchange到queue成功, 则不回调return。
         *  exchange到queue失败, 则回调return(需设置mandatory=true, 否则不回回调, 消息丢失)。
         *
         *  配置错误的 routingKey, 可以触发ReturnCallback()、
         *  rabbitTemplate.convertAndSend("directExchange", "directAAA", msgString, correlationData);
         *  <p>
         *  配置错误的 exchange, 可以触发ConfirmCallback(),并且 ack为false！
         *  rabbitTemplate.convertAndSend("directExchangeBBBBB", "direct", msgString, correlationData);
         */
        // 每个rabbitTemplate只能有一个confirm-callback和return-callback，如果这里配置了，那么写生产者的时候不能再写confirm-callback和return-callback
        // java.lang.IllegalStateException: Only one ConfirmCallback is supported by each RabbitTemplate
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("消息发送成功--> 到达exchange：correlationData({}),cause({})", correlationData, cause);
            } else {
                log.info("消息发送失败--> 未到达exchange：correlationData({}),cause({})", correlationData, cause);
                // 日志记录, 或者存入Redis、MySQL
            }
        });
        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            log.info("消息丢失：未从exchange发送到queue：exchange({}), routingKey({}), replyCode({}), replyText({}), message:{}", exchange, routingKey, replyCode, replyText, message);
            // 日志记录, 或者存入Redis、MySQL
        });

        return rabbitTemplate;
    }
}
