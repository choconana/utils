package com.chinaums.spread.common.crypto.enums;

import com.chinaums.spread.common.crypto.strategy.*;
import lombok.Getter;

/**
 * 加密策略类枚举
 */
@Getter
public enum CryptoStrategy {
    ALS(ALSCryptoStrategy.class),
    NONE(NoneCryptoStrategy.class),
    MULTISIDE(MultiSideCryptoStrategy.class),
    YSF(YSFCryptoStrategy.class),
    ;

    private final Class<? extends IWebCrypto> bean;

    CryptoStrategy(Class<? extends IWebCrypto> bean) {
        this.bean = bean;
    }
}
