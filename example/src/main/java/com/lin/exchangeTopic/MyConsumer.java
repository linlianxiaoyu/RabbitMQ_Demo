package com.lin.exchangeTopic;

import com.rabbitmq.client.*;


public class MyConsumer {

    private final static String EXCHANGE_NAME = "exchange_topic";
    private final static String EXCHANGE_MODE = "topic";

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("127.0.0.1");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(EXCHANGE_NAME,EXCHANGE_MODE);
        String queueName = "queue_top";
        channel.queueDeclare(queueName,false,false,false,null);
        // 路由关键字
        String[] routingKeys = new String[]{"*.*.rabbit", "lazy.#"};
        for(String key : routingKeys){
            channel.queueBind(queueName,EXCHANGE_NAME,key);
        }

        System.out.println("Consumer waiting for  message ...");
        Consumer consumer = new DefaultConsumer(channel){
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body){
                String message = new String(body);
                System.out.println("Receive the mesaage '" + message + "'");
            }

        };

        channel.basicConsume(queueName,true,consumer);
    }
}
