<template>
  <v-app
    id="walletBalancePortlet"
    flat
    dark>
    <main>
      <v-container pa-0>
        <v-layout
          row
          wrap
          mx-0
          style="cursor: pointer"
          @click="navigateTo('wallet')">
          <v-flex
            d-flex
            sx12>
            <v-layout
              row
              ma-0
              class="white">
              <v-flex d-flex xs12>
                <v-card flat>
                  <v-card-text class="subtitle-2 grey--text pa-2">{{ this.$t('exoplatform.wallet.title.walletBalanceTitle') }}</v-card-text>
                </v-card>
              </v-flex>
              <v-flex 
                d-flex 
                xs12 
                justify-center>
                <v-card flat>
                  <v-card-text class="display-1 font-weight-bold pa-2 big-number">{{ walletBalance }} Ȼ</v-card-text>
                </v-card>
              </v-flex>
            </v-layout>
          </v-flex>
        </v-layout>
      </v-container>
    </main>
  </v-app>
</template>

<script>
  import {getWalletAccount} from '../../WalletBalanceAPI.js'
  export default {
    data() {
      return {
        walletBalance: ''
      }
    },
    created() {
      this.getRewardBalance();
    },
    methods: {
      getRewardBalance() {
        getWalletAccount().then(
                (data) => {
                  this.walletBalance = Math.trunc(data.tokenBalance);
                }
        )
      },
      navigateTo(pagelink) {
        location.href=`${ eXo.env.portal.context }/${ eXo.env.portal.portalName }/${ pagelink }` ;
      },
    }
  }
</script>