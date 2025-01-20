package com.imak.rpcdemo.client;


import com.imak.rpcdemo.common.RPCRequest;
import com.imak.rpcdemo.common.RPCResponse;

public interface RPCClient {
    RPCResponse sendRequest(RPCRequest request);
}
