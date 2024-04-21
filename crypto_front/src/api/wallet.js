import base from '../base'

let axios = base.axios
let baseUrl = base.url

export default {
    connect (params) {
        return axios({
            method: 'post',
            url: `${baseUrl}/wallet/connect`,
            data: params
        })
    },
    mint (params) {
        return axios({
            method: 'post',
            url: `${baseUrl}/erc20/mint`,
            data: params
        })
    },
    burn (params) {
        return axios({
            method: 'post',
            url: `${baseUrl}/erc20/burn`,
            data: params
        })
    },
    totalSupply (params) {
        return axios({
            method: 'post',
            url: `${baseUrl}/erc20/totalSupply`,
            data: params
        })
    },
}