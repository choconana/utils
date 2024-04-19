package com.chinaums.spread.common.crypto;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.asymmetric.AsymmetricAlgorithm;
import cn.hutool.crypto.asymmetric.RSA;
import com.chinaums.spread.common.crypto.annotation.WebDecrypt;
import com.chinaums.spread.common.crypto.annotation.WebEncrypt;
import com.chinaums.spread.common.crypto.enums.CryptoMode;
import com.chinaums.spread.common.crypto.enums.CryptoType;
import com.chinaums.spread.common.crypto.exception.CryptoException;
import com.chinaums.spread.common.crypto.key.*;
import com.chinaums.spread.common.crypto.key.KeyPair;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.lang.reflect.AnnotatedElement;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static cn.hutool.core.convert.Convert.hexToBytes;

@Component
public class WebCryptoSupport {

    private volatile KeyPair asymmetryKey;

    private volatile String symmetryKey;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private SymmetryCacheKey symmetryCacheKey;

    @Autowired
    private AsymmetryCacheKey asymmetryCacheKey;

    public static final long LATEST_VERSION = Long.MIN_VALUE;

    private static final String pairKeyDirectory = "F:\\project\\wxgroup\\ecology_stars\\module-henan\\web-esp-henan\\src\\main\\resources\\rsa\\";
    private static final String publicKeyPath = pairKeyDirectory + "PublicKey_api.pem";
    private static final String privateKeyPath = pairKeyDirectory + "PrivateKey_api.pem";
    private static final String publicKeyPrefix = "PUBLIC KEY";
    private static final String privateKeyPrefix = "PRIVATE KEY";
    public static final String SIGN_ALGORITHMS = "SHA256WithRSA";

    public static boolean hasCryptoAnnotation(AnnotatedElement annotatedElement, CryptoType cryptoType) {
        if (null == annotatedElement) {
            return false;
        }
        switch (cryptoType) {
            case ENCRYPT:
                return annotatedElement.isAnnotationPresent(WebEncrypt.class);
            case DECRYPT:
                return annotatedElement.isAnnotationPresent(WebDecrypt.class);
            default:
                return false;
        }
    }

    public static void checkKey(CryptoMode cryptoMode, CryptoMode type) {
        if (!cryptoMode.equals(type)) {
            throw new CryptoException("key type config not consistent");
        }
    }

    public static RSA generateRSA(byte[] rsaKey, CryptoMode.RSAKeyType type) {
        RSA rsa;
        switch (type) {
            case PUB:
                rsa = new RSA(null, rsaKey);
                break;
            case PRV:
                rsa = new RSA(rsaKey, null);
                break;
            default:
                throw new CryptoException("rsa key type not found");
        }
        return rsa;
    }

    public static void generateRSAFile() throws IOException {
        // 生成密钥
        java.security.KeyPair keyPair = KeyUtil.generateKeyPair(AsymmetricAlgorithm.RSA.getValue(), 2048);
        // 文件夹不存在，则先创建
        Files.createDirectories(Paths.get(pairKeyDirectory));

        try(FileWriter priFileWriter = new FileWriter(privateKeyPath);
            PemWriter priPemWriter = new PemWriter(priFileWriter);
            FileWriter pubFileWriter = new FileWriter(publicKeyPath);
            PemWriter pubPemWriter = new PemWriter(pubFileWriter)) {
            priPemWriter.writeObject(new PemObject(privateKeyPrefix, keyPair.getPrivate().getEncoded()));
            pubPemWriter.writeObject(new PemObject(publicKeyPrefix, keyPair.getPublic().getEncoded()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*public KeyPair getRSAFromFile(String pubKeyPath, String prvKeyPath) throws IOException {
        if (null == rsa) {
            byte[] pubKeyBytes = this.readKey(pubKeyPath);
            byte[] prvKeyBytes = this.readKey(prvKeyPath);
            if (null == pubKeyBytes || null == prvKeyBytes) {
                throw new CryptoException("未读取到公钥或私钥");
            }
            PublicKey publicKey = KeyUtil.generatePublicKey(AsymmetricAlgorithm.RSA.getValue(), pubKeyBytes);
            PrivateKey privateKey = KeyUtil.generatePrivateKey(AsymmetricAlgorithm.RSA.getValue(), prvKeyBytes);
            rsa = new KeyPair(privateKey, publicKey);
        }
        return rsa;
    }*/

    public SymmetryKeyWrapper getSymmetryKeyFromCache(long curVersion, String locator) {
        return symmetryCacheKey.getKey(curVersion, locator);
    }

    public SymmetryKeyWrapper getLatestSymmetryKeyFromCache() {
        return symmetryCacheKey.getLatestKey();
    }

    public AsymmetryKeyWrapper getAsymmetryKeyFromCache(long curVersion, String locator) {
        return asymmetryCacheKey.getKey(curVersion, locator);
    }

    public AsymmetryKeyWrapper getLatestAsymmetryKeyFromCache() {
        return asymmetryCacheKey.getLatestKey();
    }

    protected byte[] readKey(String keyPath) throws IOException {
        if (StringUtils.isBlank(keyPath)) {
            return null;
        }
        Resource resource = resourceLoader.getResource(keyPath);
        InputStream inputStream = resource.getInputStream();
        byte[] keyBytes = new byte[inputStream.available()];
        inputStream.read(keyBytes);
        inputStream.close();
        return keyBytes;
    }

    public static KeyPair genKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        // 初始化密钥对生成器，密钥大小为96-1024位
        keyPairGen.initialize(2048, new SecureRandom());
        // 生成一个密钥对，保存在keyPair中
        java.security.KeyPair keyPair = keyPairGen.generateKeyPair();
        // 得到私钥
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        // 得到公钥
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        // 得到公钥字符串
        String publicKeyString = Base64.encode(publicKey.getEncoded());
        // 得到私钥字符串
        String privateKeyString = Base64.encode((privateKey.getEncoded()));
        return new KeyPair(publicKeyString, privateKeyString);
    }

    public static String encryptByPublicKey(String str, String publicKey) throws Exception {

        // base64编码的公钥
        byte[] decoded = Base64.decode(publicKey);
        RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
        // RSA加密
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        return Base64.encode(cipher.doFinal(str.getBytes(StandardCharsets.UTF_8)));
    }
    public static String decryptByPublicKey(String str, String publicKey) throws GeneralSecurityException {
        // 64位解码加密后的字符串
        byte[] inputByte = Base64.decode(str.getBytes(StandardCharsets.UTF_8));
        // base64编码的公钥
        byte[] decoded = Base64.decode(publicKey);
        RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
        // RSA加密
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, pubKey);
        return new String(cipher.doFinal(inputByte));
    }

    public static String encryptByPrivateKey(String str, String privateKey) throws GeneralSecurityException {
        // base64编码的私钥
        byte[] decoded = Base64.decode(privateKey);
        RSAPrivateKey priKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
        // RSA加密
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, priKey);
        return Base64.encode(cipher.doFinal(str.getBytes(StandardCharsets.UTF_8)));
    }
    public static String decryptByPrivateKey(String str, String privateKey) throws GeneralSecurityException {
        // 64位解码加密后的字符串
        byte[] inputByte = Base64.decode(str.getBytes(StandardCharsets.UTF_8));
        // base64编码的私钥
        byte[] decoded = Base64.decode(privateKey);
        RSAPrivateKey priKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
        // RSA解密
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, priKey);
        return new String(cipher.doFinal(inputByte));
    }

    public static String encryptAES(String raw, String key, String iv) throws GeneralSecurityException {

            Cipher cipher = Cipher.getInstance("AES/CBC/NOPadding");   //参数分别代表 算法名称/加密模式/数据填充方式
            int blockSize = cipher.getBlockSize();

            byte[] dataBytes = raw.getBytes();
            int plaintextLength = dataBytes.length;
            if (plaintextLength % blockSize != 0) {
                plaintextLength = plaintextLength + (blockSize - (plaintextLength % blockSize));
            }

            byte[] plaintext = new byte[plaintextLength];
            System.arraycopy(dataBytes, 0, plaintext, 0, dataBytes.length);

            SecretKeySpec keyspec = new SecretKeySpec(key.getBytes(), "AES");
            IvParameterSpec ivspec = new IvParameterSpec(iv.getBytes());

            cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec);
            byte[] encrypted = cipher.doFinal(plaintext);

            return Base64.encode(encrypted);
    }

    public static String decryptAES(String encode, String key, String iv) throws GeneralSecurityException, IOException {
            byte[] encrypted1 = Base64.decode(encode);

            Cipher cipher = Cipher.getInstance("AES/CBC/NOPadding");
            SecretKeySpec keyspec = new SecretKeySpec(key.getBytes(), "AES");
            IvParameterSpec ivspec = new IvParameterSpec(iv.getBytes());

            cipher.init(Cipher.DECRYPT_MODE, keyspec, ivspec);

            byte[] original = cipher.doFinal(encrypted1);
            String originalString = new String(original);
            return originalString;
    }

    public static String generate16ByteRandom() {
        return generateNByteRandom(16);
    }

    public static String generateNByteRandom(int n) {
        //产生n位的强随机数
        StringBuilder uid = new StringBuilder();
        Random rd = new SecureRandom();
        for (int i = 0; i < n; i++) {
            //产生0-2的3位随机数
            int type = rd.nextInt(3);
            switch (type){
                case 0:
                    //0-9的随机数
                    uid.append(rd.nextInt(10));
                    break;
                case 1:
                    //ASCII在65-90之间为大写,获取大写随机
                    uid.append((char)(rd.nextInt(25)+65));
                    break;
                case 2:
                    //ASCII在97-122之间为小写，获取小写随机
                    uid.append((char)(rd.nextInt(25)+97));
                    break;
                default:
                    break;
            }
        }
        return uid.toString();
    }

    /**
     * 重放攻击检测
     * @param key
     * @return
     */
    public boolean replayAttacksDetect(String key, String url, long expire) {
        String value = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.isBlank(value)) {
            stringRedisTemplate.opsForValue().set(key, url, expire, TimeUnit.MILLISECONDS);
            return false;
        }
        return true;
    }

    /**
     * 密钥版本号由非对称密钥版本号和对称密钥版本号组成
     * 密钥版本号数据类型为64位长整型，二进制表示为无符号大端序，高32位为非对称密钥版本，低32位为对称密钥版本
     * @param version 密钥版本号
     * @param cryptoMode 密钥类型
     * @return version 非对称密钥版本号或者对称密钥版本号
     */
    public static long resolveKeyVersion(long version, CryptoMode cryptoMode) {
        switch (cryptoMode) {
            case DES:
            case AES:
                return version & 0xffffffffL;
            case RSA:
                return version >>> 32;
            default:
                return 0;
        }
    }

    /**
     *  如果无密钥类型，则返回非对称密钥和对称密钥的合计版本号
     * @param cryptoMode 密钥类型
     * @return version 指定密钥类型的版本号
     */
    public long getCurKeyVersion(CryptoMode cryptoMode) {
        switch (cryptoMode) {
            case DES:
            case AES:
                return symmetryCacheKey.getVersion();
            case RSA:
                return asymmetryCacheKey.getVersion();
            case NULL:
                return asymmetryCacheKey.getVersion() << 32 | symmetryCacheKey.getVersion();
            default:
                return -1;
        }
    }

    public static long updateKeyVersion(long oldVersion, long newVersion, CryptoMode cryptoMode) {
        switch (cryptoMode) {
            case DES:
            case AES:
                return oldVersion & 0xffffffff00000000L + newVersion;
            case RSA:
                return oldVersion & 0xffffffffL + newVersion << 32;
            default:
                return 0;
        }
    }

    public static String signWithRSA(String value, String privateKey) throws Exception {

        byte[] keyBytes = Base64Utils.decodeFromString(privateKey);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);

        KeyFactory keyf = KeyFactory.getInstance("RSA");

        PrivateKey priKey = keyf.generatePrivate(keySpec);

        Signature signature = Signature.getInstance(SIGN_ALGORITHMS);

        signature.initSign(priKey);

        signature.update(value.getBytes(StandardCharsets.UTF_8));

        byte[] signed = signature.sign();

        return Base64Utils.encodeToString(signed);

    }

    public static boolean verifyWithRSA(Map<String, String> param, String publicKey, String sign) throws Exception {

        String value = sortMap(param);
        byte[] keyBytes = Base64.decode(publicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyf = KeyFactory.getInstance("RSA");
        PublicKey pubkey = keyf.generatePublic(keySpec);
        Signature signature = Signature.getInstance(SIGN_ALGORITHMS);
        signature.initVerify(pubkey);
        signature.update(value.getBytes());

        return signature.verify(Base64.decode(sign.getBytes()));
    }

    public static boolean verifyWithRSA(String raw, String publicKey, String sign) throws Exception {

        byte[] keyBytes = Base64.decode(publicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyf = KeyFactory.getInstance("RSA");
        PublicKey pubkey = keyf.generatePublic(keySpec);
        Signature signature = Signature.getInstance(SIGN_ALGORITHMS);
        signature.initVerify(pubkey);
        signature.update(raw.getBytes());

        return signature.verify(Base64.decode(sign.getBytes()));
    }

    public static String sortMap(Map<String, String> param){
        StringBuilder result = new StringBuilder();
        Collection<String> keySet = param.keySet();
        List<String> list = new ArrayList<>(keySet);
        Collections.sort(list);

        for (int i = 0; i < list.size(); ++i) {
            String key = list.get(i);
            if(param.get(key) == null || "".equals(param.get(key).trim())){
                continue;
            }
            result.append(key).append("=").append(param.get(key)).append("&");
        }

        return result.substring(0, result.length() - 1);
    }

    public static String sha256(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return bytesToHex(md.digest(data));
    }

    public static String bytesToHex(byte[] bytes) {
        String hexArray = "0123456789abcdef";
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            int bi = b & 0xff;
            sb.append(hexArray.charAt(bi >> 4));
            sb.append(hexArray.charAt(bi & 0xf));
        }
        return sb.toString();
    }

    public static String encrypt3DESString(String value, String key) throws Exception {
        if (null == value || "".equals(value)) {
            return "";
        }
        byte[] valueByte = value.getBytes();
        byte[] sl = encrypt3DES(valueByte, key.getBytes());
        return Base64.encode(sl);
    }

    private static byte[] encrypt3DES(byte[] input, byte[] key) throws Exception {
        Cipher c = Cipher.getInstance("DESede/ECB/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "DESede"));
        return c.doFinal(input);
    }

    public static String decrypt3DESString(String value, String key) throws Exception {

        if (null == value || "".equals(value)) {
            return "";
        }

        byte[] valueByte = Base64.decode(value);
        byte[] sl = decrypt3DES(valueByte, key.getBytes());
        return new String(sl);
    }

    public static byte[] decrypt3DES(byte[] input, byte[] key) throws Exception {
        Cipher c = Cipher.getInstance("DESede/ECB/PKCS5Padding");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "DESede"));
        return c.doFinal(input);
    }


    public static void main(String[] args) throws Exception {
        String raw = "appId=f472cffdf78b44b2ba477334854794bb&nonceStr=abc&secret=fdebddd0697e4aeba93d49bf49cb6382&timestamp=1700559072";
        System.out.println(sha256(raw.getBytes()));
    }

}
