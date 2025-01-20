package com.imak.rpcdemo.server;


import com.imak.rpcdemo.service.BlogService;
import com.imak.rpcdemo.service.BlogServiceImpl;
import com.imak.rpcdemo.service.UserService;
import com.imak.rpcdemo.service.UserServiceImpl;

public class TestServer {
    public static void main(String[] args) throws InterruptedException {
        UserService userService = new UserServiceImpl();
        BlogService blogService = new BlogServiceImpl();

        ServiceProvider serviceProvider = new ServiceProvider("127.0.0.1",8899);
        serviceProvider.provideServiceInterface(userService);
        serviceProvider.provideServiceInterface(blogService);

        RPCServer RPCServer = new NettyRPCServer(serviceProvider);
        RPCServer.start(8899);
    }
}
