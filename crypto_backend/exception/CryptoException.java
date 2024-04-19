package com.chinaums.spread.common.crypto.exception;

import com.chinaums.spread.dataaccess.entity.exception.RRException;

public class CryptoException extends RRException {
    public CryptoException(String msg) {
        super(msg);
    }

    public CryptoException(String msg, Throwable e) {
        super(msg, e);
    }
}
