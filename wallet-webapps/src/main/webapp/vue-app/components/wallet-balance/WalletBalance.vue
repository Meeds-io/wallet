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
          style="cursor: pointer">
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
                  <v-card-text class="pa-2">
                    <a :href="walletUrl" class="display-1 font-weight-bold big-number">{{ walletBalance }} Ȼ</a>
                  </v-card-text>
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
        walletBalance: '',
        walletUrl: `${ eXo.env.portal.context }/${ eXo.env.portal.portalName }/wallet`
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
      }
    }
  }
</script>