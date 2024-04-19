package com.chinaums.spread.common.crypto.strategy;

import com.chinaums.spread.common.crypto.WebCryptoSupport;
import com.chinaums.spread.common.crypto.config.CryptoKeyConfig;
import com.chinaums.spread.common.crypto.enums.CryptoMode;
import com.chinaums.spread.common.crypto.exception.CryptoException;
import com.chinaums.spread.common.crypto.key.AsymmetryKeyWrapper;
import com.chinaums.spread.common.crypto.key.KeyPair;
import com.chinaums.spread.common.crypto.key.SymmetryKeyWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.NoSuchAlgorithmException;

@Slf4j
@Component
public abstract class AbstractWebCrypto implements IWebCrypto {
    @Autowired
    protected CryptoKeyConfig keyConfig;
    @Autowired
    protected WebCryptoSupport webCryptoSupport;

    protected String getSymmetryKey(long version) {
        String secretKey = null;
        long symkVersion = WebCryptoSupport.LATEST_VERSION == version ?
                version : WebCryptoSupport.resolveKeyVersion(version, CryptoMode.AES);
        switch (keyConfig.getSymmetryKeyType()) {
            case RANDOM:
                secretKey = WebCryptoSupport.generate16ByteRandom();
                break;
            case CACHE:
                SymmetryKeyWrapper keyWrapper = WebCryptoSupport.LATEST_VERSION == symkVersion
                        ? webCryptoSupport.getLatestSymmetryKeyFromCache()
                        : webCryptoSupport.getSymmetryKeyFromCache(symkVersion, keyConfig.getSymmetryKey());
                secretKey = null == keyWrapper ? null : keyWrapper.getSecretKey();
                break;
            case FILE:
                secretKey = keyConfig.getSymmetryKey();
                break;
            default:
        }
        if (StringUtils.isBlank(secretKey)) {
            log.error("未获取到密钥，客户端需要刷新");
            throw new CryptoException("请求处理失败，请刷新");
        }
        return secretKey;
    }

    protected KeyPair getAsymmetryKey(long version) throws NoSuchAlgorithmException {
        KeyPair keyPair = null;
        long asymkVersion = WebCryptoSupport.LATEST_VERSION == version ?
                version : WebCryptoSupport.resolveKeyVersion(version, CryptoMode.RSA);
        switch (keyConfig.getAsymmetryKeyType()) {
            case FILE:
                // 暂不支持
                break;
            case CACHE:
                AsymmetryKeyWrapper keyWrapper = WebCryptoSupport.LATEST_VERSION == asymkVersion
                        ? webCryptoSupport.getLatestAsymmetryKeyFromCache()
                        : webCryptoSupport.getAsymmetryKeyFromCache(asymkVersion, this.keyConfig.getPubKeyLocator());
                keyPair = null == keyWrapper ? null : keyWrapper.getKeyPair();
                break;
            default:
        }
        if (null == keyPair) {
            log.error("未获取到密钥，客户端需要刷新");
            throw new CryptoException("请求处理失败，请刷新");
        }
        return keyPair;
    }
}
