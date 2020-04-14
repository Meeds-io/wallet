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
                  <v-card-text
                    class="subtitle-2 text-sub-title pa-2">
                    <span :class="firstLoadingWalletBalance && 'skeleton-text skeleton-background skeleton-header skeleton-border-radius'">{{ $t('exoplatform.wallet.title.walletBalanceTitle') }}</span>
                  </v-card-text>
                </v-card>
              </v-flex>
              <v-flex 
                d-flex 
                xs12 
                justify-center>
                <v-card flat>
                  <v-card-text class="pa-2">
                    <a
                      :href="walletUrl"
                      :class="firstLoadingWalletBalance && 'skeleton-text skeleton-background skeleton-border-radius'"
                      class="display-1 font-weight-bold big-number">{{ walletBalance }} Ȼ</a>
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
        walletUrl: `${ eXo.env.portal.context }/${ eXo.env.portal.portalName }/wallet`,
        firstLoadingWalletBalance: true,
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
              if (this.firstLoadingWalletBalance) {
                this.firstLoadingWalletBalance = false;
              }
            }
        )
      }
    }
  }
</script>