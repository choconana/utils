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
      <el-form-item>
        <el-button type="primary" @click="connect()">connect</el-button>
      </el-form-item>
      <el-form-item>
        <el-input placeholder="请输入金额(元)" v-model="amount"></el-input>
        <el-button type="primary" @click="mint()">mint</el-button>
      </el-form-item>
      <el-form-item>
        <el-input placeholder="请输入金额(元)" v-model="burnAmt"></el-input>
        <el-button type="primary" @click="burn()">burn</el-button>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="totalSupply()">totalSupply</el-button>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="txQuery()">txQuery</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
  import { createDefaultWallet, sign_SHA256withECDSA, verify_SHA256withECDSA } from '../utils/EthersUtil'
  import WalletApi from '../api/wallet'
  import TransactionApi from '../api/transaction'

  export default {
    data () {
      wallet: null
      return {
        amount: 0,
        burnAmt: 0
      } 
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
      },
      connect() {
        debugger
        const sigValueHex = sign_SHA256withECDSA(this.wallet.privateKey.slice(2), 'coco')
        let params = {
          sig: sigValueHex,
          pub: this.wallet.signingKey.compressedPublicKey.slice(2)
        }
        WalletApi.connect(params).then(resp => {
          alert(resp.data)
        }).catch(error => {
          console.log(error)
          this.$message({
              message: error,
              type: 'error'
          })
        })
      },
      mint() {
        let params = {
          pub: this.wallet.signingKey.compressedPublicKey.slice(2),
          amount: this.amount
        }
        WalletApi.mint(params).then(resp => {
          alert(resp.data)
        }).catch(error => {
          console.log(error)
          this.$message({
              message: error,
              type: 'error'
          })
        })
      },
      burn() {
        let params = {
          pub: this.wallet.signingKey.compressedPublicKey.slice(2),
          amount: this.burnAmt
        }
        WalletApi.burn(params).then(resp => {
          alert(resp.data)
        }).catch(error => {
          console.log(error)
          this.$message({
              message: error,
              type: 'error'
          })
        })
      },
      totalSupply() {
        let params = {
          pub: this.wallet.signingKey.compressedPublicKey.slice(2)
        }
        WalletApi.totalSupply(params).then(resp => {
          alert(resp.data)
        }).catch(error => {
          console.log(error)
          this.$message({
              message: error,
              type: 'error'
          })
        })
      },
      txQuery() {
        let params = {
          pub: this.wallet.signingKey.compressedPublicKey.slice(2)
        }
        TransactionApi.query(params).then(resp => {
          alert(resp.data)
        }).catch(error => {
          console.log(error)
          this.$message({
              message: error,
              type: 'error'
          })
        })
      }
    }
  }
</script>