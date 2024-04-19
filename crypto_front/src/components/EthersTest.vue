<template>
  <h3>以太测试</h3>
  <div class="ethersTest">
    <el-form>
      <el-form-item>
        <el-button type="primary" @click="createWallet()">createWallet</el-button>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="sign()">sign</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
  import { createDefaultWallet, sign_SHA256withECDSA, verify_SHA256withECDSA } from '../utils/EthersUtil'

  export default {
    data () {
      wallet: null
    },
    created () {
      console.log('ether created...')
    },
    mounted () {
      console.log('ether mounted...')
    },
    methods: {
      createWallet() {
        this.wallet = createDefaultWallet()
        alert(this.wallet.address)
      },
      sign() {
        const sigValueHex = sign_SHA256withECDSA(this.wallet.privateKey.slice(2), 'coco')
        console.log(sigValueHex)
        debugger
        const res = verify_SHA256withECDSA(this.wallet.signingKey.publicKey.slice(2), 'coco', sigValueHex)
        alert(res)
      }
    }
  }
</script>