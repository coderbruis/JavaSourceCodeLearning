package com.bruis.rocketmqdemo.demo01;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.common.RemotingHelper;

import java.util.List;

/**
 * @author lhy
 *
 * 普通消费者
 *
 * @date 2021/7/10
 */
public class Consumer {

    public static final String DEMO01_CONSUMER_GROUP_NAME = "demo01_consumer_group_name";

    public static void main(String[] args) throws Exception {

        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(DEMO01_CONSUMER_GROUP_NAME);
        consumer.setNamesrvAddr(Producer.NAMESRV_ADDRESS);
        // 从哪开始进行消费
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        consumer.subscribe(Producer.TOPIC_NAME,"*");
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                MessageExt messageExt = msgs.get(0);
                try {
                    String topic = messageExt.getTopic();
                    String tags = messageExt.getTags();
                    String keys = messageExt.getKeys();

                    if ("keyDuplicate".equals(keys)) {
                        System.err.println("消息消费失败");
                        int a = 1 / 0;
                    }

                    String msgBody = new String(messageExt.getBody(), RemotingHelper.DEFAULT_CHARSET);
                    System.err.println("topic: " + topic + ",tags: " + tags + ", keys: " + keys + ",body: " + msgBody);
                } catch (Exception e) {
                    e.printStackTrace();
                    // 消费次数
//                    int reconsumeTimes = messageExt.getReconsumeTimes();
//                    System.err.println("reconsumeTimes: " + reconsumeTimes);
//                    // 重试三次
//                    if (reconsumeTimes == 3) {
//                        // 日志记录
//                        // 重试补偿成功
//                        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
//                    }
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        consumer.start();
        System.err.println("Consumer start ....");
    }
}
