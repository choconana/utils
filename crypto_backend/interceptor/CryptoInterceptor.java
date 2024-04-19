package com.chinaums.spread.common.crypto.interceptor;

import com.alibaba.fastjson.JSONObject;
import com.chinaums.spread.common.crypto.WebCryptoContext;
import com.chinaums.spread.common.crypto.WebCryptoSupport;
import com.chinaums.spread.common.crypto.annotation.WebDecrypt;
import com.chinaums.spread.common.crypto.constants.HttpHeaderConstant;
import com.chinaums.spread.common.crypto.enums.CryptoMode;
import com.chinaums.spread.common.crypto.enums.CryptoStrategy;
import com.chinaums.spread.common.crypto.enums.CryptoType;
import com.chinaums.spread.common.crypto.strategy.ALSCryptoStrategy;
import com.chinaums.spread.common.util.SpringContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class CryptoInterceptor implements HandlerInterceptor {

    @Autowired
    protected WebCryptoSupport webCryptoSupport;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String url = request.getRequestURL().toString();
        if (url.contains("alive")) {
            this.getEncryptKeys(response);
            return true;
        }

        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        if (WebCryptoSupport.hasCryptoAnnotation(method, CryptoType.DECRYPT)) {
            return this.decrypt(request, response, method, method);
        } else {
            Class<?> clz = method.getDeclaringClass();
            if (WebCryptoSupport.hasCryptoAnnotation(clz, CryptoType.ENCRYPT)) {
               return this.decrypt(request, response, clz, method);
            }
        }
        return true;
    }

    private boolean decrypt(HttpServletRequest request,
                            HttpServletResponse response,
                            AnnotatedElement annotatedElement,
                            Method method) throws IOException {
        WebDecrypt webDecrypt = annotatedElement.getAnnotation(WebDecrypt.class);
        if (null != webDecrypt) {
            String url = request.getRequestURL().toString();
            if (CryptoStrategy.NONE == webDecrypt.strategy()) {
                log.info("请求[{}]无需加解密", url);
                return true;
            }
            log.info("请求解密中, url: {}", url);
            String requestMethod = request.getMethod();
            CryptoRequestWrapper cryptoRequestWrapper = (CryptoRequestWrapper) request;
            WebCryptoContext context = new WebCryptoContext(SpringContextUtils.getBean(webDecrypt.strategy().getBean()));
            if(requestMethod.equals("GET")) {
                String encode = request.getParameter("param");
                if (StringUtils.isBlank(encode)) {
                    this.crytoFailResponse(url, response);
                    return false;
                }
                JSONObject raw = context.decrypt(encode, request, response);
                if (null == raw) {
                    this.crytoFailResponse(url, response);
                    return false;
                }
                cryptoRequestWrapper.addAllParameters(raw.getInnerMap());
                cryptoRequestWrapper.removeParameter("param");
            } else if(requestMethod.equals("POST")) {
                String encode = cryptoRequestWrapper.getBodyString();
                JSONObject raw = context.decrypt(encode, request, response);
                if (null != raw) {
                    Map<String, Object> paramMap = new HashMap<>();
                    Annotation[][] parameterAnnotations = method.getParameterAnnotations();
                    for (Annotation[] parameterAnnotation : parameterAnnotations) {
                        for (Annotation annotation : parameterAnnotation) {
                            if (annotation.annotationType().equals(RequestParam.class)) {
                                RequestParam requestParam = (RequestParam) annotation;
                                String paramName = requestParam.value();
                                paramMap.put(paramName, raw.get(paramName));
                            }
                        }
                    }
                    if (!paramMap.isEmpty()) {
                        cryptoRequestWrapper.addAllParameters(paramMap);
                    }
                    cryptoRequestWrapper.setBody(String.valueOf(raw));
                }
            }
            log.info("请求解密成功, url: {}", url);
        }
        return true;
    }

    private void getEncryptKeys(HttpServletResponse response) throws IOException {
        ALSCryptoStrategy alsCryptoStrategy = (ALSCryptoStrategy) SpringContextUtils.getBean(CryptoStrategy.ALS.getBean());
        long curKeyVersion = webCryptoSupport.getCurKeyVersion(CryptoMode.NULL);
        String[] keys = alsCryptoStrategy.getEncryptKeys(curKeyVersion);
        if (WebCryptoSupport.resolveKeyVersion(curKeyVersion, CryptoMode.RSA) == 0
                || WebCryptoSupport.resolveKeyVersion(curKeyVersion, CryptoMode.AES) == 0) {
            curKeyVersion = webCryptoSupport.getCurKeyVersion(CryptoMode.NULL);
        }
        response.setHeader(HttpHeaderConstant.ENCODE_SYMMETRY_KEY, keys[1]);
        response.setHeader(HttpHeaderConstant.URLENCODE_PUBLIC_KEY, keys[2]);
        response.setHeader(HttpHeaderConstant.KEY_VERSION, String.valueOf(curKeyVersion));
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");
        JSONObject res = new JSONObject();
        res.put("code", "200");
        res.put("data", StringUtils.rightPad(String.valueOf(System.currentTimeMillis()), 16, '0'));
        res.put("msg", "success");
        response.getWriter().write(res.toString());
    }

    private void crytoFailResponse(String url, HttpServletResponse response) throws IOException {
        log.error("加密接口请求[{}]未正确加密", url);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");
        JSONObject res = new JSONObject();
        res.put("code","500");
        res.put("msg","请求数据处理异常!");
        response.getWriter().write(res.toString());
    }
}
