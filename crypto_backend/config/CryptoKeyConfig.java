package com.chinaums.spread.common.crypto.config;

import com.chinaums.spread.common.crypto.enums.CryptoKeyType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "crypto")
public class CryptoKeyConfig {

    // 盐值, sha和md5等哈希签名使用
    private String salt;

    // 对称密钥类型：随机生成、文件存储、cache存储
    private CryptoKeyType symmetryKeyType;

    // 对称密钥, des和aes等对称加密方式使用
    // 如果symmetryKeyType为cache存储，则该值为密钥存储路径
    // 如果symmetryKeyType为文件存储，则该值为密钥
    private String symmetryKey;

    // 非对称密钥类型：文件存储、cache存储
    private CryptoKeyType asymmetryKeyType;
    // 非对称加密公钥定位符
    private String pubKeyLocator;
    // 非对称加密私钥定位符
    private String prvKeyLocator;
}
