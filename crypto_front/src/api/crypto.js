import base from '../base'

let axios = base.axios
let baseUrl = base.url

export default {
    cryptoTest (params) {
        return axios({
            headers: {'X-En-Info': 'yes'},
            method: 'get',
            url: `${baseUrl}/xxjf/order/cryptoTest`,
            params
        })
    },

    paying (params) {
        return axios({
            headers: {'X-En-Info': 'yes'},
            method: 'post',
            url: `${baseUrl}/xxjf/payment/paying`,
            data: params
        })
    },
    pay (params) {
        return axios({
            headers: {'X-En-Info': 'yes'},
            method: 'get',
            url: `${baseUrl}/miniapp//pay`,
            params
        })
    },
    getNotPaidOrderList (params) {
        return axios({
            headers: {'X-En-Info': 'yes'},
            method: 'get',
            url: `${baseUrl}/xxjf/order/getNotPaidOrderList`,
            params
        })
    },

    setSession (params) {
        return axios({
            headers: {'X-En-Info': 'yes','X-Side': 'wx'},
            method: 'get',
            url: `${baseUrl}/wxlogin/test`,
            params
        })
    },
    logout () {
        return axios({
            headers: {'X-En-Info': 'yes'},
            method: 'post',
            url: `${baseUrl}/wxlogin/logout`
        })
    },
    alive () {
        return axios({
            method: 'get',
            url: `${baseUrl}/web/alive?_t=` + new Date().getTime()
        })
    },
    checkLogin (params) {
        return axios({
            headers: {'X-Client-Channel': 'wx', 'X-Proj-Code': 'xxjf'},
            method: 'get',
            url: `${baseUrl}/web/checkLogin`,
            params
        })
    },
    validateCodeByGT () {
        return axios({
            method: 'get',
            url: `${baseUrl}/web/validateCodeGT`
        })
    },
    sendGTCode (params) {
        return axios({
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            method: 'post',
            url: `${baseUrl}/web/sendCodeByGeetest`,
            data: params
        })
    }
    
}
