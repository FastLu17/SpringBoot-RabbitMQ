package com.luxf.rabbitmq.demo.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 监听绑定到DirectExchange上的Queue、
 *
 * @author 小66
 * @date 2020-06-22 19:58
 **/
@Component
@RabbitListener(queues = "routingKeyForError")
public class ErrorConsumer {

    @RabbitHandler
    public void errorConsumer(Object message) throws InterruptedException {
        Thread.sleep(2000);
        System.out.println("ErrorConsumer receive message = " + message);
    }
}