package com.bruis.learnnetty.im.model;

import static com.bruis.learnnetty.im.model.Command.MESSAGE_REQUEST;

/**
 * @Description
 * @Author luohaiyang
 * @Date 2022/3/22
 */
public class MessageRequestPacket extends Packet {

    public MessageRequestPacket(){}

    public MessageRequestPacket(String message) {
        this.message = message;
    }

    private String message;

    @Override
    public Byte getCommand() {
        return MESSAGE_REQUEST;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
