package com.chinaums.spread.common.crypto;

import com.chinaums.spread.common.crypto.annotation.WebEncrypt;
import com.chinaums.spread.common.util.SpringContextUtils;
import com.chinaums.spread.common.crypto.enums.CryptoType;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.lang.reflect.Method;

@Order(-1)
@ControllerAdvice
@RequiredArgsConstructor
public class WebEncryptAdvice implements ResponseBodyAdvice<Object> {


    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
         return this.checkEncrypt(returnType);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null) {
            return null;
        }
        response.getHeaders().setContentType(MediaType.TEXT_PLAIN);
        Method method = returnType.getMethod();
        WebEncrypt webEncrypt;
        if (null != method) {
            if (WebCryptoSupport.hasCryptoAnnotation(method, CryptoType.ENCRYPT)) {
                webEncrypt = method.getAnnotation(WebEncrypt.class);
                if (null != webEncrypt) {
                    WebCryptoContext context = new WebCryptoContext(SpringContextUtils.getBean(webEncrypt.strategy().getBean()));
                    return context.encrypt(body, request, response);
                }
            } else {
                Class<?> clz = returnType.getDeclaringClass();
                if (WebCryptoSupport.hasCryptoAnnotation(clz, CryptoType.ENCRYPT)) {
                    webEncrypt = clz.getAnnotation(WebEncrypt.class);
                    if (null != webEncrypt) {
                        WebCryptoContext context = new WebCryptoContext(SpringContextUtils.getBean(webEncrypt.strategy().getBean()));
                        return context.encrypt(body, request, response);
                    }
                }
            }
        } else {
            Class<?> clz = returnType.getDeclaringClass();
            if (WebCryptoSupport.hasCryptoAnnotation(clz, CryptoType.ENCRYPT)) {
                webEncrypt = clz.getAnnotation(WebEncrypt.class);
                if (null != webEncrypt) {
                    WebCryptoContext context = new WebCryptoContext(SpringContextUtils.getBean(webEncrypt.strategy().getBean()));
                    return context.encrypt(body, request, response);
                }
            }
        }
        return null;
    }

    public boolean checkEncrypt(MethodParameter returnType) {
        Class<?> declaringClass = returnType.getDeclaringClass();
        if (WebCryptoSupport.hasCryptoAnnotation(declaringClass, CryptoType.ENCRYPT)) {
            return true;
        }
        Method method = returnType.getMethod();
        if (method != null) {
            return WebCryptoSupport.hasCryptoAnnotation(method, CryptoType.ENCRYPT);
        }
        return false;
    }
}
