package com.bruis.learnnetty.im.model;

import io.netty.util.AttributeKey;

/**
 * @Description Netty 属性集
 * @Author haiyangluo
 * @Date 2022/3/22
 */
public interface Attributes {
    AttributeKey<Boolean> LOGIN = AttributeKey.newInstance("login");
}
