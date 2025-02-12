package com.imak.rpcdemo.service;


import com.imak.rpcdemo.common.User;
import com.imak.rpcdemo.retry.Idempotent;

//幂等性注解

public class UserServiceImpl implements UserService {
    @Override
    @Idempotent
    public User getUserByUserId(Integer id) {
        // 模拟从数据库中取用户的行为
        User user = User.builder().id(id).userName("he2121").sex(true).build();
        System.out.println("客户端查询了"+id+"用户");
        return user;
    }

    @Override
    public Integer insertUserId(User user) {
        System.out.println("插入数据成功："+user);
        return 1;
    }
}
