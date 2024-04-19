package com.chinaums.spread.common.crypto.utils;

import lombok.Getter;
import org.apache.commons.codec.binary.Base64;
import javax.crypto.Cipher;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class CryptoUtil {
    private static final String pubkey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0zpXpC4fO0/5N6g3ZbOHZ2iuyzw3aEp9EfRLPppHtnsAQJkfcnmZV6QGY0x34P/ReBgrUygSGyn0fdw5hvMClOWpr79nbUi+UUdXUi6vYW1D9RyrvSrZW8MUYFXyDd3qGk/lBUlNhAlQz4CNxg1nP4auAEZHPzSr6xZLcdJs7i5H2Rm5MTyvToDrTuKW1dzilHTsmduPNtHZ7/HDCMaDeGO1uHRvg2RO2rIXvKMLRRG1YQS0zUI1a6XiuvHryAOI8gYVJXBhF8RNE6iIJ46of0Qf7o7OvSAh3YPsgdWmUwUA5VPDPFvA91L+Jqj7hEmlbtt1ioVu4SQwdDY4gHXw7wIDAQAB";
    private static final String prvkey = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDTOlekLh87T/k3qDdls4dnaK7LPDdoSn0R9Es+mke2ewBAmR9yeZlXpAZjTHfg/9F4GCtTKBIbKfR93DmG8wKU5amvv2dtSL5RR1dSLq9hbUP1HKu9KtlbwxRgVfIN3eoaT+UFSU2ECVDPgI3GDWc/hq4ARkc/NKvrFktx0mzuLkfZGbkxPK9OgOtO4pbV3OKUdOyZ24820dnv8cMIxoN4Y7W4dG+DZE7ashe8owtFEbVhBLTNQjVrpeK68evIA4jyBhUlcGEXxE0TqIgnjqh/RB/ujs69ICHdg+yB1aZTBQDlU8M8W8D3Uv4mqPuESaVu23WKhW7hJDB0NjiAdfDvAgMBAAECggEALsAF77+JiA4eQZRIlojSmi+QKEkuljWOUu/3y2ZIIvo8Rw/c5vBE4SHbjBS7n5/fE659wEzpHPv/MfARB243jw75rH03ffeQr9qTWAwfIvgVB2YOGLMoImgd1WiQR0NrceFm9fGQZ33MVfDE8CW9SVEcp9DmKfYVatWnYwzIsTpDrws5Lo9yKc6jW9KJmB6BMihMmZ+/dmg1PAngwU45TWeAfBiaNlv2XpwweWq+mxs9PHkKADg024a4EEtilKuQhJVed3LojbUCyRnsvZHeybvr2F2bVRh4Z7qbjw+CsCNZnW8x3EUqeVxJtNYjoZYJajzl3HG9idfG0Zl8LXRi2QKBgQD+l3EbYIUVydwVB4kCelGMllmKdraw03n8DpW1F3cCdLahxRLgkXzhBBe57MhPz1KLMKiaVtVpsqoYY27+wH/0Wh/PEIWqiawwNTYrGEKP1uIx3+z03HrbwEYddjEA32F8Wfrc8Srzvg7X4G0iAeKz1YLSFGO2F16iugpmTg+jGwKBgQDUZXzp0Or7Jf7PI+4W1+XjR43A8xRUebFamiA6SqIJuxI6D+sKEM3y1OqdiDaB0hbo9oTUP/3VqeswmbSKKH9ga4gdcGcxK4gfcZefn5Xc+bt4nXVT6JdXBL6JP8uDlt2a8d/JTkLX8e7MHuEJbnbqiNa9T7L02vlXjjg29OHyvQKBgH5SmlahL6e9/yuYD41hL/F38HnZqZBXfiFpAzNVr0FStAPUiydOSQ5FP5iLOmEPV7+kpyPdjgriEbAENmDFFzghN8NASXJy2TMaVARSB0TjtFxW5XYhp/w9jQy1Rl9Od0qCQw05xLwoQ6ktvDixgtEEUeL3JvqE4foQIXTdqridAoGAY2QKjC/jhhtFnhEmPTJStYSqZPxbKxy3TbqKEj0SjpMn+FuylUc/2L6h+43eU+nIJsQVbG11jyKwmFGVFoHU+X3YeE49O9kvHee+GEhJjNqgS9UDLnzNNT9XGkrsJWvXz8YX/s/Mn9jq8kIQ9KToqN/X/Ubqa4J84+f9jloR0dECgYBnOlgXsW9jOsTR+SSVLA1dQDsyLXMbrYiLHX2CQW7VgM9VEyhInFF76sEEg75osQAR55qFpMKWXqTo19/LKKi15LP02hQhjO4JY2y4+KcPglBszJOW+obx7c7p3uhPtImeD9MN6ZD9/TPg4QzJZFE0lFoR+utRXJmamOTFm8PvOQ==";

    public static void genKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        // 初始化密钥对生成器，密钥大小为96-1024位
        keyPairGen.initialize(1024, new SecureRandom());
        // 生成一个密钥对，保存在keyPair中
        KeyPair keyPair = keyPairGen.generateKeyPair();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        // 得到私钥
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        // 得到公钥
        String publicKeyString = new String(Base64.encodeBase64(publicKey.getEncoded()));
        // 得到私钥字符串
        String privateKeyString = new String(Base64.encodeBase64((privateKey.getEncoded())));

    }

    public static String encryptByPublicKey(String str, String publicKey) throws Exception {

        // base64编码的公钥
        byte[] decoded = Base64.decodeBase64(publicKey);
        RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
        // RSA加密
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        return Base64.encodeBase64String(cipher.doFinal(str.getBytes("UTF-8")));
    }
    public static String decryptByPublicKey(String str, String publicKey) throws Exception {
        // 64位解码加密后的字符串
        byte[] inputByte = Base64.decodeBase64(str.getBytes("UTF-8"));
        // base64编码的公钥
        byte[] decoded = Base64.decodeBase64(publicKey);
        RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
        // RSA加密
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, pubKey);
        return new String(cipher.doFinal(inputByte));
    }

    public static String encryptByPrivateKey(String str, String privateKey) throws Exception {
        // base64编码的私钥
        byte[] decoded = Base64.decodeBase64(privateKey);
        RSAPrivateKey priKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
        // RSA加密
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, priKey);
        return Base64.encodeBase64String(cipher.doFinal(str.getBytes("UTF-8")));
    }
    public static String decryptByPrivateKey(String str, String privateKey) throws Exception {
        // 64位解码加密后的字符串
        byte[] inputByte = Base64.decodeBase64(str.getBytes("UTF-8"));
        // base64编码的私钥
        byte[] decoded = Base64.decodeBase64(privateKey);
        RSAPrivateKey priKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
        // RSA解密
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, priKey);
        return new String(cipher.doFinal(inputByte));
    }

    public static void main(String[] args) throws Exception {
//        String str = "c9eh/Srqa8VXxxfTs7qdWg/nTDbNnbBA4snXxP3aQazvqM8mdHQ6qsZBhv2kRPUAMm5s07EEY3CMQ0S9AcuiHEmZIsNGde6RV2WPlyeRwFa4zl35NGYze9P+Yy7h467JT6Ep0DSaSOKTZo9blL+qsfBBrIOnSx65pJ3wrGyUZEhOoZ2VAXgaMkkM0fJFbPnvd3CvtHUdkEBCgxPQg7p491eKlaaXWXO8PimQhX90mrtZobc2f7o5jFcFRdD1EBahTR23uMAjHeAh6gybuAstt1pUTgf10dirOUqsLdd++ifIKm5G93CMdP9fCWqq0hENGxyWCHzOUaq8KkGEjvk2oQ==";
//        System.out.println(decryptByPrivateKey(str, prvkey));
        String raw = "abcde";
//        String encrypt = encryptByPrivateKey(raw, prvkey);
        System.out.println(encryptByPublicKey(raw, pubkey));
    }

}
