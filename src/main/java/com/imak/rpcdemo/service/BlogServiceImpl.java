package com.imak.rpcdemo.service;


import com.imak.rpcdemo.common.Blog;
import com.imak.rpcdemo.retry.Idempotent;


public class BlogServiceImpl implements BlogService {
    @Override
    @Idempotent
    public Blog getBlogById(Integer id) {
        Blog blog = Blog.builder().id(id).title("我的博客").useId(22).build();
        System.out.println("客户端查询了"+id+"博客");
        return blog;
    }
}
