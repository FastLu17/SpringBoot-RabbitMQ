package com.luxf.rabbitmq.demo.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * {@link RabbitListener}注解是监听队列的，当队列有消息的时候，它会自动获取。
 * <p>
 * {@link RabbitListener}标注在类上面表示当有收到消息的时候，就交给 @RabbitHandler 的方法处理，
 * 具体使用哪个方法处理，根据 MessageConverter 转换后的参数类型
 * <p>
 * 注意:
 * <p>
 * 发送消息时, 调用的是{@link RabbitTemplate#convertAndSend(Object)},消息处理方法参数是由 MessageConverter 转化。
 * <P>
 *     如果要发送实体类、则实体类必须支持序列化！
 * </P>
 * 若使用自定义 MessageConverter 则需要在 RabbitListenerContainerFactory 实例中去设置（默认 Spring 使用的实现是 SimpleRabbitListenerContainerFactory）
 * 消息的 content_type 属性表示消息 body 数据以什么数据格式存储，接收消息除了使用 Message 对象接收消息（包含消息属性等信息）之外，还可直接使用对应类型接收消息 body 内容，但若方法参数类型不正确会抛异常：
 * application/octet-stream：二进制字节数组存储，使用 byte[]
 * application/x-java-serialized-object：java 对象序列化格式存储，使用 Object、相应类型（反序列化时类型应该同包同名，否者会抛出找不到类异常）
 * text/plain：文本数据类型存储，使用 String
 * application/json：JSON 格式，使用 Object、相应类型
 *
 * @author 小66
 * @date 2020-06-22 19:24
 **/
@Component
@RabbitListener(queues = "helloQueue")
public class OneConsumer {

    @RabbitHandler
    public void consumer(Object message) throws InterruptedException {
        Thread.sleep(2000);
        System.out.println("OneConsumer receive message = " + message);
    }
}
