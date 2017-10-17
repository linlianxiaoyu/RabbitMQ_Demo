package com.lin.exchangeTopic;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class MyProducer {

    private final static String EXCHANGE_NAME = "exchange_topic";
    private final static String EXCHANGE_MODE = "topic";

    public static void main(String[] args) throws Exception{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("127.0.0.1");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(EXCHANGE_NAME,EXCHANGE_MODE);


        String[] routingKeys = new String[]{
                "quick.orange.rabbit",
                "lazy.orange.elephant",
                "quick.orange.fox",
                "lazy.brown.fox",
                "quick.brown.fox",
                "quick.orange.male.rabbit",
                "lazy.orange.male.rabbit"
        };

        for(String key : routingKeys){
            String message = "Current routing key is " + key ;
            channel.basicPublish(EXCHANGE_NAME,key,null,message.getBytes());
            System.out.println("Send message '" + message + "'");
        }
        channel.close();
        connection.close();
    }
}
