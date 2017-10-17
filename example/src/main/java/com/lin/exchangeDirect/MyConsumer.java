package com.lin.exchangeDirect;

import com.rabbitmq.client.*;


public class MyConsumer {

    private final static String EXCHANGE_NAME = "exchange_direct";
    private final static String EXCHANGE_MODE = "direct";

    private final static String[] routingKey = new String[]{"apple","banana"};

    public static void main(String[] args) throws  Exception{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("127.0.0.1");
        Connection connection = factory.newConnection();
        Channel channel =  connection.createChannel();
        channel.exchangeDeclare(EXCHANGE_NAME,EXCHANGE_MODE);

        String queueName = channel.queueDeclare().getQueue();

        //根据路由关键字进行绑定
        for(String key : routingKey){
            channel.queueBind(queueName,EXCHANGE_NAME,key);
        }

        System.out.println("Consumer waiting for message ...");

        Consumer consumer = new DefaultConsumer(channel){

            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body){
                String message = new String(body);
                System.out.println("Receive the message '" + message + "'");
            }

        };

        channel.basicConsume(queueName,true,consumer);
    }
}
