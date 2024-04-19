package com.chinaums.spread.common.crypto.strategy;

import com.alibaba.fastjson.JSONObject;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public interface IWebCrypto {
    JSONObject decrypt(Object body, ServletRequest request, ServletResponse response);

    Object encrypt(Object body, ServerHttpRequest request, ServerHttpResponse response);
}
