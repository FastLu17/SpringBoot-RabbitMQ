package com.luxf.rabbitmq.demo.product;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * @author 小66
 * @date 2020-06-22 20:36
 **/
@RestController
public class CustomizeMessageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 自定义消息详细内容
     */
    @GetMapping("/customizeMessage")
    public void customizeMessage() {
        String correlationId = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();

        // 自定义消息详细
        String msg = "Customize Message !";
        Message build = MessageBuilder.withBody(msg.getBytes())
                .setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN)
                .setCorrelationId(correlationId).build();

        rabbitTemplate.sendAndReceive("directExchange", "direct", build);
    }
}
