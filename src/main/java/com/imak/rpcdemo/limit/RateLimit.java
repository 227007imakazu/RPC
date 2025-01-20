package com.imak.rpcdemo.limit;
//限流接口
public interface RateLimit {
    //获取访问许可
    boolean getToken();
}
