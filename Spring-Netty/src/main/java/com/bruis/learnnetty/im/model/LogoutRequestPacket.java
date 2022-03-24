package com.bruis.learnnetty.im.model;

import static com.bruis.learnnetty.im.model.Command.LOGOUT_REQUEST;

/**
 * @Description
 * @Author luohaiyang
 * @Date 2022/3/23
 */
public class LogoutRequestPacket extends Packet {
    @Override
    public Byte getCommand() {

        return LOGOUT_REQUEST;
    }
}
