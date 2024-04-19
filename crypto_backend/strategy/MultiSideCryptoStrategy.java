package com.chinaums.spread.common.crypto.strategy;

import com.alibaba.fastjson.JSONObject;
import com.chinaums.spread.common.crypto.WebCryptoContext;
import com.chinaums.spread.common.crypto.constants.HttpHeaderConstant;
import com.chinaums.spread.common.util.SpringContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 多客户端加密策略
 */
@Slf4j
@Scope(value = "prototype")
@Component(value = "multiSideCryptoStrategy")
public class MultiSideCryptoStrategy extends AbstractWebCrypto {

    private static final Map<String, Class<? extends IWebCrypto>> strategyMapping = new HashMap<String, Class<? extends IWebCrypto>>(){
        {
            put("wx", ALSCryptoStrategy.class);
            put("ysf", YSFCryptoStrategy.class);
        }
    };

    @Override
    public JSONObject decrypt(Object body, ServletRequest request, ServletResponse response) {
        String clientSide = ((HttpServletRequest) request).getHeader(HttpHeaderConstant.API_CLIENT_SIDE);
        Class<? extends IWebCrypto> beanClass = strategyMapping.get(clientSide);
        beanClass = beanClass == null ? ALSCryptoStrategy.class : beanClass;
        log.info("当前为{}客户端的请求，使用{}加密策略", clientSide, beanClass.getSimpleName());
        WebCryptoContext context = new WebCryptoContext(SpringContextUtils.getBean(beanClass));
        return context.decrypt(body, request, response);
    }

    @Override
    public Object encrypt(Object body, ServerHttpRequest request, ServerHttpResponse response) {
        String clientSide = Optional.ofNullable(request.getHeaders().get(HttpHeaderConstant.API_CLIENT_SIDE)).orElse(Collections.singletonList("wx")).get(0);
        Class<? extends IWebCrypto> beanClass = strategyMapping.get(clientSide);
        beanClass = beanClass == null ? ALSCryptoStrategy.class : beanClass;
        log.info("当前为{}客户端的请求响应，使用{}加密策略", clientSide, beanClass.getSimpleName());
        WebCryptoContext context = new WebCryptoContext(SpringContextUtils.getBean(beanClass));
        return context.encrypt(body, request, response);
    }

    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        System.out.println(list.get(0));
    }
}
