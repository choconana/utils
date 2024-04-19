import axios from 'axios'
import system from '@/system'
import {webEncrypt, webDecrypt} from '@/utils/WebCrypto'

// 全局参数，自定义参数可在发送请求时设置
axios.defaults.timeout = 1000 * system.ajaxTimeout // 超时时间ms
axios.defaults.withCredentials = true


// 请求时的拦截
// 回调里面不能获取错误信息
axios.interceptors.request.use(
    function (config) {
        config.headers.Authorization = 'Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4eGpmIiwiYXVkIjoiY29tbW9uIiwic2lkZSI6InVzZXIiLCJjaGFubmVsIjoid3giLCJpc3MiOiJ1bXMtaG4iLCJleHAiOjE3MDI4NzAxMzksInVzZXJJZCI6IkNEQjcxNDhBNTI2NjQwNzg5MjRBRUZCOTg0NTYxREU3IiwiaWF0IjoxNzAyMjY1MzM5fQ.T2rpbi-m3k8ULpjNFDY8nZ9yE_ozSqM1ZE3XNCgXDpY'
        config = webEncrypt(config)
        return config
    },
    function (error) {
        // 当请求异常时做一些处理
        console.warn('error')
        return Promise.reject(error)
        // return Promise.reject('请求异常，请稍后再试');
    }
)

axios.interceptors.response.use(async function (response) {
    // debugger
    response = webDecrypt(response)
    return response
}, function (error) {
    // .
    // Do something with response error
    // console.warn('error')
    return Promise.reject(error)
})

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