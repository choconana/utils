package com.chinaums.spread.common.crypto.strategy;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chinaums.spread.common.crypto.WebCryptoSupport;
import com.chinaums.spread.common.crypto.constants.HttpHeaderConstant;
import com.chinaums.spread.common.crypto.exception.CryptoException;
import com.chinaums.spread.common.crypto.key.KeyPair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

import static com.chinaums.spread.common.constant.HenanRedisConstants.HENAN_REDIS_KEY_PREFIX;

/**
 * API-Layer-Security
 * API层面安全策略
 */
@Slf4j
@Primary
@Scope(value = "prototype")
@Component(value = "alsCryptoStrategy")
public class ALSCryptoStrategy extends AbstractWebCrypto {

    // api请求超时时间
    private static final long API_EXPIRE = 3 * 60 * 1000;

    // 请求重放攻击检测redis key
    private static final String REPLAY_ATTACK = HENAN_REDIS_KEY_PREFIX + "replay_attack_detect:";

    @Override
    public JSONObject decrypt(Object body, ServletRequest request, ServletResponse response) {
        log.info("当前使用ALS加密策略");
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String reqPath = ((HttpServletRequest) request).getServletPath();
        String url = ((HttpServletRequest) request).getRequestURL().toString();
        if (null == body) {
            log.error("{}请求体为空", url);
            return null;
        }
        JSONObject raw;
        String method = ((HttpServletRequest) request).getMethod();
        Object encryptBody;
        if (method.equals("GET")) {
            encryptBody = body;
        } else {
            encryptBody = JSON.parseObject(String.valueOf(body)).get("param");
        }
        if (null == encryptBody) {
            log.error("{}请求密文为空", url);
            return null;
        }
        try {
            String decodeBody = URLDecoder.decode(String.valueOf(encryptBody).replace("/", "%"), "UTF-8");
            String iv = Base64.decodeStr(httpRequest.getHeader(HttpHeaderConstant.DECRYPT_SYMMETRY_IV));
            long keyVersion = Long.parseLong(httpRequest.getHeader(HttpHeaderConstant.KEY_VERSION));
            /*
            rawJson: {
                raw: 请求原文,
                whisper: 公钥加密的字符串(timestamp.randomNumber),
                sign: 签名(raw;iv;timestamp.randomNumber)
            }
             */
            String rawBody = WebCryptoSupport.decryptAES(
                    decodeBody,
                    this.getSymmetryKey(keyVersion),
                    iv);
            JSONObject rawJson = JSON.parseObject(rawBody);
            if (null == rawJson) {
                log.error("{}原生请求体为空", url);
                return null;
            }
            String whisper = String.valueOf(rawJson.get("whisper"));
            KeyPair rsa = this.getAsymmetryKey(keyVersion);
            String randomTimestamp = WebCryptoSupport.decryptByPrivateKey(whisper, rsa.getPrivateKey());
            String[] rtsArr = StringUtils.split(randomTimestamp, '.');
            String timestamp = rtsArr[0];
            String randomNumber = rtsArr[1];
            if ((System.currentTimeMillis() - Long.parseLong(timestamp)) > API_EXPIRE) {
                log.error("{}请求超时", url);
                throw new CryptoException("请求超时");
            }
            if (webCryptoSupport.replayAttacksDetect(REPLAY_ATTACK + reqPath + ":" + randomTimestamp, url, API_EXPIRE)) {
                log.error("{}请求疑似遭遇重放攻击", url);
                throw new CryptoException("请求重复");
            }
            // 请求原文
            Object rawParams = rawJson.get("raw");
            raw = JSON.parseObject(String.valueOf(rawParams));
            // 校验请求原文是否被篡改
            Object sign = rawJson.get("sign");
            Object authSign = SecureUtil.md5(rawParams + ";" + iv + ";" + randomTimestamp);
            if (!sign.equals(authSign)) {
                log.error("{}请求签名校验失败", url);
                throw new CryptoException("请求处理失败");
            }
        } catch (Throwable ex) {
            log.error("解密失败:\n{}", ExceptionUtils.getFullStackTrace(ex));
            if (ex instanceof CryptoException) {
                throw new CryptoException(((CryptoException) ex).getMsg());
            }
            throw new CryptoException("请求处理失败");
        }
        return raw;
    }

    @Override
    public Object encrypt(Object body, ServerHttpRequest request, ServerHttpResponse response) {
        log.info("当前使用ALS解密策略");
        HttpHeaders requestHeaders = request.getHeaders();
        HttpHeaders responseHeaders = response.getHeaders();
        long requestVersion = Long.parseLong(requestHeaders.get(HttpHeaderConstant.KEY_VERSION).get(0));
        String[] keys = this.getAndSetEncryptKeys2Header(requestVersion, responseHeaders);
        String raw = JSON.toJSONString(body);
        String encode;
        try {
            // 响应原文进行对称加密，对称密钥由非对称私钥加密
            encode = WebCryptoSupport.encryptAES(raw, keys[0], keys[4]);
        } catch (GeneralSecurityException e) {
            log.error("AES加密失败:\n{}", ExceptionUtils.getFullStackTrace(e));
            throw new CryptoException("响应数据处理失败");
        }
        return encode;
    }

    public String[] getEncryptKeys(long version) {
        KeyPair rsa;
        try {
            rsa = this.getAsymmetryKey(version);
        } catch (NoSuchAlgorithmException e) {
            log.error("读取RSA失败:\n{}", ExceptionUtils.getFullStackTrace(e));
            throw new CryptoException("响应数据处理失败");
        }
        String symmetryKey = this.getSymmetryKey(version);
        String encodeKey;
        try {
            encodeKey = WebCryptoSupport.encryptByPrivateKey(
                    Base64.encode(symmetryKey.getBytes(StandardCharsets.UTF_8)),
                    rsa.getPrivateKey());
        } catch (Exception e) {
            log.error("RSA加密失败:\n{}", ExceptionUtils.getFullStackTrace(e));
            throw new CryptoException("响应数据处理失败");
        }
        String urlEncodePubKey;
        String randomSalt = WebCryptoSupport.generate16ByteRandom();
        String pubKeySignature = SecureUtil.md5(rsa.getPublicKey() + this.keyConfig.getSalt() + randomSalt);
        String iv = WebCryptoSupport.generate16ByteRandom();
        try {
            urlEncodePubKey = URLEncoder.encode(rsa.getPublicKey(), "UTF-8").replace("%", "/");
        } catch (UnsupportedEncodingException e) {
            log.error("AES加密失败:\n{}", ExceptionUtils.getFullStackTrace(e));
            throw new CryptoException("响应数据处理失败");
        }
        return new String[]{symmetryKey, encodeKey, urlEncodePubKey, pubKeySignature, iv, randomSalt};
    }

    /**
     * 对称密钥、非对称密钥公钥、非对称加密的对称密钥
     * @param version 密钥版本
     * @return 密钥数组
     */
    public String[] getMajorKeys(long version) {
        KeyPair rsa = this.getAsymmetryKeyPair(version);
        String symmetryKey = this.getSymmetryKey(version);
        String encodeKey;
        String urlEncodePubKey;
        try {
            encodeKey = WebCryptoSupport.encryptByPrivateKey(
                    Base64.encode(symmetryKey.getBytes(StandardCharsets.UTF_8)),
                    rsa.getPrivateKey());
            urlEncodePubKey = URLEncoder.encode(rsa.getPublicKey(), "UTF-8").replace("%", "/");
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            log.error("RSA加密失败:\n{}", ExceptionUtils.getFullStackTrace(e));
            throw new CryptoException("响应数据处理失败");
        }
        return new String[]{symmetryKey, encodeKey, urlEncodePubKey};
    }

    public String[] getRandomKeys(long version) {
        KeyPair rsa = this.getAsymmetryKeyPair(version);
        String randomSalt = WebCryptoSupport.generate16ByteRandom();
        String pubKeySignature = SecureUtil.md5(rsa.getPublicKey() + this.keyConfig.getSalt() + randomSalt);
        String iv = WebCryptoSupport.generate16ByteRandom();
        return new String[]{pubKeySignature, iv, randomSalt};
    }

    public KeyPair getAsymmetryKeyPair(long version) {
        KeyPair rsa;
        try {
            rsa = this.getAsymmetryKey(version);
        } catch (NoSuchAlgorithmException e) {
            log.error("读取RSA失败:\n{}", ExceptionUtils.getFullStackTrace(e));
            throw new CryptoException("响应数据处理失败");
        }
        return rsa;
    }

    public String[] getAndSetEncryptKeys2Header(long requestVersion, HttpHeaders headers) {
        String[] keys = this.getEncryptKeys(requestVersion);
        headers.add(HttpHeaderConstant.PUBLIC_KEY_SIGNATURE, keys[3]);
        headers.add(HttpHeaderConstant.ENCRYPT_SYMMETRY_IV, keys[4]);
        headers.add(HttpHeaderConstant.RANDOM_SALT, keys[5]);
        headers.add(HttpHeaderConstant.API_ENCRYPT_INFO_NAME, HttpHeaderConstant.API_ENCRYPT_INFO_VALUE_YES);
        return keys;
    }
}