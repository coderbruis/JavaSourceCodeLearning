package com.bruis.learnnetty.im.model;

import com.bruis.learnnetty.im.session.Session;

import static com.bruis.learnnetty.im.model.Command.GROUP_MESSAGE_RESPONSE;

/**
 * @Description
 * @Author luohaiyang
 * @Date 2022/3/24
 */
public class GroupMessageResponsePacket extends Packet {

    private String fromGroupId;

    private Session fromUser;

    private String message;

    @Override
    public Byte getCommand() {

        return GROUP_MESSAGE_RESPONSE;
    }

    public String getFromGroupId() {
        return fromGroupId;
    }

    public void setFromGroupId(String fromGroupId) {
        this.fromGroupId = fromGroupId;
    }

    public Session getFromUser() {
        return fromUser;
    }

    public void setFromUser(Session fromUser) {
        this.fromUser = fromUser;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}