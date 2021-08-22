package com.bruis.learnnetty.netty.heartbeat;

import java.nio.ByteBuffer;

/**
 * @author lhy
 * @date 2021/8/19
 */
public class RemotingCommand {

    private Integer code;       // 请求码

    private byte[] body;        // 请求内容

    public static RemotingCommand decode(final ByteBuffer byteBuffer) {
        int limit = byteBuffer.limit();

        int oriHeaderLen = byteBuffer.getInt();
        int headerLength = getHeaderLength(oriHeaderLen);

        byte[] headerData = new byte[headerLength];
        byteBuffer.get(headerData);

        int bodyLength = limit - 4 - headerLength;

        byte[] body = new byte[bodyLength];
        byteBuffer.get(body);
        RemotingCommand remotingCommand = new RemotingCommand();
        remotingCommand.setBody(body);
        return remotingCommand;
    }

    public ByteBuffer encodeHeader() {
        return encodeHeader(this.body.length);
    }

    public ByteBuffer encodeHeader(final int bodyLength) {
        int length = 4;

        byte[] headerData;
        headerData = this.headerEncode();
        length += headerData.length;        // 头
        length += bodyLength;               // 请求/响应体

        ByteBuffer result = ByteBuffer.allocate(4 + length - bodyLength);        // 分配header
        result.putInt(length);
        result.put(markProtocolType(headerData.length, SerializeType.JSON));
        result.put(headerData);             // 添加头
        result.flip();

        return result;
    }

    public static byte[] markProtocolType(int source, SerializeType type) {
        byte[] result = new byte[4];

        result[0] = type.getCode();
        result[1] = (byte) ((source >> 16) & 0xFF);
        result[2] = (byte) ((source >> 8) & 0xFF);
        result[3] = (byte) (source & 0xFF);
        return result;
    }

    private byte[] headerEncode() {
        return RemotingSerializable.encode(this);
    }

    public static int getHeaderLength(int length) {
        return length & 0xFFFFFF;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }
}
