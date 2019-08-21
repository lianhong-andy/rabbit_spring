package com.andy.rabbit_spring.config;

import com.andy.rabbit_spring.adapter.MessageDeletage;
import com.andy.rabbit_spring.convert.ImageMessageConverter;
import com.andy.rabbit_spring.convert.PDFMessageConverter;
import com.andy.rabbit_spring.convert.TextMessageConvert;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.amqp.support.converter.ContentTypeDelegatingMessageConverter;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author lianhong
 * @description 配置类
 * @date 2019/8/13 0013下午 1:27
 */
@Configuration
@ComponentScan("com.andy.rabbit_spring.*")
public class RabbitMQConfig {

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setAddresses("192.168.56.108:5672");
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");
        connectionFactory.setVirtualHost("/");
        return connectionFactory;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }

    /**
     * 声明topic交换机
     * @return
     */
    @Bean
    public TopicExchange exchange1() {
        return new TopicExchange("topic001",true,false);
    }

    @Bean
    public Queue queue1() {
        return new Queue("queue001",true);
    }

    @Bean
    public Binding binding1() {
        return BindingBuilder.bind(queue1()).to(exchange1()).with("spring.*");
    }

    @Bean
    public TopicExchange exchange2() {
        return new TopicExchange("topic002",true,false);
    }

    @Bean
    public Queue queue2() {
        return new Queue("queue002",true);
    }

    @Bean
    public Binding binding2() {
        return BindingBuilder.bind(queue2()).to(exchange2()).with("*.rabbit");
    }


    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
        return rabbitTemplate;
    }

    @Bean
    public Queue queue3() {
        return new Queue("queue003", true); //队列持久
    }

    @Bean
    public Binding binding3() {
        return BindingBuilder.bind(queue3()).to(exchange1()).with("mq.*");
    }

    @Bean
    public Queue queue_image() {
        return new Queue("image_queue", true); //队列持久
    }

    @Bean
    public Queue queue_pdf() {
        return new Queue("pdf_queue", true); //队列持久
    }

    /**
     * 当 messageContainer 和myMessageContainer同时存在是，messageContainer优先（只识别messageContainer）
     * @param connectionFactory
     * @return
     */
//    @Bean
    public SimpleMessageListenerContainer messageContainer(ConnectionFactory connectionFactory) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueues(queue1());
        //设置消费者的数量
        container.setConcurrentConsumers(5);
        //设置是否重回队列
        container.setDefaultRequeueRejected(false);
        //设置是否自动应答
        container.setAcknowledgeMode(AcknowledgeMode.AUTO);
        container.setExposeListenerChannel(true);
        /*container.setConsumerTagStrategy(new ConsumerTagStrategy() {
            @Override
            public String createConsumerTag(String queue) {
                //指定标签id
                return queue + "_" + UUID.randomUUID().toString();
            }
        });*/
        //用lambda表达式替换
        container.setConsumerTagStrategy(queue -> {
            //指定标签id
            return queue + "_" + UUID.randomUUID().toString();
        });

        /*container.setMessageListener(new ChannelAwareMessageListener() {
            @Override
            public void onMessage(Message message, Channel channel) throws Exception {
                String msg = new String (message.getBody());
                System.err.println("=================消费者："+msg);
            }
        });*/

        //替换成lambda表达式
        container.setMessageListener((ChannelAwareMessageListener) (message, channel) -> {
            String msg = new String (message.getBody());
            System.err.println("=================消费者："+msg);
        });
        return container;
    }

    @Bean
    public SimpleMessageListenerContainer myMessageContainer(ConnectionFactory connectionFactory) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueues(queue1(),queue2(),queue2(),queue_image(),queue_pdf());
        //设置消费者的数量
        container.setConcurrentConsumers(5);
        //设置是否重回队列
        container.setDefaultRequeueRejected(false);
        //设置是否自动应答
        container.setAcknowledgeMode(AcknowledgeMode.AUTO);
        container.setExposeListenerChannel(true);
        //用lambda表达式替换
        container.setConsumerTagStrategy(queue -> {
            //指定标签id
            return queue + "_" + UUID.randomUUID().toString();
        });

        /**
         * 1.适配器方式，默认是有自己的方法名字的：handleMessage
         * 可以自己指定一个方法的名字：consumer
         * 也可以添加一个转换器：从字节数组转换为String
        MessageListenerAdapter adapter = new MessageListenerAdapter(new MessageDelegate());
        adapter.setDefaultListenerMethod("consumerMessage");
        adapter.setMessageConverter(new TextMessageConvert());
        container.setMessageListener(adapter);
        */

        /**
         * 适配器方式：我们的队列名称和方法也可以进行一一匹配
         * */
        /**
        MessageListenerAdapter adapter = new MessageListenerAdapter(new MessageDelegate());
        adapter.setMessageConverter(new TextMessageConvert());
        Map<String, String> queueOrTagToMethodName = new HashMap<>();
        queueOrTagToMethodName.put(queue1().getName(),"method1");
        queueOrTagToMethodName.put(queue2().getName(),"method2");
        adapter.setQueueOrTagToMethodName(queueOrTagToMethodName);
        container.setMessageListener(adapter);
        */



        //1.1支持json格式的转换器
        /**
        MessageListenerAdapter adapter = new MessageListenerAdapter(new MessageDeletage());
        adapter.setDefaultListenerMethod("consumeMessage");
        adapter.setMessageConverter(new Jackson2JsonMessageConverter());
        container.setMessageListener(adapter);
        */

        //1.2 DefaultJackson2JavaTypeMapper & Jackson2JsonMessageConverter 支持java对象转换
        /**
        MessageListenerAdapter adapter = new MessageListenerAdapter(new MessageDeletage());
        adapter.setDefaultListenerMethod("consumeMessage");
        Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper javaTypeMapper = new DefaultJackson2JavaTypeMapper();
        javaTypeMapper.setTrustedPackages("*");
        jackson2JsonMessageConverter.setJavaTypeMapper(javaTypeMapper);
        adapter.setMessageConverter(jackson2JsonMessageConverter);
        container.setMessageListener(adapter);
        */

        //1.3 DefaultJackson2JavaTypeMapper & Jackson2JsonMessageConvert 支持java对象多映射对象
        /**
        MessageListenerAdapter adapter = new MessageListenerAdapter(new MessageDeletage());
        adapter.setDefaultListenerMethod("consumeMessage");
        Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper javaTypeMapper = new DefaultJackson2JavaTypeMapper();

        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put("order",com.andy.rabbit_spring.entity.Order.class);
        idClassMapping.put("packaged",com.andy.rabbit_spring.entity.Packaged.class);
        javaTypeMapper.setIdClassMapping(idClassMapping);
        javaTypeMapper.setTrustedPackages("*");

        jackson2JsonMessageConverter.setJavaTypeMapper(javaTypeMapper);

        adapter.setMessageConverter(jackson2JsonMessageConverter);
        container.setMessageListener(adapter);
        */

        //1.4 ext convert
        MessageListenerAdapter adapter = new MessageListenerAdapter(new MessageDeletage());
        adapter.setDefaultListenerMethod("consumeMessage");
        /**创建一个大的转换器，可以包含多种转换器*/
        ContentTypeDelegatingMessageConverter convert = new ContentTypeDelegatingMessageConverter();

        /**文本转换器*/
        TextMessageConvert textConvert = new TextMessageConvert();
        convert.addDelegate("text",textConvert);
        convert.addDelegate("html/text",textConvert);
        convert.addDelegate("xml/text",textConvert);
        convert.addDelegate("text/plain",textConvert);

        /**对象转换器*/
        Jackson2JsonMessageConverter jsonConvert = new Jackson2JsonMessageConverter();
        convert.addDelegate("json",jsonConvert);
        convert.addDelegate("application/json",jsonConvert);

        /**图片转换器*/
        ImageMessageConverter imgConvert = new ImageMessageConverter();
        convert.addDelegate("image/jpg",imgConvert);
        convert.addDelegate("image",imgConvert);

        /**pdf转换器*/
        PDFMessageConverter pdfConvert = new PDFMessageConverter();
        convert.addDelegate("application/pdf",pdfConvert);

        adapter.setMessageConverter(convert);

        container.setMessageListener(adapter);


        return container;
    }

}
