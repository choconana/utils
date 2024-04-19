package com.chinaums.spread.common.crypto.enums;

import cn.hutool.crypto.asymmetric.KeyType;
import lombok.Getter;

public enum CryptoMode {
    NULL,

    MD5,

    SHA256,

    AES,

    DES,

    RSA,

    ;

    @Getter
    public enum RSAKeyType {
        NULL(null),
        PUB(KeyType.PublicKey),
        PRV(KeyType.PrivateKey),
        ;

        RSAKeyType(KeyType keyType) {
            this.keyType = keyType;
        }

        private KeyType keyType;
    }

}
