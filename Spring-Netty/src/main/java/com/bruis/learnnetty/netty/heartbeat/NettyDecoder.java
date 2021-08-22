package com.bruis.learnnetty.netty.heartbeat;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.nio.ByteBuffer;

/**
 * @author lhy
 * @date 2021/8/19
 */
public class NettyDecoder extends LengthFieldBasedFrameDecoder {

    public NettyDecoder() {
        super(16777216, 0, 4, 0, 4);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = null;

        try {
            frame = (ByteBuf) super.decode(ctx, in);
            if (null == frame) {
                return null;
            }
            ByteBuffer byteBuffer = frame.nioBuffer();
            return RemotingCommand.decode(byteBuffer);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != frame) {
                frame.release();
            }
        }

        return null;
    }
}
