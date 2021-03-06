package com.lin.basic;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;


public class Producer {

    private final static String QUEUE_NAME = "hello";
    private final static String message = "Hello Rabbit";

    public static void main(String[] args) throws Exception{

        //创建链接链接到RabbitMQ
        ConnectionFactory connectionFactory = new ConnectionFactory();
        //设置RabbitMQ所在的主机IP或者主机名
        connectionFactory.setHost("127.0.0.1");
        //创建一个链接
        Connection connection = connectionFactory.newConnection();
        //创建一个频道
        Channel channel = connection.createChannel();
        //指定一个队列
        /*queueDeclare
        *第一个参数表示队列名称、
        *第二个参数为是否持久化（true表示是，队列将在服务器重启时生存）、
        *第三个参数为是否是独占队列（创建者可以使用的私有队列，断开后自动删除）、
        *第四个参数为当所有消费者客户端连接断开时是否自动删除队列、
        *第五个参数为队列的其他参数
        */
        channel.queueDeclare(QUEUE_NAME,false,false,false,null);
        //向队列发送一条消息
        /*basicPublish
        *第一个参数为队列名称、
        *第二个参数为队列映射的路由key、
        *第三个参数为消息的其他属性、
        *第四个参数为发送信息的主体
        */
        channel.basicPublish("",QUEUE_NAME,null,message.getBytes());
        System.out.println("[X] send '" + message + "'");
        //关闭频道和链接
        channel.close();
        connection.close();
    }
}
