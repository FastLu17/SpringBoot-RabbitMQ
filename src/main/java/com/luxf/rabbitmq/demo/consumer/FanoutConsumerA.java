package com.luxf.rabbitmq.demo.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @author 小66
 * @date 2020-06-22 20:27
 **/
@Component
@RabbitListener(queues = "fanout.A")
public class FanoutConsumerA {
    @RabbitHandler
    public void consumer(Object message) throws InterruptedException {
        Thread.sleep(2000);
        System.out.println("FanoutConsumerA receive message = " + message);
    }
}
