package com.luxf.rabbitmq.demo.product;

import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 配置错误的 routingKey, 可以触发ReturnCallback()、
 * rabbitTemplate.convertAndSend("directExchange", "directAAA", msgString, correlationData);
 * <p>
 * 配置错误的 exchange, 可以触发ConfirmCallback(),并且 ack为false！
 * rabbitTemplate.convertAndSend("directExchangeBBBBB", "direct", msgString, correlationData);
 *
 * @author 小66
 * @date 2020-06-22 19:53
 **/
@RestController
public class DirectProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @GetMapping("/directExchange")
    public void method() {
        String msgString = "directSender :hello i am Direct Model";

        System.out.println("DirectSender Start Send：" + msgString);
        // 由于已经配置过名为"directExchange"的DirectExchange, 此处直接指定Exchange的名称即可、
        CorrelationData correlationData = new CorrelationData();
        String id = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
        System.out.println("id = " + id);
        // 消息绑定ID, 正常业务中,需要生成一个唯一的ID、
        correlationData.setId(id);
//        rabbitTemplate.convertAndSend("directExchange", "direct", msgString, correlationData);
        rabbitTemplate.convertAndSend("directExchange", "direct", msgString);
    }
}
