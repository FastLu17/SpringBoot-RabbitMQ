package com.luxf.rabbitmq.demo.product;

import com.luxf.rabbitmq.demo.config.RabbitQueueAndExchangeConfig;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * @author 小66
 * @date 2020-06-22 19:14
 **/
@RestController
public class OneProducer {

    /**
     * AmqpTemplate可以说是RabbitTemplate父类，RabbitTemplate实现类RabbitOperations接口，RabbitOperations继承了AmqpTemplate接口
     */
    @Autowired

    private AmqpTemplate amqpTemplate;

    /**
     * {@link RabbitQueueAndExchangeConfig} 中自定义的 rabbitTemplate、
     */
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 连续多次请求该生产者时,为啥 spring_listener_return_correlation 会是相同的？
     * TODO: 消费者接收的：headers={spring_listener_return_correlation=e35c5c9f-7447-4259-8cc5-c4294652b56f} 是相同的、
     */
    @GetMapping("/sendMessage")
    public void sendMessage() {
        String sendMsg = "hello1 :" + LocalDate.now();

        System.out.println("sendMessage To Exchange：" + sendMsg);
        // 没有指定Exchange时, 有个默认的Exchange、 Alias for amq.direct default exchange.
        rabbitTemplate.convertAndSend("helloQueue", sendMsg);

        // TODO: java.lang.IllegalStateException: Only one ConfirmCallback is supported by each RabbitTemplate
        // 因为配置RabbitTemplate时,已设置ConfirmCallback和ReturnCallback、因此会报错！
        // rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> System.out.println("ack = " + ack));
    }
}
