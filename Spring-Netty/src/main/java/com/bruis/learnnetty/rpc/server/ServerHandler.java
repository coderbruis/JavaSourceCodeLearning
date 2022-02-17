package com.bruis.learnnetty.rpc.server;

import com.alibaba.fastjson.JSONObject;
import com.bruis.learnnetty.rpc.utils.Mediator;
import com.bruis.learnnetty.rpc.utils.RequestFuture;
import com.bruis.learnnetty.rpc.utils.Response;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @author lhy
 * @date 2022/2/11
 */
@ChannelHandler.Sharable
public class ServerHandler extends ChannelInboundHandlerAdapter {
    /**
     * 接受客户端发送过来的请求
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RequestFuture request = JSONObject.parseObject(msg.toString(), RequestFuture.class);
        Response response = Mediator.process(request);
        ctx.channel().writeAndFlush(JSONObject.toJSONString(response));
    }
}
