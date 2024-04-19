package com.chinaums.spread.common.crypto.key;

import lombok.Getter;

@Getter
public class SymmetryKeyWrapper extends KeyWrapper {

    private final String secretKey;

    public SymmetryKeyWrapper(String secretKey, long version, long expireTime) {
        this.secretKey = secretKey;
        this.version = version;
        this.expireTime = expireTime;
    }

    public SymmetryKeyWrapper(String secretKey, long version) {
        this.secretKey = secretKey;
        this.version = version;
        this.expireTime = -1;
    }
}
