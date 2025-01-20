package com.imak.rpcdemo.server.impl;

import com.imak.rpcdemo.common.Blog;
import com.imak.rpcdemo.service.BlogService;

/**
 * @version 1.0
 * @Author Imak
 * @Date 2025/1/13 21:54
 * @博客服务实现类
 */
public class BlogServiceImpl implements BlogService {
    @Override
    public Blog getBlogById(Integer id) {
        Blog blog = Blog.builder().id(id).title("我的博客").useId(22).build();
        System.out.println("客户端查询了"+id+"博客");
        return blog;
    }
}
