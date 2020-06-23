package com.luxf.rabbitmq.demo.product;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 小66
 * @date 2020-06-22 20:04
 **/
@RestController
public class TopicProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @GetMapping("/topicExchange")
    public void topicExchange() {

        String msg1 = "I am topic.message msg======";

        System.out.println("topicSender1 : " + msg1);

        this.rabbitTemplate.convertAndSend("exchange", "topic.message", msg1);

        String msg2 = "I am topic.messages2222 msg########";

        System.out.println("topicSender2 : " + msg2);

        this.rabbitTemplate.convertAndSend("topicExchange", "topic.messages", msg2);

    }
}
