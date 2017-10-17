package com.lin.exchangeDirect;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class MyProducer {

    private final static String EXCHANGE_NAME = "exchange_direct";
    private final static String EXCHANGE_MODE = "direct";

    private final static String[] routingKey = new String[]{"apple" ,"banana", "orange"};

    public static void main(String[] args) throws Exception{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("127.0.0.1");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME,EXCHANGE_MODE);

        for(String key : routingKey){
            String message = "Current fruit is " + key;
            channel.basicPublish(EXCHANGE_NAME,key,null,message.getBytes());
            System.out.println("Send message '" + message + "'");
        }

        channel.close();
        connection.close();
    }
}
