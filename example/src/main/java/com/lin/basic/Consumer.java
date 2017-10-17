package com.lin.basic;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class Consumer {

    private final static String QUEUE_NAME = "hello";

    public static void main(String[] args) throws Exception{
        //打开链接创建频道
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("127.0.0.1");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        //声明队列，主要为了防止消息接收者先运行此程序，队列还不存在时创建队列。
        channel.queueDeclare(QUEUE_NAME,false,false,false,null);
        System.out.println("[X] Waiting for message. To exit press Ctrl+C");

        //DefaultConsumer类实现了Consumer接口，通过传入一个频道，
        // 告诉服务器我们需要那个频道的消息，如果频道中有消息，就会执行回调函数handleDelivery
        com.rabbitmq.client.Consumer consumer = new DefaultConsumer(channel){

          //envelope主要存放生产者相关信息（比如交换机、路由key等）body是消息实体
          @Override
          public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws UnsupportedEncodingException{
              String message = new String(body,"UTF-8");
              System.out.println("Cunsumer Recieved '" + message + "'");
          }

        };

        //自动回复队列应答，---- RabbitMQ中的消息确认机制
        channel.basicConsume(QUEUE_NAME,true,consumer);


    }
}
