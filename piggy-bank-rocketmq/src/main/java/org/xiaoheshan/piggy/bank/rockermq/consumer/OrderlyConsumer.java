package org.xiaoheshan.piggy.bank.rockermq.consumer;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;

/**
 * @author _Chf
 * @since 02-15-2019
 */
public class OrderlyConsumer {

    public static void main(String[] args) throws Exception {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("rockermq-test");
        consumer.setNamesrvAddr("localhost:9876");
        consumer.subscribe("topic-test", "*");
        consumer.registerMessageListener((MessageListenerOrderly) (msgs, context) -> {
            System.out.printf(Thread.currentThread().getName() + " Receive New Messages: " + new String(msgs.get(0).getBody()) + "%n");
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignore) {
            }
            return ConsumeOrderlyStatus.SUCCESS;
        });
        consumer.start();
        System.out.printf("Consumer Started.%n");
    }
}
