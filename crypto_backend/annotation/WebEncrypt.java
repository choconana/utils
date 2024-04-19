package com.chinaums.spread.common.crypto.annotation;

import com.chinaums.spread.common.crypto.enums.CryptoStrategy;

import java.lang.annotation.*;

/**
 * 该注解可以用在类型和方法上
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(value = {
        ElementType.METHOD,
        ElementType.TYPE})
public @interface WebEncrypt {

    CryptoStrategy strategy();
}
