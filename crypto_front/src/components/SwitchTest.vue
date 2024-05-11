<template>
    <h3>切换页面测试</h3>
    <div class="switchTest">
        <el-form
            :model="order"
            status-icon
            label-width="120px"
            class="crypto"
        >
            <el-form-item>
                <el-button type="primary" @click="alive">alive</el-button>
            </el-form-item>
            <el-form-item label="orderNo">
                <el-input v-model="order.orderNo" />
            </el-form-item>
            <el-form-item label="feePhone">
                <el-input v-model="order.feePhone" />
            </el-form-item>
            <el-form-item>
                <el-button type="primary" @click="setSession">setSession</el-button>
                <el-button type="primary" @click="submit(order)">Submit</el-button>
            </el-form-item>
            <!-- <el-form-item>
                <el-button @click="crypto(order.feePhone)">crypto</el-button>
            </el-form-item> -->
            <el-form-item label="payId">
                <el-input v-model="payOrder.payId" />
            </el-form-item>
            <el-form-item label="mode">
                <el-input v-model="payOrder.mode" />
            </el-form-item>
            <el-form-item label="status">
                <el-input v-model="payOrder.status" />
            </el-form-item>
            <el-form-item>
                <el-button type="primary" @click="paying(payOrder)">paying</el-button>
            </el-form-item>
            <el-form-item label="feePhone">
                <el-input v-model="order.feePhone" />
            </el-form-item>
            <el-form-item label="feeName">
                <el-input v-model="order.feeName" />
            </el-form-item>
            <el-form-item label="signature">
                <el-input v-model="order.signature" />
            </el-form-item>
            <el-form-item>
                <el-button type="primary" @click="getNotPaidOrderList(order)">get</el-button>
            </el-form-item>
        </el-form>
        <el-form-item>
            <el-button type="primary" @click="goBack()">返回</el-button>
        </el-form-item>
    </div>
</template>

<script>
import CryptoApi from '@/api/crypto'
import JSEncrypt from '@/patch/jsencrypt'

export default {
    data () {
        return {
            order: {
                id: null,
                orderNo: null,
                batchNo: null,
                feeName: null,
                feePhone: '19123456789',
                feeType: null,
                payerPhone: null,
                payMode: null,
                importerName: null,
                payStatus: null,
                writeoffStatus: null,
                refundFlag: null,
                payStatusName: null,
                writeoffStatusName: null,
                refundFlagName: null,
                createTime: null,
                payTime: null,
                writeoffTime: null,
                remark: null,
                signature: 'bb61e652c663d57f22269e41dd53da5e'
            },
            payOrder: {
                payId: 'E87425CF2B5D40F091BB5335ACB91E2E',
                mode: '2',
                status: 'success'
            }
        }
    },
    created () {
        // CryptoApi.alive()
    },
    mounted () {
        
    },
    methods: {
        setSession () {
            let params = {
                'phone': '19123456789',
                'openid': 'oA0i35dw29W8khNryTBXxUfeCBZk',
                'sessionkey':'XXjUkS2zdLOP1p1Jl6Unvg=='
            }
            CryptoApi.setSession(params).then(resp => {
                // .
                if (resp && resp.data['code'] === 200) {
                    alert("success!")
                } else {
                    console.log(resp.msg)
                    this.$message({
                        message: resp.msg,
                        type: 'error'
                    })
                }
            }).catch(error => {
                // .
                console.log(error)
                this.$message({
                    message: error,
                    type: 'error'
                })
            })
        },
        submit (data) {
            CryptoApi.cryptoTest(data).then(resp => {
                // .
                if (resp && resp.data['code'] == '200') {
                    console.log(resp.data.data)
                    alert(resp.data.code)
                } else {
                    console.log(resp.data.msg)
                    this.$message({
                        message: resp.data.msg,
                        type: 'error'
                    })
                }
            }).catch(error => {
                console.log(error)
                this.$message({
                    message: error,
                    type: 'error'
                })
            })
        },
        paying (data) {
            // .
            CryptoApi.paying(data).then(resp => {
                // .
                if (resp && resp.data['code'] === 200) {
                    console.log(resp.data.data)
                    alert(resp.data)
                } else {
                    console.log(resp.data.msg)
                    this.$message({
                        message: resp.data.msg,
                        type: 'error'
                    })
                }
            }).catch(error => {
                console.log(error)
                this.$message({
                    message: error,
                    type: 'error'
                })
            })
        },
        getNotPaidOrderList (data) {
            let params = {
                'page': '1',
                'limit': '5',
                'feePhone': data.feePhone,
                'feeName': data.feeName,
                'signature': data.signature
            }
            CryptoApi.getNotPaidOrderList(params).then(resp => {
                // .
                if (resp && resp.data['code'] === 200) {
                    console.log(resp.data.data)
                    alert(resp.data.code)
                } else {
                    console.log(resp.data.msg)
                    this.$message({
                        message: resp.data.msg,
                        type: 'error'
                    })
                }
            }).catch(error => {
                console.log(error)
                this.$message({
                    message: error,
                    type: 'error'
                })
            })
        },
        crypto (data) {
            let options = {
                default_key_size: 1024
            }
            // .
            var pubkey = 'MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0zpXpC4fO0/5N6g3ZbOHZ2iuyzw3aEp9EfRLPppHtnsAQJkfcnmZV6QGY0x34P/ReBgrUygSGyn0fdw5hvMClOWpr79nbUi+UUdXUi6vYW1D9RyrvSrZW8MUYFXyDd3qGk/lBUlNhAlQz4CNxg1nP4auAEZHPzSr6xZLcdJs7i5H2Rm5MTyvToDrTuKW1dzilHTsmduPNtHZ7/HDCMaDeGO1uHRvg2RO2rIXvKMLRRG1YQS0zUI1a6XiuvHryAOI8gYVJXBhF8RNE6iIJ46of0Qf7o7OvSAh3YPsgdWmUwUA5VPDPFvA91L+Jqj7hEmlbtt1ioVu4SQwdDY4gHXw7wIDAQAB'
            var prvkey = 'MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDTOlekLh87T/k3qDdls4dnaK7LPDdoSn0R9Es+mke2ewBAmR9yeZlXpAZjTHfg/9F4GCtTKBIbKfR93DmG8wKU5amvv2dtSL5RR1dSLq9hbUP1HKu9KtlbwxRgVfIN3eoaT+UFSU2ECVDPgI3GDWc/hq4ARkc/NKvrFktx0mzuLkfZGbkxPK9OgOtO4pbV3OKUdOyZ24820dnv8cMIxoN4Y7W4dG+DZE7ashe8owtFEbVhBLTNQjVrpeK68evIA4jyBhUlcGEXxE0TqIgnjqh/RB/ujs69ICHdg+yB1aZTBQDlU8M8W8D3Uv4mqPuESaVu23WKhW7hJDB0NjiAdfDvAgMBAAECggEALsAF77+JiA4eQZRIlojSmi+QKEkuljWOUu/3y2ZIIvo8Rw/c5vBE4SHbjBS7n5/fE659wEzpHPv/MfARB243jw75rH03ffeQr9qTWAwfIvgVB2YOGLMoImgd1WiQR0NrceFm9fGQZ33MVfDE8CW9SVEcp9DmKfYVatWnYwzIsTpDrws5Lo9yKc6jW9KJmB6BMihMmZ+/dmg1PAngwU45TWeAfBiaNlv2XpwweWq+mxs9PHkKADg024a4EEtilKuQhJVed3LojbUCyRnsvZHeybvr2F2bVRh4Z7qbjw+CsCNZnW8x3EUqeVxJtNYjoZYJajzl3HG9idfG0Zl8LXRi2QKBgQD+l3EbYIUVydwVB4kCelGMllmKdraw03n8DpW1F3cCdLahxRLgkXzhBBe57MhPz1KLMKiaVtVpsqoYY27+wH/0Wh/PEIWqiawwNTYrGEKP1uIx3+z03HrbwEYddjEA32F8Wfrc8Srzvg7X4G0iAeKz1YLSFGO2F16iugpmTg+jGwKBgQDUZXzp0Or7Jf7PI+4W1+XjR43A8xRUebFamiA6SqIJuxI6D+sKEM3y1OqdiDaB0hbo9oTUP/3VqeswmbSKKH9ga4gdcGcxK4gfcZefn5Xc+bt4nXVT6JdXBL6JP8uDlt2a8d/JTkLX8e7MHuEJbnbqiNa9T7L02vlXjjg29OHyvQKBgH5SmlahL6e9/yuYD41hL/F38HnZqZBXfiFpAzNVr0FStAPUiydOSQ5FP5iLOmEPV7+kpyPdjgriEbAENmDFFzghN8NASXJy2TMaVARSB0TjtFxW5XYhp/w9jQy1Rl9Od0qCQw05xLwoQ6ktvDixgtEEUeL3JvqE4foQIXTdqridAoGAY2QKjC/jhhtFnhEmPTJStYSqZPxbKxy3TbqKEj0SjpMn+FuylUc/2L6h+43eU+nIJsQVbG11jyKwmFGVFoHU+X3YeE49O9kvHee+GEhJjNqgS9UDLnzNNT9XGkrsJWvXz8YX/s/Mn9jq8kIQ9KToqN/X/Ubqa4J84+f9jloR0dECgYBnOlgXsW9jOsTR+SSVLA1dQDsyLXMbrYiLHX2CQW7VgM9VEyhInFF76sEEg75osQAR55qFpMKWXqTo19/LKKi15LP02hQhjO4JY2y4+KcPglBszJOW+obx7c7p3uhPtImeD9MN6ZD9/TPg4QzJZFE0lFoR+utRXJmamOTFm8PvOQ=='
            var privateKey = '-----BEGIN PRIVATE KEY-----'+prvkey+'-----END PRIVATE KEY-----'
            var publicKey = '-----BEGIN PUBLIC KEY-----'+pubkey+'-----END PUBLIC KEY-----'

            let encryptor = new JSEncrypt(options)
            encryptor.setPublicKey(pubkey)
            // encryptor.setPrivateKey(prvkey)
            // let encode = encryptor.encrypt(data)
            let encode = 'psX7El6dlfgWrBU4U3Fj2GnLLOAA4/C5PaAi5PNYJyIwaeKQGV/0cB6fIQylLLLEr8RsZohHCaU+sjQFjaPBLDLkqWm4mQtcs59JwUAZikH4skPWjEnkbZl8LisaBk6XorE4zUWDIZcXMykukjpEd39zkIYJjlwHvT7xwjZy7NEwXUQK5vZ/2xTau6+L22IbyrDSNqCbR200oLOSN3TIi95t6jb+3toDk2uSoiiLIjtCV6peS86vC4wXWWDwdpDpGmBC661Go8vCt/dtgkhyLOJdlNUAeElV5fMd9CZIkWwFJjlu1K/+jB3egYcsYsYqV0PBWA5okjvHyrCDOrzWmw=='
            // let encode = 'yB08mMVCPidDPSDlYbhuC0EIgNFD5cPeQTwBrXEhZhgBp2Sxy16Kmt3Cu6V4Gg1bJvffQch04FuPCc2xwce5kaZ0l0iiEIfUvItkid/EQPYHmABtG3ILsatlSYlaDUvem+b10s+fO6DxBNTmaHiDwVnNxP0X0e1muD+eRie4cMzvxie0pInsswvNq/y1uv0E/oIAWPB+KVer5T/ByG5cpq1t4axwF6CNxSEHFxS/SQqJR430z6Nu2kWBA4e9J/d1CnmtyAfKfUUmRQAgmVwwWthL9rqQ5ql1jNwh7IGgMtPiAkR1kYpE65iXKm8T++qGxgi2vqbeXEPtKeZTBJ2ebA=='
            let raw = encryptor.decryptByPublicKey(encode)
            
            // const privateObj = forge.pki.privateKeyFromPem(privateKey);
            // const publicObj = forge.pki.publicKeyFromPem(publicKey);
            // let bytes = publicObj.encrypt(data)
            // const encode = forge.util.encode64(bytes)
            // let raw = privateObj.decrypt(bytes)
            alert(raw)
        },
        alive () {
            let param = {
                _t: new Date().getTime()
            }
            CryptoApi.alive(param).then(resp => {
                // .
                if (resp && resp.data['code'] == 200) {
                    console.log(resp.data.data)
                    alert(resp.data.code)

                } else {
                    console.log(resp.data.msg)
                    this.$message({
                        message: resp.data.msg,
                        type: 'error'
                    })
                }
            }).catch(error => {
                console.log(error)
                this.$message({
                    message: error,
                    type: 'error'
                })
            })
        },
        goBack () {
            this.$router.push('/')
        }
    }
}
</script>