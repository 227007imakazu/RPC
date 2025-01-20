package com.imak.rpcdemo.client;

import com.imak.rpcdemo.breaker.CircuitBreaker;
import com.imak.rpcdemo.breaker.CircuitBreakerProvider;
import com.imak.rpcdemo.common.RPCRequest;
import com.imak.rpcdemo.common.RPCResponse;
import com.imak.rpcdemo.register.ServiceRegister;
import com.imak.rpcdemo.register.ZkServiceRegister;
import com.imak.rpcdemo.retry.guavaRetry;
import lombok.AllArgsConstructor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @version 1.0
 * @Author Imak
 * @Date 2025/1/13 20:47
 * @动态代理封装请求
 */
@AllArgsConstructor
public class ClientProxy implements InvocationHandler {
    // 传入参数Service接口的class对象，反射封装成一个request
    private RPCClient client;
    private ServiceRegister serviceRegister;
//    public ClientProxy()throws InterruptedException{ {
//        serviceRegister=new ZkServiceRegister();
//    }
    private CircuitBreakerProvider circuitBreakerProvider;


    // jdk 动态代理， 每一次代理对象调用方法，会经过此方法增强（反射获取request对象，socket发送至客户端）
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RPCRequest request = RPCRequest.builder()
                .interfaceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .params(args).paramsTypes(method.getParameterTypes()).build();
        //获取熔断器
        CircuitBreaker circuitBreaker=circuitBreakerProvider.getCircuitBreaker(method.getName());
        //判断熔断器是否允许请求经过
        if (!circuitBreaker.allowRequest()){
            //这里可以针对熔断做特殊处理，返回特殊值
            return null;
        }
        // 数据传输
        RPCResponse response;
        //后续添加逻辑：为保持幂等性，只对白名单上的服务进行重试
        if (serviceRegister.checkRetry(request.getInterfaceName())){
            //调用retry框架进行重试操作
            response=new guavaRetry().sendServiceWithRetry(request,client);
        }else {
            //只调用一次
            response= client.sendRequest(request);
        }
        //记录response的状态，上报给熔断器
        if (response.getCode() ==200){
            circuitBreaker.recordSuccess();
        }
        if (response.getCode()==500){
            circuitBreaker.recordFailure();
        }
        return response.getData();

    }

    // 通过动态代理获取到Service的代理对象
    // proxy存储代理对象的引用，通过代理对象调用方法时，会调用invoke方法，发送请求到服务端
    <T>T getProxy(Class<T> clazz){
        Object o = Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, this);
        return (T)o;
    }
}
