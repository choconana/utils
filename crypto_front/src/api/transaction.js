import base from '../base'

let axios = base.axios
let baseUrl = base.url

export default {
    query (params) {
        return axios({
            method: 'get',
            url: `${baseUrl}/tx/query`,
            params
        })
    },
}