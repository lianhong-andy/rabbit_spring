package com.andy.rabbit_spring;

import com.andy.rabbit_spring.entity.Order;
import com.andy.rabbit_spring.entity.Packaged;
import com.andy.rabbit_spring.utils.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RabbitSpringApplicationTests {

    @Test
    public void contextLoads() {
    }

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Test
    public void testAdmin(){




        rabbitAdmin.declareExchange(new FanoutExchange("test_fanout",false,false));
        rabbitAdmin.declareQueue(new Queue("test_fanout_queue1",false));
        rabbitAdmin.declareBinding(new Binding("test_direct_queue1",Binding.DestinationType.QUEUE,
                "test_direct","direct",new HashMap<String, Object>()));



        rabbitAdmin.declareExchange(new DirectExchange("test_direct",false,false));
        rabbitAdmin.declareQueue(new Queue("test_direct_queue1",false));
        rabbitAdmin.declareBinding(
                BindingBuilder.bind(new Queue("test_fanout_queue1",false))
                .to(new FanoutExchange("test_fanout",false,false)));



        rabbitAdmin.declareQueue(new Queue("test_topic_queue1",false,false,false));
        rabbitAdmin.declareExchange(new TopicExchange("test_topic_exchange",true,false));
        //        如果注释掉上面两句实现声明，直接进行下面的绑定竟然不行，该版本amqp-client采用的是5.1.2,将上面两行代码放开，则运行成功
        rabbitAdmin.declareBinding(
                BindingBuilder
                        .bind(new Queue("test_topic_queue1",true,false,false))//直接创建队列
                        .to(new TopicExchange("test_topic_exchange",true,false))//直接创建交换机，建立关联关系
                        .with("user.#"));//指定路由key

        rabbitAdmin.declareQueue(new Queue("test.topic.queue", true, false, false));
        rabbitAdmin.declareExchange(new TopicExchange("test.topic", true, false));
        rabbitAdmin.declareBinding(BindingBuilder.bind(new Queue("test.topic.queue", true, false, false))
                .to(new TopicExchange("test.topic", true, false)).with("mq.topic"));

        rabbitAdmin.purgeQueue("test.topic.queue", false);//清空队列消息



    }


    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    public void testTemplate(){
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType("text/plain");
        messageProperties.getHeaders().put("desc","信息描述");
        messageProperties.getHeaders().put("type","自定义消费类型");
        Message message = new Message("Hello RabbitMQ".getBytes(), messageProperties);

        rabbitTemplate.convertAndSend("topic001","spring.amqp",message, new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                System.out.println("==========添加额外的设置===========" );
                message.getMessageProperties().getHeaders().put("ex","额外修改的信息描述");
                message.getMessageProperties().getHeaders().put("attr","额外添加的属性");
                return message;
            }
        });

        rabbitTemplate.convertAndSend("topic002","abc.rabbit","hello andy");
    }

    /**
     * 发送json
     * @throws JsonProcessingException
     */
    @Test
    public void testSendJsonMessage() throws JsonProcessingException {
        Order order = new Order("001","订单消息","订单描述");
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(order);
        System.out.println("order 4 json = " + JsonUtils.toString(order));

        MessageProperties messageProperties = new MessageProperties();
        /**这里一定要修改contentType为application/json*/
        messageProperties.setContentType("application/json");
        Message message = new Message(json.getBytes(),messageProperties);

        rabbitTemplate.convertAndSend("topic001", "spring.order", message);

    }

    /**
     * 发送java对象
     * @throws JsonProcessingException
     */
    @Test
    public void testSendJavaMessage() throws JsonProcessingException {
        Order order = new Order("001","订单消息","订单描述");
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(order);
        System.out.println("order 4 json = " + JsonUtils.toString(order));

        MessageProperties messageProperties = new MessageProperties();
        /**这里一定要修改contentType为application/json*/
        messageProperties.setContentType("application/json");
        messageProperties.getHeaders().put("__TypeId__","com.andy.rabbit_spring.entity.Order");
        Message message = new Message(json.getBytes(),messageProperties);

        rabbitTemplate.convertAndSend("topic001", "spring.order", message);

    }

    @Test
    public void testSendMappingMessage() throws JsonProcessingException {
        Order order = new Order("001","订单消息","订单描述");
        ObjectMapper mapper = new ObjectMapper();
        String json1 = mapper.writeValueAsString(order);
        System.out.println("order 4 json = " + JsonUtils.toString(order));

        MessageProperties messageProperties1 = new MessageProperties();
        messageProperties1.setContentType("application/json");
        messageProperties1.getHeaders().put("__TypeId__","com.andy.rabbit_spring.entity.Order");

        Message message1 = new Message(json1.getBytes(), messageProperties1);

        rabbitTemplate.send("topic001","spring.order",message1);

        Packaged packaged = new Packaged("001", "包裹消息", "包裹描述");
        String json2 = JsonUtils.toString(packaged);
        System.out.println("packaged 4 json = " + json2);

        MessageProperties messageProperties2 = new MessageProperties();
        messageProperties2.setContentType("application/json");
        messageProperties2.getHeaders().put("__TypeId__","com.andy.rabbit_spring.entity.Packaged");

        Message message2 = new Message(json2.getBytes(), messageProperties2);

        rabbitTemplate.send("topic001","spring.packaged",message2);

    }

    @Test
    public void testSendExtConverterMessage() throws IOException {
        String path = "C:/Users/Administrator.PC-20181201GPRD.000/Desktop";
        String imgName = "doge.jpg";
        String pdfName = "驴妈妈API接口定义文档_线路分销V2.0正式版_20170803 (1).pdf";
//        byte[] body = Files.readAllBytes(Paths.get(path, imgName));
//        MessageProperties messageProperties = new MessageProperties();
//        messageProperties.setContentType("image/jpg");
//        messageProperties.getHeaders().put("extName", "jpg");
//        Message message = new Message(body, messageProperties);
//        rabbitTemplate.send("", "image_queue", message);

        byte[] body = Files.readAllBytes(Paths.get(path, pdfName));
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType("application/pdf");
        messageProperties.getHeaders().put("extName","pdf");
        Message message = new Message(body, messageProperties);

        rabbitTemplate.send("","pdf_queue",message);
    }


}
