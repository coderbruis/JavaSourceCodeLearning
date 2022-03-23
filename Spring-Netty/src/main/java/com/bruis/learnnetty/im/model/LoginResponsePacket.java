package com.bruis.learnnetty.im.model;

import static com.bruis.learnnetty.im.model.Command.LOGIN_RESPONSE;

/**
 * @Description
 * @Author luohaiyang
 * @Date 2022/3/22
 */
public class LoginResponsePacket extends Packet {
    private boolean success;

    private String reason;


    @Override
    public Byte getCommand() {
        return LOGIN_RESPONSE;
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
