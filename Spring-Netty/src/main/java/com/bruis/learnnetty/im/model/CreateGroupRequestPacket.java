package com.bruis.learnnetty.im.model;

import java.util.List;

import static com.bruis.learnnetty.im.model.Command.CREATE_GROUP_REQUEST;

/**
 * @Description
 * @Author luohaiyang
 * @Date 2022/3/23
 */
public class CreateGroupRequestPacket extends Packet {

    private List<String> userIdList;

    @Override
    public Byte getCommand() {
        return CREATE_GROUP_REQUEST;
    }

    public List<String> getUserIdList() {
        return userIdList;
    }

    public void setUserIdList(List<String> userIdList) {
        this.userIdList = userIdList;
    }
}
