import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'
import * as VueRouter from 'vue-router'
// import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import CryptoApi from '@/api/crypto'

const routes = [
    /*{
        path: '/',
        name: 'cryptoTest',
        component: () => import('./components/CryptoTest.vue')
    },
    {
        path: '/switchTest',
        name: 'switchTest',
        component: () => import('./components/SwitchTest.vue')
    },*/
    {
        path: '/',
        name: 'ethersTest',
        component: () => import('./components/EthersTest.vue')
    }
]

const router = VueRouter.createRouter({
    // 4. 内部提供了 history 模式的实现。为了简单起见，我们在这里使用 hash 模式。
    history: VueRouter.createWebHashHistory(),
    routes, // `routes: routes` 的缩写
})

console.log("1111")
var aliveTask = setInterval(() => {
    setTimeout(() => {
        console.log('=====执行alive定时任务中=====, 时间:' + new Date())
        CryptoApi.alive().catch(error => {})
    }, 1000)
}, 60 * 1000)

router.beforeEach((to, from) => {
    // .
    // clearInterval(aliveTask)
    console.log('执行alive定时任务' + new Date())
    CryptoApi.alive().catch(error => {})
})
var s1 = '', s2 = ''
console.log('s1' + !s1)
console.log('s1:s2' + !s1 || !s2)
console.log(Number.MAX_SAFE_INTEGER)
console.log(Math.random())
console.log(Math.random() * Number.MAX_SAFE_INTEGER + 99999)
const app = createApp(App)
app.use(ElementPlus)
app.use(router)
app.mount('#app')
