package com.andy.rabbit_spring.convert;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;

/**
 * @author lianhong
 * @description
 * @date 2019/8/17 0017下午 4:53
 */
public class TextMessageConvert implements MessageConverter {
    @Override
    public Message toMessage(Object o, MessageProperties messageProperties) throws MessageConversionException {
        return new Message(o.toString().getBytes(),messageProperties);
    }

    @Override
    public Object fromMessage(Message message) throws MessageConversionException {
        String contentType = message.getMessageProperties().getContentType();
        if (null!=contentType && contentType.contains("text")){
            return new String(message.getBody());
        }
        return null;
    }
}
