package com.luxf.rabbitmq.demo.config;

import com.rabbitmq.client.Channel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用于缓存消费端消费失败时的Channel对象, 以便于 进行手动ACK,NACK处理、
 *
 * @author 小66
 * @date 2020-06-24 10:07
 **/
public enum ChannelCache {
    /**
     * 单例对象
     */
    INSTANCE;
    private static final Map<String, Channel> CHANNEL_MAP = new ConcurrentHashMap<>();
    public static final String MESSAGE_CORRELATION_ID = "spring_returned_message_correlation";
    public static final String LISTENER_CORRELATION_ID = "spring_listener_return_correlation";

    public Channel get(String id) {
        return CHANNEL_MAP.get(id);
    }

    public void putIfAbsent(String id, Channel channel) {
        CHANNEL_MAP.putIfAbsent(id, channel);
    }

    public void remove(String id) {
        CHANNEL_MAP.remove(id);
    }

    public void put(String id, Channel channel) {
        CHANNEL_MAP.put(id, channel);
    }
}
