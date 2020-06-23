package com.luxf.rabbitmq.demo.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * #：匹配多个或0个、
 * *：匹配一个
 * 通配符("topic.#","topic.*")的routingKey 会多次消费、
 * BindingBuilder.bind(queueMessages).to(topicExchange).with("topic.#");
 *
 * @author 小66
 * @date 2020-06-22 20:08
 **/
@Component
@RabbitListener(queues = "topic.messages")
public class ManyTopicConsumer {
    @RabbitHandler
    public void consumer(Object message) throws InterruptedException {
        Thread.sleep(2000);
        System.out.println("ManyTopicConsumer receive message = " + message);
    }
}
