package com.bruis.learnnetty.netty.connections.shortconnections;

import com.alibaba.fastjson.JSONObject;
import com.bruis.learnnetty.thread.synchronize.RequestFuture;
import com.bruis.learnnetty.thread.synchronize.Response;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @author lhy
 * @date 2022/2/11
 */
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
        long id = request.getId();
        Response response = new Response();
        response.setId(id);
        response.setResult("我是服务端(" + id + ")");
        /**
         * 给客户端发送响应结果
         */
        ctx.channel().writeAndFlush(JSONObject.toJSONString(response));
    }
}
