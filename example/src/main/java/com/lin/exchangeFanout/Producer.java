package com.lin.exchangeFanout;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class Producer {

    private final static String EXCHANGE_NAME = "exchangeFanout";
    private final static String EXCHANGE_MODE = "fanout";

    public static void main(String[] args) throws Exception{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("127.0.0.1");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        //创建一个交换机
        //该模式，将消息分发给所有与该exchange绑定的queue
        channel.exchangeDeclare(EXCHANGE_NAME,EXCHANGE_MODE);

        //发送消息
        for(int i=0;i<10;i++){
            String message = "Hello Exchange " + i;
            channel.basicPublish(EXCHANGE_NAME,"",false,null,message.getBytes());
            System.out.println("Send message '" + message + "'");
        }

        channel.close();
        connection.close();

    }
}
