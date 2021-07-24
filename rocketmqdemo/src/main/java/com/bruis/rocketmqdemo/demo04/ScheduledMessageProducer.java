package com.bruis.rocketmqdemo.demo04;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;

/**
 *
 * 延时消息生产
 *
 * @author lhy
 * @date 2021/7/24
 */
public class ScheduledMessageProducer {

    public static final String GROUP_NAME = "scheduled_group";

    public static final String TOPIC_NAME = "scheduled_test_topic";

    public static final String NAMESRV_ADDRESS = "127.0.0.1:9876";

    public static void main(String[] args) throws Exception {

        DefaultMQProducer producer = new DefaultMQProducer(GROUP_NAME);
        producer.setNamesrvAddr(NAMESRV_ADDRESS);
        producer.start();

        int totalMessagesToSend = 100;

        for (int i = 0; i < totalMessagesToSend; i++) {
            Message message = new Message(TOPIC_NAME, ("Hello Scheduled Message " + i).getBytes());
            // 设置延时等级3,这个消息将在10s之后发送(现在只支持固定的几个时间,详看delayTimeLevel)
            message.setDelayTimeLevel(3);
            // 发送消息
            producer.send(message);
        }

        producer.shutdown();
    }

}
