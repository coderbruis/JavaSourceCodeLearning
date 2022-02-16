package com.bruis.learnnetty.netty.connections.shortconnections;

import com.alibaba.fastjson.JSONObject;
import com.bruis.learnnetty.thread.synchronize.Response;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Promise;

/**
 * @author lhy
 * @date 2022/2/11
 */
public class ClientHandler extends ChannelInboundHandlerAdapter {
    private Promise<Response> promise;
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Response response = JSONObject.parseObject(msg.toString(), Response.class);
        promise.setSuccess(response);
    }

    public Promise<Response> getPromise() {
        return promise;
    }

    public void setPromise(Promise<Response> promise) {
        this.promise = promise;
    }
}
