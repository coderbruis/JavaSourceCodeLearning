package com.bruis.learnnetty.thread.reentranlock;

import java.util.ArrayList;
import java.util.List;

/**
 * 模拟Netty通讯过程
 * 主线程，获取子线程的结果
 *
 * @author lhy
 * @date 2022/2/10
 */
public class FutureMain {
    private static List<RequestFuture> reqs = new ArrayList<>();
    public static void main(String[] args) {
        mockClient();
        mockServer();
    }

    /**
     * 模拟服务端  接受结果
     */
    private static void mockServer() {
        for (RequestFuture req : reqs) {
            /**
             * 主线程获取结果
             */
            Object result = req.get();
            System.out.println("服务端接受到响应结果：" + result.toString());
        }
    }
    /**
     * 模拟客户端  发送请求
     */
    private static void mockClient() {
        for (int i = 0; i < 100; i++) {
            long id = i;
            RequestFuture req = new RequestFuture();
            req.setId(id);
            req.setRequest("hello world");
            /**
             * 把请求缓存起来
             */
            RequestFuture.addFuture(req);
            /**
             * 将请求放入到请求列表中
             */
            reqs.add(req);
            sendMsg(req);
            SubThread subThread = new SubThread(req);
            /**
             * 开启子线程
             */
            subThread.start();
        }
    }
    /**
     * 模拟请求处理
     * @param req
     */
    private static void sendMsg(RequestFuture req) {
        System.out.println("客户端发送数据，请求id为===============" + req.getId());
    }
}
