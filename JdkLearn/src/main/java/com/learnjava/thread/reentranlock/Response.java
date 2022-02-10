package com.learnjava.thread.reentranlock;

/**
 * 响应结果类
 *
 * @author lhy
 * @date 2022/2/10
 */
public class Response {
    private long id;
    private Object result;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
