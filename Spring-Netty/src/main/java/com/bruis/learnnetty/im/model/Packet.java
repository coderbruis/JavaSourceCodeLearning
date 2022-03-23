package com.bruis.learnnetty.im.model;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * @Description
 * @Author luohaiyang
 * @Date 2022/3/22
 */
public abstract class Packet {
    /**
     * 协议版本
     */
    @JSONField(deserialize = false , serialize = false)
    private Byte version = 1;

    @JSONField(serialize = false)
    public abstract Byte getCommand();

    public Byte getVersion() {
        return version;
    }

    public void setVersion(Byte version) {
        this.version = version;
    }
}
