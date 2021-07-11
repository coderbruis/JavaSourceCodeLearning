package com.bruis.rocketmqdemo.demo02;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

/**
 * @author lhy
 *
 * 普通生产者（异步方式发送）
 *
 * @date 2021/7/10
 */
public class Producer {

    public static final String NAMESRV_ADDRESS = "127.0.0.1:9876";

    // 生产者组
    public static final String DEMO01_PRODUCER_GROUP_NAME = "demo02_producer_group_name";

    // topic
    public static final String TOPIC_NAME = "demo02_topic";

    public static void main(String[] args) throws Exception {
        // 指定生产者组名称
        DefaultMQProducer producer = new DefaultMQProducer(DEMO01_PRODUCER_GROUP_NAME);
        producer.setNamesrvAddr(NAMESRV_ADDRESS);

        producer.start();

        for (int i = 0; i < 5; i++) {
            Message message = new Message(TOPIC_NAME,// topic
                    "TagA",//tag
                    "key" + i,//keys
                    ("Hello world RocketMQ Demo01" + i).getBytes());

            // 向broker发送消息
            producer.send(message, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    System.out.printf("msgId: " + sendResult.getMsgId() + ", status: " + sendResult.getSendStatus());
                }

                @Override
                public void onException(Throwable e) {
                    e.printStackTrace();
                    System.err.println("==============发送失败");
                }
            });
        }
    }

}
