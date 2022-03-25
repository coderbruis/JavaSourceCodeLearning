package com.bruis.learnnetty.im.model;

import static com.bruis.learnnetty.im.model.Command.JOIN_GROUP_REQUEST;

/**
 * @Description
 * @Author luohaiyang
 * @Date 2022/3/24
 */
public class JoinGroupRequestPacket extends Packet {

    private String groupId;

    @Override
    public Byte getCommand() {
        return JOIN_GROUP_REQUEST;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
}
