package com.imak.rpcdemo.register;

import com.google.common.cache.Cache;
import com.imak.rpcdemo.client.cache.serviceCache;
import com.imak.rpcdemo.loadbalance.LoadBalance;
import com.imak.rpcdemo.loadbalance.RandomLoadBalance;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.net.InetSocketAddress;
import java.util.List;

import static org.apache.curator.SessionFailRetryLoop.Mode.RETRY;

public class ZkServiceRegister implements ServiceRegister {
    // curator 提供的zookeeper客户端
    private CuratorFramework client;
    // zookeeper根路径节点
    private static final String ROOT_PATH = "MyRPC";
    // 初始化负载均衡器， 这里用的是随机， 一般通过构造函数传入
    private LoadBalance loadBalance = new RandomLoadBalance();

    private serviceCache cache;

    //负责zookeeper客户端的初始化，并与zookeeper服务端进行连接
    public ZkServiceRegister() throws InterruptedException {
        // 指数时间重试
        RetryPolicy policy = new ExponentialBackoffRetry(1000, 3);
        // zookeeper的地址固定，不管是服务提供者还是消费者都要与之建立连接
        // sessionTimeoutMs 与 zoo.cfg中的tickTime 有关系，
        // zk还会根据minSessionTimeout与maxSessionTimeout两个参数重新调整最后的超时值。默认分别为tickTime 的2倍和20倍
        // 使用心跳监听状态
        this.client = CuratorFrameworkFactory.builder().connectString("127.0.0.1:2181")
                .sessionTimeoutMs(40000).retryPolicy(policy).namespace(ROOT_PATH).build();
        this.client.start();
        System.out.println("zookeeper 连接成功");
        //初始化本地缓存
        cache=new serviceCache();
        //加入zookeeper事件监听器
        watchZK watcher=new watchZK(client,cache);
        //监听启动
        watcher.watchToUpdate(ROOT_PATH);
    }

    @Override
    public void register(String serviceName, InetSocketAddress serverAddress,boolean canRetry){
        try {
            // serviceName创建成永久节点，服务提供者下线时，不删服务名，只删地址
            if(client.checkExists().forPath("/" + serviceName) == null){
                client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath("/" + serviceName);
            }
            // 路径地址，一个/代表一个节点
            String path = "/" + serviceName +"/"+ getServiceAddress(serverAddress);
            // 临时节点，服务器下线就删除节点
            client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path);
            //如果服务是幂等的，将服务名加入到节点
            if(canRetry){
                path = "/" + RETRY + "/" + serviceName;
                client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path);
            }
        } catch (Exception e) {
            System.out.println("此服务已存在");
        }
    }
    // 根据服务名返回地址,服务发现
    @Override
    public InetSocketAddress serviceDiscovery(String serviceName) {
        try {
            //先查缓存
            List<String> serviceList = cache.getServiceFromCache(serviceName);
            if(serviceList==null){
                //缓存中没有再去zookeeper中查找
                serviceList = client.getChildren().forPath("/" + serviceName);
                //这里不直接缓存服务，因为可能新的服务永远无法被感知，始终使用缓存中的服务
                //而是使用watcher监听zookeeper中服务的变化，当服务发生变化时，更新缓存
            }
            // 负载均衡选择器，选择一个
            String string = loadBalance.balance(serviceList);
            return parseAddress(string);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 地址 -> XXX.XXX.XXX.XXX:port 字符串
    private String getServiceAddress(InetSocketAddress serverAddress) {
        return serverAddress.getHostName() +
                ":" +
                serverAddress.getPort();
    }
    // 字符串解析为地址
    private InetSocketAddress parseAddress(String address) {
        String[] result = address.split(":");
        return new InetSocketAddress(result[0], Integer.parseInt(result[1]));
    }

    //检查是否可以重试
    public boolean checkRetry(String serviceName) {
        boolean canRetry =false;
        try {
            //获取白名单列表
            //会从 ZooKeeper 中获取指定路径（"/" + RETRY）下的所有子节点的列表。
            List<String> serviceList = client.getChildren().forPath("/" + RETRY);
            for(String s:serviceList){
                //如果列表中有该服务
                if(s.equals(serviceName)){
                    System.out.println("服务"+serviceName+"在白名单上，可进行重试");
                    canRetry=true;
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return canRetry;
    }

}
