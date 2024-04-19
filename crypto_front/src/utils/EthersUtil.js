import { ethers } from 'ethers';
import { KEYUTIL, KJUR, hextob64} from 'jsrsasign';
import CryptoJS from 'crypto-js'



export function createDefaultWallet () {
  var eckey =  new KJUR.crypto.ECDSA({'curve': 'secp256k1'})
  var keypair = eckey.generateKeyPairHex()
  var privateKey = keypair.ecprvhex
  var publicKey = keypair.ecpubhex
  
  // var prvkey = uint8Array2Hex(privateKey)
  debugger
  var wallet = new ethers.Wallet('0x' + privateKey)
  console.log("账号地址: " + wallet.address)

  return wallet
}

function uint8Array2Hex(uint8Array) {
  return Array.prototype.map
    .call(uint8Array, (x) => ('00' + x.toString(16)).slice(-2))
    .join('')
}

export function sign_SHA256withECDSA(prvHex, msg) {
  var sig = new KJUR.crypto.Signature({'alg':'SHA256withECDSA'})
  sig.init({d: prvHex, curve: 'secp256k1'})
  sig.updateString(msg)
  return sig.sign()
}

export function verify_SHA256withECDSA(pubHex, msg, sigValueHex) {
  var sig = new KJUR.crypto.Signature({'alg':'SHA256withECDSA'})
  sig.init({xy: pubHex, curve: 'secp256k1'})
  sig.updateString(msg)
  return sig.verify(sigValueHex)
}

function example() {
  var eckey =  new KJUR.crypto.ECDSA({'curve': 'secp256k1'})
  var keypair = eckey.generateKeyPairHex()
  var privateKey = keypair.ecprvhex
  var publicKey = keypair.ecpubhex
  
  // var prvkey = uint8Array2Hex(privateKey)
  debugger
  var wallet = new ethers.Wallet('0x' + privateKey)
  console.log("账号地址: " + wallet.address)

  var sig = new KJUR.crypto.Signature({'alg':'SHA256withECDSA'})
  sig.init({d: wallet.privateKey.slice(2), curve: 'secp256k1'});
  sig.updateString('coco');
  var sigValueHex = sig.sign();
  debugger
  var sig2 = new KJUR.crypto.Signature({'alg':'SHA256withECDSA', 'prov': 'cryptojs/jsrsa'});
  sig2.init({xy: wallet.signingKey.publicKey.slice(2), curve: 'secp256k1'});
  sig2.updateString('coco');
  var result = sig2.verify(sigValueHex);
}