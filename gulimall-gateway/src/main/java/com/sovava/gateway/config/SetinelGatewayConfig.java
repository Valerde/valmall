package com.sovava.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.fastjson2.JSON;
import com.sovava.common.utils.R;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
public class SetinelGatewayConfig {

    public SetinelGatewayConfig(){
        GatewayCallbackManager.setBlockHandler(new BlockRequestHandler() {
            //网关先溜了就会调用此回调
            @Override
            public Mono<ServerResponse> handleRequest(ServerWebExchange serverWebExchange, Throwable throwable) {

                R error = R.error(123, "请求过多");
                String s = JSON.toJSONString(error);
                Mono<ServerResponse> serverResponseMono = ServerResponse.ok().body(Mono.just(s), String.class);

                return serverResponseMono;
            }
        });
    }
}
