package com.bruis.learnnetty.im.model;

import com.bruis.learnnetty.im.session.Session;
import io.netty.util.AttributeKey;

/**
 * @Description Netty 属性集
 * @Author haiyangluo
 * @Date 2022/3/22
 */
public interface Attributes {
    AttributeKey<Session> SESSION = AttributeKey.newInstance("session");
}
