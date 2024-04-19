package com.chinaums.spread.common.crypto.strategy;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

@Slf4j
@Component(value = "noneCryptoStrategy")
public class NoneCryptoStrategy extends AbstractWebCrypto {
    @Override
    public JSONObject decrypt(Object body, ServletRequest request, ServletResponse response) {
        String url = ((HttpServletRequest) request).getRequestURL().toString();
        log.info("{}请求为非加密策略", url);
        return JSON.parseObject((String) body);
    }

    @Override
    public Object encrypt(Object body, ServerHttpRequest request, ServerHttpResponse response) {
        log.info("{}请求为非加密策略", request.getURI());
        HttpHeaders headers = response.getHeaders();
        headers.add("s-info", "no");
        return body;
    }
}
