package com.bruis.learnnetty.im.model;

import static com.bruis.learnnetty.im.model.Command.MESSAGE_RESPONSE;

/**
 * @Description
 * @Author luohaiyang
 * @Date 2022/3/22
 */
public class MessageResponsePacket extends Packet {

    private String message;

    @Override
    public Byte getCommand() {

        return MESSAGE_RESPONSE;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}