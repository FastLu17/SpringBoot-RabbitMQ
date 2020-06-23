package com.luxf.rabbitmq.demo.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 该类初始化创建队列、交换机，并把队列绑定到交换机
 * 可以定义多个Queue, Exchange
 *
 * @author 小66
 */
@Configuration
public class RabbitQueueAndExchangeConfig {

    @Bean
    public Queue helloQueue() {
        return new Queue("helloQueue");
    }

    @Bean
    public Queue directQueue() {
        return new Queue("directQueue");
    }

    //===============以下是验证topic Exchange的队列==========
    @Bean(name = "message")
    public Queue queueMessage() {
        return new Queue("topic.message");
    }

    @Bean(name = "messages")
    public Queue queueMessages() {
        return new Queue("topic.messages");
    }
    //===============以上是验证topic Exchange的队列==========

    //===============以下是验证Fanout Exchange的队列==========
    @Bean
    public Queue aMessage() {
        return new Queue("fanout.A");
    }

    @Bean
    public Queue bMessage() {
        return new Queue("fanout.B");
    }

    @Bean
    public Queue cMessage() {
        return new Queue("fanout.C");
    }
    //===============以上是验证Fanout Exchange的队列==========

    /**
     * exchange是交换机交换机的主要作用是接收相应的消息并且绑定到指定的队列.交换机有四种类型,分别为Direct,topic,headers,Fanout.
     * <p>
     * 　　Direct是RabbitMQ默认的交换机模式,也是最简单的模式.即创建消息队列的时候,指定一个BindingKey.当发送者发送消息的时候,指定对应的Key.当Key和消息队列的BindingKey一致的时候,消息将会被发送到该消息队列中.
     * <p>
     * 　　topic转发信息主要是依据通配符,队列和交换机的绑定主要是依据一种模式(通配符+字符串),而当发送消息的时候,只有指定的Key和该模式相匹配的时候,消息才会被发送到该消息队列中.
     * <p>
     * 　　headers也是根据一个规则进行匹配,在消息队列和交换机绑定的时候会指定一组键值对规则,而发送消息的时候也会指定一组键值对规则,当两组键值对规则相匹配的时候,消息会被发送到匹配的消息队列中.
     * <p>
     * 　　Fanout是路由广播的形式,将会把消息发给绑定它的全部队列,即便设置了key,也会被忽略.
     */
    @Bean
    DirectExchange directExchange() {
        return new DirectExchange("directExchange");
    }

    @Bean
    TopicExchange topicExchange() {
        return new TopicExchange("topicExchange");
    }

    /**
     * Fanout是路由广播的形式,将会把消息发给绑定它的全部队列,  即便设置了routingKey,也会被忽略.
     *
     * @return FanoutExchange
     */
    @Bean
    FanoutExchange fanoutExchange() {
        // 1：交换器名称, 2：是否持久化, 3：是否在服务器不适用该交换器的时候,自动删除该交换器
        return new FanoutExchange("fanoutExchange", true, false);
    }

    /**
     * 绑定 Queue 和 Exchange
     *
     * @param directQueue    queue
     * @param directExchange exchange
     * @return
     */
    @Bean
    Binding bindingDirectExchange(Queue directQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(directQueue).to(directExchange).with("direct");
    }

    /**
     * 将队列topic.message与exchange绑定，routing_key为topic.message,就是完全匹配
     *
     * @param queueMessage
     * @param topicExchange
     * @return
     */
    @Bean
    Binding bindingTopicExchange(@Qualifier("message") Queue queueMessage, TopicExchange topicExchange) {
        return BindingBuilder.bind(queueMessage).to(topicExchange).with("topic.message");
    }

    /**
     * 将队列topic.messages与exchange绑定，routing_key为topic.#,模糊匹配
     *
     * @param queueMessages
     * @param topicExchange
     * @return
     */
    @Bean
    Binding bindingTopicExchangeByGeneric(@Qualifier("messages") Queue queueMessages, TopicExchange topicExchange) {
        return BindingBuilder.bind(queueMessages).to(topicExchange).with("topic.#");
    }

    @Bean
    Binding bindingFanoutExchangeA(Queue aMessage, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(aMessage).to(fanoutExchange);
    }

    @Bean
    Binding bindingFanoutExchangeB(Queue bMessage, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(bMessage).to(fanoutExchange);
    }

    @Bean
    Binding bindingFanoutExchangeC(Queue cMessage, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(cMessage).to(fanoutExchange);
    }

    @Bean
    public DirectExchange exchangeForError() {
        return new DirectExchange("exchangeForError");
    }

    @Bean
    public Queue routingKeyForError() {
        return new Queue("routingKeyForError");
    }

    @Bean
    Binding bindingExchangeForError(Queue routingKeyForError, DirectExchange exchangeForError) {
        return BindingBuilder.bind(routingKeyForError).to(exchangeForError).with("routingKeyForError");
    }

    /**
     * 可以配置MessageRecoverer对异常消息进行处理，此处理会在listener.retry次数尝试完并还是抛出异常的情况下才会调用。
     * 默认有两个实现：
     * <p>
     * 1、RepublishMessageRecoverer：将消息重新发送到指定队列, 需手动配置。
     * 2、RejectAndDontRequeueRecoverer：如果不手动配置MessageRecoverer，会默认使用这个。
     *
     * 需要主动在重测结束的地方执行 recover.recover(message, lastThrowable); --> 相当于rabbitTemplate.convertAndSend()、
     *
     * @param rabbitTemplate template
     * @return MessageRecoverer
     */
    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, "exchangeForError", "routingKeyForError");
    }

    /**
     * 配置死信队列
     */
    @Bean
    public Queue deadLetterQueue() {
        return new Queue("deadLetterQueue", true);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("deadLetterExchange", true, false);
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("deadLetterKey");
    }
}