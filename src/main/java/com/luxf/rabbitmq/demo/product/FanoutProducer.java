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
public class FanoutProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @GetMapping("/fanoutExchange")
    public void topicExchange() {

        String msgString = "fanoutSender ：hello i am fanout!";
        System.out.println(msgString);
        // 由于是FanoutExchange、因此routingKey 配置了也会被忽略、 监听此queue的Consumer, 全都会消费message、
        this.rabbitTemplate.convertAndSend("fanoutExchange", "fanoutRoutingKey", msgString);

    }
}
