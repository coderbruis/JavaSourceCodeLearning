package com.bruis.learnnetty.im.model;

import com.bruis.learnnetty.im.session.Session;

import java.util.List;

import static com.bruis.learnnetty.im.model.Command.LIST_GROUP_MEMBERS_RESPONSE;

/**
 * @Description
 * @Author luohaiyang
 * @Date 2022/3/24
 */
public class ListGroupMembersResponsePacket extends Packet {

    private String groupId;

    private List<Session> sessionList;

    @Override
    public Byte getCommand() {

        return LIST_GROUP_MEMBERS_RESPONSE;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public List<Session> getSessionList() {
        return sessionList;
    }

    public void setSessionList(List<Session> sessionList) {
        this.sessionList = sessionList;
    }
}