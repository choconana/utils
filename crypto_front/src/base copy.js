import axios from 'axios'
import system from '@/system'
import md5 from 'js-md5'
import JSEncrypt from '@/patch/jsencrypt'
import CryptoJS from 'crypto-js'
import {Base64} from 'js-base64'
import CryptoApi from '@/api/crypto'

// 全局参数，自定义参数可在发送请求时设置
axios.defaults.timeout = 1000 * system.ajaxTimeout // 超时时间ms
axios.defaults.withCredentials = true

var globalIV = ''
var globalSK = ''
var globalPK = ''
var keyVersion = '4294967297'

// 请求时的拦截
// 回调里面不能获取错误信息
axios.interceptors.request.use(
    function (config) {
        // 发送请求之前做一些处理
        config.headers.set('X-Versky', keyVersion)
        var data
        if (config.headers['X-En-Info'] === 'yes') {
            // 需公共加密
            // .
            if (config.method === 'get') {
                // .
                data = config.params || {}
                config.params = encrypt(config, data)
            } else {
                data = config.data || {}
                config.data = encrypt(config, data)
                // .
            }
        }
        return config
    },
    function (error) {
        // 当请求异常时做一些处理
        console.warn('error')
        return Promise.reject(error)
        // return Promise.reject('请求异常，请稍后再试');
    }
)

function encrypt (config, data) {
    let array = new Uint32Array(10)
    window.crypto.getRandomValues(array)
    // .
    let index = Math.floor(Math.random() * 9)
    // let sk = sessionStorage.getItem('sk')
    // let iv = sessionStorage.getItem('ik')
    // let pk = sessionStorage.getItem('pk')
    let sk = globalSK
    let iv = globalIV
    let pk = globalPK
    if (!sk || !iv || !pk) {
        return
    }
    let options = {
        default_key_size: 2048
    }
    let encryptor = new JSEncrypt(options)
    let decodePk = decodeURIComponent(pk.replace(/\//g, '%'))
    encryptor.setPublicKey(decodePk)
    let basesk = encryptor.decryptByPublicKey(sk)
    let rawsk = Base64.decode(basesk)
    let rawJson = JSON.stringify(data)
    let timestamp = new Date().getTime() + '.' + array[index]
    let timestampEncrypt = encryptor.encrypt(timestamp)
    
    let params = {
        raw: rawJson,
        sign: md5(rawJson + ';' + iv + ';' + timestamp),
        whisper: timestampEncrypt
    }
    var encrypted = CryptoJS.AES.encrypt(JSON.stringify(params), CryptoJS.enc.Utf8.parse(rawsk), {
        iv: CryptoJS.enc.Utf8.parse(iv),
        mode: CryptoJS.mode.CBC,
        padding: CryptoJS.pad.ZeroPadding
    });
    // .
    let encryptStr = CryptoJS.format.OpenSSL.stringify(encrypted)
    config.headers.set('Content-Type', 'application/json;charset=utf-8')
    config.headers.set('vector', Base64.encode(iv))
    data = {
        param: encodeURIComponent(encryptStr).replace(/%/g, '/')
    }
    return data
}   

axios.interceptors.response.use(async function (response) {
    if (response !== null & response.data !== null) {
        if (response.request.responseURL.includes('alive')) {
            // sessionStorage.setItem('sk', resp.headers['X-Rabbits'])
            // sessionStorage.setItem('pk', resp.headers['X-Prefer-To'])
            // sessionStorage.setItem('ik', resp.data.data)
            // .
            let sk = response.headers['x-rabbits']
            let pk = response.headers['x-prefer-to']
            let iv = response.data.data
            let version = response.headers['x-versky']
            if ((sk != null && sk !== '') && (pk != null && sk !== '') && (version != null && version !== '')) {
                globalSK = sk
                globalPK = pk
                keyVersion = version
            }
            if (iv) {
                globalIV = iv
            }
        }
        let sInfo = response.headers['X-En-Info']
        if (sInfo === 'yes') {
            response.data = decrypt(response)
        }
    }
    return response
}, function (error) {
    // .
    // Do something with response error
    // console.warn('error')
    return Promise.reject(error)
})

function decrypt(response) {
    let encodeData = response.data
    let sk = globalSK
    let pk = globalPK
    let ksign = response.headers['X-Savour-Certain']
    let iv = response.headers['X-Carrots']
    let rs = response.headers['X-Rancret']
    const salt = 'c&8Y^Kn5_~'
    let decodePk = decodeURIComponent(pk.replace(/\//g, '%'))
    if (md5(decodePk+salt+rs) !== ksign) {
        console.error('响应数据异常')
        return Promise.reject('响应数据异常')
    }
    let options = {
        default_key_size: 2048
    }
    let encryptor = new JSEncrypt(options)
    encryptor.setPublicKey(decodePk)
    // .
    let basesk = encryptor.decryptByPublicKey(sk)
    let rawsk = Base64.decode(basesk)
    let rawData = CryptoJS.AES.decrypt(encodeData, CryptoJS.enc.Utf8.parse(rawsk), {
        iv: CryptoJS.enc.Utf8.parse(iv),
        padding: CryptoJS.pad.ZeroPadding
    })
    // sessionStorage.setItem('ik', iv)
    globalIV = iv
    sessionStorage.setItem('tick', new Date().getTime())
    return JSON.parse(rawData.toString(CryptoJS.enc.Utf8))
}

const getServerDomain = () => {
    return system.devServerDomain
}
  
const getServerUrl = () => {
    return getServerDomain() + system.baseUrl
}
  
const base = {
    url: getServerUrl(),
    domain: getServerDomain(),
    axios,
    baseUrl: system.baseUrl
}
export default base