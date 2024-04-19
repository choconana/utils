package com.chinaums.spread.common.crypto.strategy;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * 云闪付加密策略
 */
@Slf4j
@Component(value = "ysfCryptoStrategy")
public class YSFCryptoStrategy extends AbstractWebCrypto {
    @Override
    public JSONObject decrypt(Object body, ServletRequest request, ServletResponse response) {
        return null;
    }

    @Override
    public Object encrypt(Object body, ServerHttpRequest request, ServerHttpResponse response) {
        return null;
    }
}
