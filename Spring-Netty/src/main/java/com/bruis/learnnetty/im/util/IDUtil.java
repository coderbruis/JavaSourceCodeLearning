package com.bruis.learnnetty.im.util;

import java.util.UUID;

/**
 * @Description
 * @Author luohaiyang
 * @Date 2022/3/24
 */
public class IDUtil {

    public static String randomUserId() {
        return UUID.randomUUID().toString().split("-")[0];
    }

}
