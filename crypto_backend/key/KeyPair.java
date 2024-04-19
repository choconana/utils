package com.chinaums.spread.common.crypto.key;

import lombok.Getter;
import lombok.Setter;

@Getter
public class KeyPair {

    private final String publicKey;

    private final String privateKey;

    public KeyPair(String publicKey, String privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

}
