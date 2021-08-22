package com.bruis.learnnetty.netty.heartbeat;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.ByteBuffer;

/**
 * @author lhy
 * @date 2021/8/19
 */
@ChannelHandler.Sharable
public class NettyEncoder extends MessageToByteEncoder<RemotingCommand> {

    @Override
    protected void encode(ChannelHandlerContext ctx, RemotingCommand remotingCommand, ByteBuf out) throws Exception {
        try {
            ByteBuffer header = remotingCommand.encodeHeader();
            out.writeBytes(header);
            byte[] body = remotingCommand.getBody();
            if (null != body) {
                out.writeBytes(body);
            }
//            out.writeBytes(remotingCommand.getBody());
        } catch (Exception e) {
            e.printStackTrace();
            ctx.channel().close().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    // 关闭channel成功
                }
            });
        }
    }
}
