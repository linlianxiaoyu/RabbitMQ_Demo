package com.lin.multiConsumer;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MyConsumer {
    protected final static String TASK_QUEUE_NAME = "task_queue";

    public static void main(String[] args){

        ConsumerThread cunsumer1 = new ConsumerThread();
        ConsumerThread cunsumer2 = new ConsumerThread();
        ThreadPoolExecutor pool = new ThreadPoolExecutor(10,15,20,
                TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(5));
        pool.execute(cunsumer1);
        pool.execute(cunsumer2);

    }

}

class ConsumerThread implements Runnable{

    public void run(){
        try{

            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("127.0.0.1");
            Connection connection = factory.newConnection();
            final Channel channel = connection.createChannel();

            channel.queueDeclare(MyConsumer.TASK_QUEUE_NAME,true,false,false,null);
            System.out.println("Comsumer" + Thread.currentThread().getName() + "waiting for message");


            //每次从队列获取的消息数量
            channel.basicQos(1);

            Consumer consumer = new DefaultConsumer(channel){

                @Override
                public void handleDelivery(String consumerTag,Envelope envelope,
                                           AMQP.BasicProperties properties,
                                           byte[] body) throws IOException{

                    String message = new String(body,"UTF-8");
                    System.out.println("Comsumer" + Thread.currentThread().getName() + "receive message'"
                            + message + "'");

                    //设置是否自动回复
                    channel.basicAck(envelope.getDeliveryTag(),false);
                }
            };

            boolean autoAck = false;
            //消息消费完成确认
            channel.basicConsume(MyConsumer.TASK_QUEUE_NAME, autoAck, consumer);

        }catch (Exception e){
            e.printStackTrace();
        }


    }
}
