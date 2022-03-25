package com.bruis.learnnetty.im.model;

import static com.bruis.learnnetty.im.model.Command.QUIT_GROUP_REQUEST;
import static com.bruis.learnnetty.im.model.Command.QUIT_GROUP_RESPONSE;

/**
 * @Description
 * @Author luohaiyang
 * @Date 2022/3/24
 */
public class QuitGroupResponsePacket extends Packet {

    private String groupId;

    private boolean success;

    private String reason;

    @Override
    public Byte getCommand() {
        return QUIT_GROUP_RESPONSE;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
