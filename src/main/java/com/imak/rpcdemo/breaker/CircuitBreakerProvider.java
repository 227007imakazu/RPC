package com.imak.rpcdemo.breaker;

import java.util.HashMap;
import java.util.Map;
//熔断器提供者 维护不同服务的熔断器
public class CircuitBreakerProvider {
    private Map<String,CircuitBreaker> circuitBreakerMap=new HashMap<>();

    public CircuitBreaker getCircuitBreaker(String serviceName){
        CircuitBreaker circuitBreaker;
        if(circuitBreakerMap.containsKey(serviceName)){
            circuitBreaker=circuitBreakerMap.get(serviceName);
        }else {
            circuitBreaker=new CircuitBreaker(3,0.5,10000);
        }
        return circuitBreaker;
    }
}
