package com.lin.exchangeFanout;

import com.rabbitmq.client.*;


public class MyConsumer {

    private final static String EXCHANGE_NAME = "exchangeFanout";
    private final static String EXCHANGE_MODE = "fanout";


    public static void main(String[] args) throws Exception{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("127.0.0.1");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(EXCHANGE_NAME,EXCHANGE_MODE);

        //产生一个随机队列
        String queueName = channel.queueDeclare().getQueue();
        //绑定队列
        channel.queueBind(queueName,EXCHANGE_NAME,"");
        System.out.println("Consumer waiting for message...");


        Consumer consumer = new DefaultConsumer(channel){

            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body){
                String message = new String(body);
                System.out.println("Receive the message '" + message + "'");
            }

        };

        //队列会自动删除
        channel.basicConsume(queueName,true,consumer);
    }
}
