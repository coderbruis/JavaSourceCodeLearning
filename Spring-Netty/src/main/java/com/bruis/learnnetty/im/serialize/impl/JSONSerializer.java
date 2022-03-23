package com.bruis.learnnetty.im.serialize.impl;

import com.alibaba.fastjson.JSON;
import com.bruis.learnnetty.im.serialize.Serializer;
import com.bruis.learnnetty.im.serialize.SerializerAlogrithm;

/**
 * @Description
 * @Author luohaiyang
 * @Date 2022/3/22
 */
public class JSONSerializer implements Serializer {
    @Override
    public byte getSerializerAlogrithm() {
        return SerializerAlogrithm.JSON;
    }

    @Override
    public byte[] serialize(Object object) {

        return JSON.toJSONBytes(object);
    }

    @Override
    public <T> T deserialize(Class<T> clazz, byte[] bytes) {

        return JSON.parseObject(bytes, clazz);
    }
}
