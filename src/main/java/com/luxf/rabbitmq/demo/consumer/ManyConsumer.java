package com.luxf.rabbitmq.demo.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 多个消费者, 监听同一个Queue, 默认使用轮询分配消息
 *
 * @author 小66
 * @date 2020-06-22 19:42
 **/
@Component
@RabbitListener(queues = "helloQueue")
public class ManyConsumer {

    /**
     * 发送消息的类型是什么,就可以用什么类型接收、如果使用Object接收,则具有完整的相关信息
     *
     * @param message 消息
     * @throws InterruptedException
     */
    @RabbitHandler
    public void consumer(String message) throws InterruptedException {
        Thread.sleep(2000);
        System.out.println("SecondConsumer receive message = " + message);
    }
}
