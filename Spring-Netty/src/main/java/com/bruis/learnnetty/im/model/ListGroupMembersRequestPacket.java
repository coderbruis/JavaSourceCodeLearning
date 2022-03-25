package com.bruis.learnnetty.im.model;

import static com.bruis.learnnetty.im.model.Command.LIST_GROUP_MEMBERS_REQUEST;

/**
 * @Description
 * @Author luohaiyang
 * @Date 2022/3/24
 */
public class ListGroupMembersRequestPacket extends Packet {

    private String groupId;

    @Override
    public Byte getCommand() {

        return LIST_GROUP_MEMBERS_REQUEST;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
}