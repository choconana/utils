package com.chinaums.spread.common.crypto.constants;

public class HttpHeaderConstant {

    // 加密的对称密钥
    public static final String ENCODE_SYMMETRY_KEY = "X-Rabbits";

    // url编码后的公钥
    public static final String URLENCODE_PUBLIC_KEY = "X-Prefer-To";

    // 公钥签名
    public static final String PUBLIC_KEY_SIGNATURE = "X-Savour-Certain";

    // 对称加密所需的iv
    public static final String ENCRYPT_SYMMETRY_IV = "X-Carrots";

    // 哈希摘要盐值的随机部分
    public static final String RANDOM_SALT = "X-Rancret";

    // 前端传的解密iv
    public static final String DECRYPT_SYMMETRY_IV = "X-Vector";


    /**
     * 密钥版本号由非对称密钥版本号和对称密钥版本号组成
     * 密钥版本号数据类型为64位长整型，无符号二进制，高32位为非对称密钥版本，低32位为对称密钥版本
     */
    public static final String KEY_VERSION = "X-Versky";

    // api加密信息
    public static final String API_ENCRYPT_INFO_NAME = "X-En-Info";
    // api加密
    public static final String API_ENCRYPT_INFO_VALUE_YES = "yes";
    // api不加密
    public static final String API_ENCRYPT_INFO_VALUE_NO = "no";
    // api接口客户端
    public static final String API_CLIENT_SIDE = "X-Side";
}
