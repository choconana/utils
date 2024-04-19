package com.chinaums.spread.common.crypto;

import com.alibaba.fastjson.JSONObject;
import com.chinaums.spread.common.crypto.strategy.IWebCrypto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;

@Slf4j
public class WebCryptoContext {

    private final IWebCrypto webCrypto;

    public WebCryptoContext(IWebCrypto webCrypto) {
        this.webCrypto = webCrypto;
    }

    public JSONObject decrypt(Object object, ServletRequest request, ServletResponse response) {
        String url = ((HttpServletRequest) request).getRequestURL().toString();
        log.info("[{}]请求密文:\n{}", url, object);
        JSONObject raw = webCrypto.decrypt(object, request, response);
        log.info("[{}]请求原文:\n{}", url, raw);
        return raw;
    }

    public Object encrypt(Object body, ServerHttpRequest request, ServerHttpResponse response) {
        URI uri = request.getURI();
        String url = uri.getAuthority() + uri.getPath();
        log.info("[{}]响应原文:\n{}", url, body);
        Object encrypt = webCrypto.encrypt(body, request, response);
        log.info("[{}]响应密文:\n{}", url, encrypt);
        return encrypt;
    }
}
