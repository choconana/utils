import Vue from 'vue'
import * as Router from 'vue-router'
Vue.use(Router)

export default Router.createRouter({
    routes: [
        {
            path: '/cryptoTest',
            name: 'cryptoTest',
            component: () => import('./components/CryptoTest.vue')
        },
        {
            path: '/switchTest',
            name: 'switchTest',
            component: () => import('./components/SwitchTest.vue')
        },
        {
            path: '/ethersTest',
            name: 'ethersTest',
            component: () => import('./components/EthersTest.vue')
        }
    ]
})