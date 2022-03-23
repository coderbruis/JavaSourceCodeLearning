package com.bruis.learnnetty.im.util;

import com.bruis.learnnetty.im.model.Attributes;
import io.netty.channel.Channel;
import io.netty.util.Attribute;

/**
 * @Description
 * @Author luohaiyang
 * @Date 2022/3/22
 */
public class LoginUtil {

    public static void markAsLogin(Channel channel) {
        channel.attr(Attributes.LOGIN).set(true);
    }

    public static boolean hasLogin(Channel channel) {
        Attribute<Boolean> loginAttr = channel.attr(Attributes.LOGIN);

        return loginAttr.get() != null;
    }
}
