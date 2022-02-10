package com.learnjava.thread.synchronize;

/**
 * 子线程，用于模拟服务端处理
 *
 * @author lhy
 * @date 2022/2/10
 */
public class SubThread extends Thread {

    private RequestFuture request;

    public SubThread(RequestFuture request) {
        this.request = request;
    }

    @Override
    public void run() {
        Response response = new Response();
        response.setId(request.getId());
        response.setResult("服务端响应了结果，线程id: " + Thread.currentThread().getId() + ", 请求id：" + response.getId());
        // 子线程睡眠1s
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(this + " -> 当前线程准备调用received: " + Thread.currentThread().getName());
        RequestFuture.received(response);
    }
}
