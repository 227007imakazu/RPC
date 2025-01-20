package com.imak.rpcdemo.server;



import com.imak.rpcdemo.limit.RateLimitProvider;
import com.imak.rpcdemo.register.ServiceRegister;
import com.imak.rpcdemo.register.ZkServiceRegister;
import com.imak.rpcdemo.retry.Idempotent;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * 存放服务接口名与服务端对应的实现类
 * 服务启动时要暴露其相关的实现类
 * 根据request中的interface调用服务端中相关实现类
 */
public class ServiceProvider {
    private Map<String,Object> interfaceProvider;



    private int port;
    private String host;
    //注册服务类
    private ServiceRegister serviceRegister;

    public ServiceProvider(String host,int port) throws InterruptedException {
        //需要传入服务端自身的网络地址
        this.host=host;
        this.port=port;
        this.interfaceProvider=new HashMap<>();
        this.serviceRegister=new ZkServiceRegister();
    }

    public void provideServiceInterface(Object service){
        String serviceName=service.getClass().getName();
        Class<?>[] interfaceName=service.getClass().getInterfaces();

        for (Class<?> clazz:interfaceName){
            //本机的映射表
            interfaceProvider.put(clazz.getName(),service);
            //在注册中心注册服务,其实就是提供可以找到该服务的一系列信息组成的地址(服务名，服务类字节码(用于反射)，主机号，端口号)
            //还要提供服务的幂等性
            boolean canRetry=true;
            //反射获取是否幂等
            //只有所有方法都是幂等的，才能进行重试
            for(Method method:clazz.getMethods()){
                if(!method.isAnnotationPresent(Idempotent.class)){
                    canRetry=false;
                    break;
                }
            }
            serviceRegister.register(clazz.getName(),new InetSocketAddress(host,port),canRetry);
        }
    }

    public Object getService(String interfaceName){
        return interfaceProvider.get(interfaceName);
    }

    public RateLimitProvider getRateLimitProvider() {
        return new RateLimitProvider();
    }
}
