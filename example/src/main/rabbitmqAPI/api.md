# Connecting to a broker
下面的代码用于连接一个AMQP协议的broker(代理服务器)，可以使用给定的参数列表(主机名，端口号等)
    
    ConnectionFactory factory = new ConnectionFactory(); 
    factory.setUsername(userName); 
    factory.setPassword(password); 
    factory.setVirtualHost(virtualHost); 
    factory.setHost(hostName); 
    factory.setPort(portNumber); 
    Connection conn = factory.newConnection(); 
也可以使用URIs 来设置连接参数:

    ConnectionFactory factory = new ConnectionFactory(); 
    factory.setUri("amqp://userName:password@hostName:portNumber/virtualHost"); 
    Connection conn = factory.newConnection(); 
    
Connection 接口可用来打开一个channel:

    Channel channel = conn.createChannel(); 

channel现在可用来发送和接收消息，正如后续章节中描述的一样.

要断开连接，只需要简单地关闭channel和connection:

    channel.close(); conn.close();

关闭channel被认为是最佳实践,但在这里不是严格必须的 - 当底层连接关闭的时候，channel也会自动关闭.



# 使用 Exchanges 和 Queues

## 采用交换器和队列工作的客户端应用程序,是AMQP高级别构建模块。在使用前，必须先声明.声明每种类型的对象都需要确保名称存在，如果有必要须进行创建.

继续上面的例子,下面的代码声明了一个交换器和一个队列，然后再将它们进行绑定.

    channel.exchangeDeclare(exchangeName, "direct", true); 
    String queueName = channel.queueDeclare().getQueue(); 
    channel.queueBind(queueName, exchangeName, routingKey);

这实际上会声明下面的对象，它们两者都可以可选参数来定制. 在这里，它们两个都没有特定参数。

+ 一个类型为direct，且持久化，非自动删除的交换器
+ 采用随机生成名称，且非持久化，私有的，自动删除队列
+ 使用给定的路由键来绑定队列和交换器.



# 路由消息

要向交换器中发布消息,可按下面这样来使用Channel.basicPublish方法:

    byte[] messageBodyBytes = "Hello, world!".getBytes(); 
    channel.basicPublish(exchangeName, routingKey, null, messageBodyBytes);

为了更好的控制，你可以使用重载方法来指定mandatory标志，或使用预先设置的消息属性来发送消息:
    
    channel.basicPublish(exchangeName, routingKey, mandatory, MessageProperties.PERSISTENT_TEXT_PLAIN,messageBodyBytes);
    
这会使用分发模式2(持久化)来发送消息, 优先级为1，且content-type 为"text/plain".你可以使用Builder类来构建你自己的消息属性对象：
    
    channel.basicPublish(exchangeName, routingKey,new AMQP.BasicProperties.Builder().contentType("text/plain").deliveryMode(2).priority(1).userId("bob").build()),messageBodyBytes);

下面的例子使用自定义的headers来发布消息:

    Map<String, Object> headers = new HashMap<String, Object>(); 
    headers.put("latitude",  51.5252949); 
    headers.put("longitude", -0.0905493);  
    channel.basicPublish(exchangeName, routingKey,new AMQP.BasicProperties.Builder().headers(headers).build()),messageBodyBytes);

下面的例子使用expiration来发布消息:

    channel.basicPublish(exchangeName, routingKey,new AMQP.BasicProperties.Builder().expiration("60000").build()),messageBodyBytes);



# Channels 和并发考虑(线程安全性)

Channel 实例不能在多个线程间共享。应用程序必须在每个线程中使用不同的channel实例,而不能将同个channel实例在多个线程间共享。
有些channl上的操作是线程安全的，有些则不是，这会导致传输时出现错误的帧交叉。在多个线程共享channels也会干扰Publisher Confirms.



# 通过订阅来来接收消息

接收消息最高效的方式是用Consumer接口来订阅。当消息到达时，它们会自动地进行分发，而不需要显示地请求。

当在调用Consumers的相关方法时, 个别订阅总是通过它们的consumer tags来确定的, consumer tags可通过客户端或服务端来生成，参考 the AMQP specification document.
同一个channel上的消费者必须有不同的consumer tags.

实现Consumer的最简单方式是继承便利类DefaultConsumer.子类可通过在设置订阅时，将其传递给basicConsume调用:

    boolean autoAck = false; 
    channel.basicConsume(queueName, autoAck, "myConsumerTag",new DefaultConsumer(channel) {          
    @Override          
    publicvoid handleDelivery(String consumerTag,Envelope envelope,AMQP.BasicProperties properties,byte[] body)throws IOException{              
    String routingKey = envelope.getRoutingKey();              
    String contentType = properties.getContentType();              
    long deliveryTag = envelope.getDeliveryTag();              
    // (process the message components here ...)              
    channel.basicAck(deliveryTag, false);          
    }      
    });

在这里，由于我们指定了autoAck = false,因此消费者有必要应答分发的消息，最便利的方式是在handleDelivery 方法中处理.
更复杂的消费者可能需要覆盖更多的方法，实践中，handleShutdownSignal会在channels和connections关闭时调用，handleConsumeOk 会在其它消费者之前
调用，传递consumer tag。

消费者可实现handleCancelOk 和 handleCancel方法来接收显示和隐式取消操作通知。
你可以使用Channel.basicCancel来显示地取消某个特定的消费者:

    channel.basicCancel(consumerTag);
    
消费者回调是在单独线程上处理的，这意味着消费者可以安全地在Connection或Channel, 如queueDeclare, txCommit, basicCancel或basicPublish上调用阻塞方法。
每个Channel都有其自己的dispatch线程.对于一个消费者一个channel的大部分情况来说，这意味着消费者不会阻挡其它的消费者。如果在一个channel上多个消费者，则必须意识到长时间运行的消费者可能阻挡此channel上其它消费者回调调度.



# 获取单个消息

要显示地获取一个消息，可使用Channel.basicGet.返回值是一个GetResponse实例, 在它之中，header信息(属性) 和消息body都可以提取:

    boolean autoAck = false; 
    GetResponse response = channel.basicGet(queueName, autoAck); 
    if (response == null) {     
    // No message retrieved. 
    } else {     
    AMQP.BasicProperties props = response.getProps();     
    byte[] body = response.getBody();     
    long deliveryTag = response.getEnvelope().getDeliveryTag();     ...

因为autoAck = false,你必须调用Channel.basicAck来应答你已经成功地接收了消息:

    channel.basicAck(method.deliveryTag, false); // acknowledge receipt of the message }




# 处理未路由消息

如果发布消息时，设置了"mandatory"标志,但如果消息不能路由的话，broker会将其返回到发送客户端 (通过 AMQP.Basic.Return 命令).
要收到这种返回的通知， clients可实现ReturnListener接口，并调用Channel.setReturnListener.如果channel没有配置return listener,那么返回的消息会默默地丢弃。

    channel.setReturnListener(new ReturnListener() {     
        publicvoid handleBasicReturn(int replyCode,String replyText,String exchange,String routingKey,AMQP.BasicProperties properties,byte[] body)     throws IOException {
             ...     
        } 
    });

return listener将被调用,例如,如果client使用"mandatory"标志向未绑定队列的direct类型交换器发送了消息.




# 关闭协议

## AMQP client 关闭概述

AMQP 0-9-1 connection和channel 使用相同的方法来管理网络故障,内部故障,以及显示本地关闭.

AMQP 0-9-1 connection  和 channel 有如下的生命周期状态:

    open: 准备要使用的对象
    closing: 对象已显示收到收到本地关闭通知, 并向任何支持的底层对象发出关闭请求,并等待其关闭程序完成
    closed: 对象已收到所有底层对象的完成关闭通知,最终将执行关闭操作

这些对象总是以closed状态结束的,不管基于什么原因引发的关闭,比如:应用程序请求,内部client library故障, 远程网络请求或网络故障.

AMQP connection 和channel 对象会持有下面与关闭相关的方法:

    addShutdownListener(ShutdownListener 监听器)和removeShutdownListener(ShutdownListener 监听器),用来管理监听器,当对象转为closed状态时,将会触发这些监听器.注意,在已经关闭的对象上添加一个ShutdownListener将会立即触发监听器
    getCloseReason(), 允许同其交互以了解对象关闭的理由
    isOpen(), 用于测试对象是否处于open状态
    close(int closeCode, String closeMessage), 用于显示通知对象关闭

### 可以像这样来简单使用监听器:

    import com.rabbitmq.client.ShutdownSignalException; 
    import com.rabbitmq.client.ShutdownListener;  
    connection.addShutdownListener(new ShutdownListener() {     
        public void shutdownCompleted(ShutdownSignalException cause)     {         ...     } }
    );

### 关闭环境信息

可通过显示调用getCloseReason()方法或通过使用ShutdownListener类中的业务方法的cause参数来从ShutdownSignalException中获取关闭原因的有用信息.

ShutdownSignalException 类提供方法来分析关闭的原因.通过调用isHardError()方法,我们可以知道是connection错误还是channel错误.getReason()会返回相关cause的相关信息,这些引起cause的方法形式-要么是AMQP.Channel.Close方法,要么是AMQP.Connection.Close (或者是null,如果是library中引发的异常,如网络通信故障,在这种情况下,可通过getCause()方法来获取信息).

    public void shutdownCompleted(ShutdownSignalException cause) {   if (cause.isHardError())   {     
        Connection conn = (Connection)cause.getReference();     
        if (!cause.isInitiatedByApplication())     {       
        Method reason = cause.getReason();       ...     }     ...   } 
    else {     Channel ch = (Channel)cause.getReference();     ...   } }

### 原子使用isOpen()方法

channel和connection对象的isOpen()方法不建议在在生产代码中使用,因为此方法的返回值依赖于shutdown cause的存在性. 下面的代码演示了竟争条件的可能性:

    public void brokenMethod(Channel channel) { 
        if (channel.isOpen())     {      
           // The following code depends on the channel being in open state.
           // However there is a possibility of the change in the channel state
           // between isOpen() and basicQos(1) call 
                   ...         
           channel.basicQos(1);     
           }
    }

相反,我们应该忽略这种检查,并简单地尝试这种操作.如果代码执行期间,connection的channel关闭了,那么将抛出ShutdownSignalException,这就表明对象处于一种无效状态了.当broker意外关闭连接时,我们也应该捕获由SocketException引发的IOException,或者当broker清理关闭时,捕获ShutdownSignalException.

    public void validMethod(Channel channel) { 
        try {  
               ...
               channel.basicQos(1); 
        } catch (ShutdownSignalException sse) { 
               // possibly check if channel was closed       
               // by the time we started action and reasons for        
               // closing it         ...    
        } catch (IOException ioe) {    
               // check why connection was closed 
                  ...     
        } 
    }




# 高级连接选项

## Consumer线程池

Consumer 线程默认是通过一个新的ExecutorService线程池来自动分配的(参考下面的Receiving).如果需要在newConnection() 方法中更好地控制ExecutorService,可以使用定制的线程池.下面的示例展示了一个比正常分配稍大的线程池:

    ExecutorService es = Executors.newFixedThreadPool(20);
    Connection conn = factory.newConnection(es); 

Executors 和 ExecutorService 都是java.util.concurrent包中的类.

当连接关闭时,默认的ExecutorService将会被shutdown(), 但用户自定义的ExecutorService (如上面所示)将不会被shutdown(). 提供自定义ExecutorService的Clients必须确保最终它能被关闭(通过调用它的shutdown() 方法), 否则池中的线程可能会阻止JVM终止.

同一个executor service,可在多个连接之间共享,或者连续地在重新连接上重用,但在shutdown()后,则不能再使用.

使用这种特性时,唯一需要考虑的是:在消费者回调的处理过程中,是否有证据证明有严重的瓶颈. 如果没有消费者执行回调,或很少,默认的配置是绰绰有余. 开销最初是很小的,分配的全部线程资源也是有界限的,即使偶尔可能出现一阵消费活动.

## 使用Host列表

可以传递一个Address数组给newConnection(). Address只是 com.rabbitmq.client 包中包含host和port组件的简单便利类. 例如:

    Address[] addrArr = new Address[]{ new Address(hostname1, portnumber1)                                  , new Address(hostname2, portnumber2)}; Connection conn = factory.newConnection(addrArr); 

将会尝试连接hostname1:portnumber1, 如果不能连接,则会连接hostname2:portnumber2,然后会返回数组中第一个成功连接(不会抛出IOException)上broker的连接.这完全等价于在factory上重复调用factory.newConnection()方法来设置host和port, 直到有一个成功返回.

如果提供了ExecutorService(在factory.newConnection(es, addrArr)中使用),那么线程池将与第一个成功连接相关联.

## 心跳超时

参考Heartbeats guide 来了解更多关于心跳及其在Java client中如何配置的更多信息.

## 自定义线程工厂

像Google App Engine (GAE)这样的环境会限制线程直接实例化. 在这样的环境中使用RabbitMQ Java client, 需要配置一个定制的ThreadFactory,即使用合适的方法来实例化线程,如: GAE's ThreadManager. 下面是Google App Engine的相关代码.

    import com.google.appengine.api.ThreadManager;
    ConnectionFactory cf = new ConnectionFactory();
    cf.setThreadFactory(ThreadManager.backgroundThreadFactory()); 




# 网络故障时自动恢复

## Connection恢复

clients和RabbitMQ节点之间的连接可发生故障. RabbitMQ Java client 支持连接和拓扑(queues, exchanges, bindings, 和consumers)的自动恢复. 大多数应用程序的连接自动恢复过程会遵循下面的步骤:

    1.重新连接
    2.恢复连接监听器
    3.重新打开通道
    4.恢复通道监听器
    5.恢复通道basic.qos 设置,发布者确认和事务设置

拓扑恢复包含下面的操作,每个通道都会执行下面的步骤:

    1.重新声明交换器(except for predefined ones)
    2.重新声明队列
    3.恢复所有绑定
    4.恢复所有消费者

要启用自动连接恢复,须使用factory.setAutomaticRecoveryEnabled(true):

    ConnectionFactory factory = new ConnectionFactory();
    factory.setUsername(userName);
    factory.setPassword(password);
    factory.setVirtualHost(virtualHost);
    factory.setHost(hostName);
    factory.setPort(portNumber);
    factory.setAutomaticRecoveryEnabled(true);
    // connection that will recover automatically
    Connection conn = factory.newConnection();

如果恢复因异常失败(如. RabbitMQ节点仍然不可达),它会在固定时间间隔后进行重试(默认是5秒). 时间间隔可以进行配置:

    ConnectionFactory factory = new ConnectionFactory();
    // attempt recovery every 10 seconds 
    factory.setNetworkRecoveryInterval(10000);

当提供了address列表时,将会在所有address上逐个进行尝试:

    ConnectionFactory factory = new ConnectionFactory();  
    Address[] addresses = {new Address("192.168.1.4"), new Address("192.168.1.5")};
    factory.newConnection(addresses);

## 恢复监听器

可在可恢复连接和通道上注册一个或多个恢复监听器. 当启用了连接恢复时,ConnectionFactory#newConnection 和 Connection#createChannel 的连接已实现了com.rabbitmq.client.Recoverable,并提供了两个方法:

    addRecoveryListener
    removeRecoveryListener

注意,在使用这些方法时,你需要将connections和channels强制转换为Recoverable.
发布影响

当连接失败时,使用Channel.basicPublish方法发送的消息将会丢失. client不会保证在连接恢复后,消息会得到分发.要确保发布的消息到达了RabbitMQ,应用程序必须使用Publisher Confirms 


## 拓扑恢复

拓扑恢复涉及交换器,队列,绑定以及消费者恢复.默认是启用的,但也可以禁用:

    ConnectionFactory factory = new ConnectionFactory();
    Connection conn = factory.newConnection();
    factory.setAutomaticRecoveryEnabled(true);
    factory.setTopologyRecoveryEnabled(false);

## 手动应答和自动恢复

当使用手动应答时,在消息分发与应答之间可能存在网络连接中断. 在连接恢复后,RabbitMQ会在所有通道上重设delivery标记. 也就是说,使用旧delivery标记的basic.ack, basic.nack, 以及basic.reject将会引发channel exception. 为了避免发生这种情况, RabbitMQ Java client可以跟踪,更新,以使它们在恢复期间单调地增长. Channel.basicAck, Channel.basicNack, 以及Channel.basicReject 然后可以转换这些 delivery tags,并且不再发送过期的delivery tags. 使用手动应答和自动恢复的应用程序必须负责处理重新分发.




# 未处理异常

关于connection, channel, recovery, 和consumer lifecycle 的异常将会委派给exception handler,Exception handler是实现了ExceptionHandler接口的任何对象. 默认情况下,将会使用DefaultExceptionHandler实例,它会将异常细节输出到标准输出上.

可使用ConnectionFactory#setExceptionHandler来覆盖原始handler,它将被用于由此factory创建的所有连接:

    ConnectionFactory factory = new ConnectionFactory();
    cf.setExceptionHandler(customHandler);         

Exception handlers 应该用于异常日志.
Google App Engine上的RabbitMQ Java Client

在Google App Engine (GAE) 上使用RabbitMQ Java client,必须使用一个自定义的线程工厂来初始化线程,如使用GAE's ThreadManager. 此外,还需要设置一个较小的心跳间隔(4-5 seconds) 来避免InputStream 上读超时:

ConnectionFactory factory = new ConnectionFactory(); cf.setRequestedHeartbeat(5);         

## 警告和限制

为了能使拓扑恢复, RabbitMQ Java client 维持了声明队列,交换器,绑定的缓存. 缓存是基于每个连接的.某些RabbitMQ的特性使得客户端不能观察到拓扑的变化,如,当队列因TTL被删除时. RabbitMQ Java client 会尝试在下面的情况中使用缓存实体失效:

    1.当队列被删除时.
    2.当交换器被删除时.
    3.当绑定被删除时.
    4.当消费者在自动删除队列上退出时.
    5.当队列或交换器不再绑定到自动删除的交换器上时.

然而, 除了单个连接外,client不能跟踪这些拓扑变化. 依赖于自动删除队列或交换器的应用程序,正如TTL队列一样 (注意:不是消息TTL!), 如果使用了自动连接恢复,在知道不再使用或要删除时,必须明确地删除这些缓存实体,以净化 client-side 拓扑cache.
这些可通过Channel#queueDelete, Channel#exchangeDelete,Channel#queueUnbind, and Channel#exchangeUnbind来进行.




# RPC (Request/Reply) 模式

为了便于编程, Java client API提供了一个使用临时回复队列的RpcClient类来提供简单的RPC-style communication.

此类不会在RPC参数和返回值上强加任何特定格式. 它只是简单地提供一种机制来向带特定路由键的交换器发送消息,并在回复队列上等待响应.

    import com.rabbitmq.client.RpcClient;  
    RpcClient rpc = new RpcClient(channel, exchangeName, routingKey);

(其实现细节为:请求消息使用basic.correlation_id唯一值字段来发送消息,并使用basic.reply_to来设置响应队列的名称.)

一旦你创建此类的实例,你可以使用下面的任意一个方法来发送RPC请求:

    byte[] primitiveCall(byte[] message); 
    String stringCall(String message) Map mapCall(Map message) Map mapCall(Object[] keyValuePairs)

primitiveCall 方法会将原始byte数组转换为请求和响应的消息体. stringCall只是一个primitiveCall的简单包装,将消息体视为带有默认字符集编码的String实例.

mapCall 变种稍为有些复杂: 它会将原始java值包含在java.util.Map中,并将其编码为AMQP 0-9-1二进制表示形式,并以同样的方式来解码response. (注意:在这里,对一些值对象类型有所限制,具体可参考javadoc.)

所有的编组/解组便利方法都使用primitiveCall来作为传输机制,其它方法只是在它上面的做了一个封装.




---------转自博客http://www.blogjava.net/qbna350816/archive/2016/06/04/430771.html