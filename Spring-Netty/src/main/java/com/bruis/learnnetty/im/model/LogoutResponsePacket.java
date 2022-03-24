package com.bruis.learnnetty.im.model;

import static com.bruis.learnnetty.im.model.Command.LOGOUT_RESPONSE;

/**
 * @Description
 * @Author luohaiyang
 * @Date 2022/3/24
 */
public class LogoutResponsePacket extends Packet {

    private boolean success;

    private String reason;


    @Override
    public Byte getCommand() {
        return LOGOUT_RESPONSE;
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