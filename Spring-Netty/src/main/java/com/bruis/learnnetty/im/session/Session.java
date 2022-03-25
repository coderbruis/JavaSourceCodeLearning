package com.bruis.learnnetty.im.session;

/**
 * @Description
 * @Author luohaiyang
 * @Date 2022/3/23
 */
public class Session {

    private String userId;

    private String userName;

    public Session(String userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public String toString() {
        return userId + "->" + userName;
    }
}