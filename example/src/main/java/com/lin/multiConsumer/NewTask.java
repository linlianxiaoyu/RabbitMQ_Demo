package com.lin.multiConsumer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;


public class NewTask {
    private final static String TASK_QUEUE_NAME = "task_queue";

    public static void main(String[] args) throws Exception{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("127.0.0.1");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(TASK_QUEUE_NAME,true,false,false,null);
        //发送多条消息
        for(int i=0;i<10;i++){
            String message = "Multi Message" + i;
            channel.basicPublish("",TASK_QUEUE_NAME,
                    MessageProperties.TEXT_PLAIN,message.getBytes("UTF-8"));
            System.out.println("NewTask send '"+message+"'");
        }

        channel.close();
        connection.close();

    }
}
