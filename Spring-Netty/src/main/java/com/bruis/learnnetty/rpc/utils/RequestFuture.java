package com.bruis.learnnetty.rpc.utils;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 模拟客户端请求类，用于构建请求对象
 *
 * @author lhy
 * @date 2022/2/10
 */
public class RequestFuture {
    public static Map<Long, RequestFuture> futures = new ConcurrentHashMap<>();
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private long id;
    /**
     * 请求参数
     */
    private Object request;
    /**
     * 响应结果
     */
    private Object result;
    /**
     * 超时时间
     */
    private long timeout = 5000;
    /**
     * 请求路径
     */
    private String path;

    public static final AtomicLong aid = new AtomicLong();

    public RequestFuture() {
        id = aid.incrementAndGet();
        addFuture(this);
    }

    /**
     * 把请求放入本地缓存中
     * @param future
     */
    public static void addFuture(RequestFuture future) {
        futures.put(future.getId(), future);
    }

    /**
     * 同步获取响应结果
     * @return
     */
    public Object get() {
        lock.lock();
        try {
            while (this.result == null) {
                try {
                    // 主线程默认等待5s，然后查看下结果
                    condition.await(timeout, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            lock.unlock();
        }
        return this.result;
    }

    /**
     * 异步线程将结果返回主线程
     * @param result
     */
    public static void received(Response result) {
        RequestFuture future = futures.remove(result.getId());
        if (null != future) {
            future.setResult(result.getResult());
        }
        /**
         * 通知主线程
         */
        Objects.requireNonNull(future, "RequestFuture").getLock().lock();
        try {
            future.getCondition().signalAll();
        } finally {
            Objects.requireNonNull(future, "RequestFuture").getLock().unlock();
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Object getRequest() {
        return request;
    }

    public void setRequest(Object request) {
        this.request = request;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public Lock getLock() {
        return lock;
    }

    public Condition getCondition() {
        return condition;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
