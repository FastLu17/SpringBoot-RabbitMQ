package com.luxf.rabbitmq.demo.consumer;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 正常情况下,死信队列 需要人工处理消费、
 *
 * @author 小66
 * @date 2020-06-23 15:45
 **/
@Component
public class DeadLetterConsumer {

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "deadLetterQueue", durable = "true"),
            exchange = @Exchange(value = "deadLetterExchange", ignoreDeclarationExceptions = "true"),
            key = "deadLetterKey"
    ))
    public void consumer(Object msg, Channel channel, Message message) {
        System.out.println("DeadLetterConsumer receive message = " + msg);
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
