import md5 from 'js-md5'
import JSEncrypt from '@/patch/jsencrypt'
import CryptoJS from 'crypto-js'
import {Base64} from 'js-base64'

var globalIV = ''
var globalSK = ''
var globalPK = ''
var keyVersion = '4294967297'

export function webEncrypt (config) {
    // debugger
    config.headers.set('X-Versky', keyVersion)
    var data
    if (config.headers['X-En-Info'] === 'yes') {
        // 需公共加密
         
        if (config.method === 'get') {
             
            data = config.params || {}
            config.params = doEncrypt(config, data)
        } else {
            data = config.data || {}
            config.data = doEncrypt(config, data)
             
        }
    }
    return config
}

export function webDecrypt (response) {
    if (response !== null & response.data !== null) {
        if (response.request.responseURL.includes('alive')) {
            // sessionStorage.setItem('sk', resp.headers['X-Rabbits'])
            // sessionStorage.setItem('pk', resp.headers['X-Prefer-To'])
            // sessionStorage.setItem('ik', resp.data.data)
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
        let sInfo = response.headers['x-en-info']
        if (sInfo === 'yes') {
            response.data = doDecrypt(response)
        }
    }
    return response
}

function doEncrypt (config, data) {
    // debugger
    let array = new Uint32Array(10)
    window.crypto.getRandomValues(array)
     
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
     
    let encryptStr = CryptoJS.format.OpenSSL.stringify(encrypted)
    config.headers.set('Content-Type', 'application/json;charset=utf-8')
    config.headers.set('X-Vector', Base64.encode(iv))
    data = {
        param: encodeURIComponent(encryptStr).replace(/%/g, '/')
    }
    return data
}

function doDecrypt(response) {
    let encodeData = response.data
    let sk = globalSK
    let pk = globalPK
    let ksign = response.headers['x-savour-certain']
    let iv = response.headers['x-carrots']
    let rs = response.headers['x-rancret']
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
     
    let basesk = encryptor.decryptByPublicKey(sk)
    let rawsk = Base64.decode(basesk)
    let rawData = CryptoJS.AES.decrypt(encodeData, CryptoJS.enc.Utf8.parse(rawsk), {
        iv: CryptoJS.enc.Utf8.parse(iv),
        padding: CryptoJS.pad.ZeroPadding
    })
    // sessionStorage.setItem('ik', iv)
    globalIV = iv
    return JSON.parse(rawData.toString(CryptoJS.enc.Utf8))
}