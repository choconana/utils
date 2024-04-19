package com.chinaums.spread.common.crypto.key;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.Getter;
import lombok.Setter;

@Getter
public class AsymmetryKeyWrapper extends KeyWrapper {

    private final String publicKey;

    private final String privateKey;

    public AsymmetryKeyWrapper(String publicKey, String privateKey, long version, long expireTime) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.version = version;
        this.expireTime = expireTime;
    }

    public AsymmetryKeyWrapper(String publicKey, String privateKey, long version) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.version = version;
        this.expireTime = -1;
    }

    public KeyPair getKeyPair() {
        return new KeyPair(this.publicKey, this.privateKey);
    }

}
