<!--
This file is part of the Meeds project (https://meeds.io/).
Copyright (C) 2020 Meeds Association
contact@meeds.io
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3 of the License, or (at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.
You should have received a copy of the GNU Lesser General Public License
along with this program; if not, write to the Free Software Foundation,
Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
-->
<template>
  <v-app
    id="WalletAdminApp"
    color="transaprent"
    class="VuetifyApp"
    flat>
    <v-main>
      <v-flex class="white text-center application-border-radius pa-5" flat>
        <div v-if="error && !loading" class="alert alert-error v-content">
          <i class="uiIconError"></i>{{ error }}
        </div>
        <v-dialog
          v-model="loading"
          persistent
          hide-overlay
          width="300">
          <v-card color="primary" dark>
            <v-card-text>
              {{ $t('exoplatform.wallet.label.loading') }} ...
              <v-progress-linear
                indeterminate
                color="white"
                class="mb-0" />
            </v-card-text>
          </v-card>
        </v-dialog>
        <wallet-tab
          ref="walletsTab"
          :wallet-address="walletAddress"
          :loading="loading"
          :fiat-symbol="fiatSymbol"
          :refresh-index="refreshIndex"
          :address-etherscan-link="addressEtherscanLink"
          :contract-details="contractDetails"
          @wallets-loaded="wallets = $event" />
      </v-flex>
      <div id="walletDialogsParent"></div>
    </v-main>
  </v-app>
</template>

<script>
export default {
  data() {
    return {
      loading: false,
      fiatSymbol: '$',
      settings: null,
      wallet: null,
      refreshIndex: 1,
      contractDetails: null,
      addressEtherscanLink: null,
      wallets: [],
      anchor: document.URL.indexOf('#') >= 0 ? document.URL.split('#')[1] : null,
    };
  },
  computed: {
    walletAddress() {
      return this.wallet && this.wallet.address && this.wallet.address.toLowerCase();
    },
  },
  created() {
    document.addEventListener('exo.wallet.modified', this.walletUpdated);
    document.addEventListener('exo.contract.modified', this.reloadContract);
    this.init()
      .then(() => {
        this.addressEtherscanLink = this.walletUtils.getAddressEtherscanlink();
      });
  },
  methods: {
    init() {
      this.loading = true;
      this.forceUpdate();
      this.error = null;

      return this.walletUtils.initSettings(false, true, true)
        .then(() => {
          if (!window.walletSettings) {
            this.forceUpdate();
            throw new Error(this.$t('exoplatform.wallet.error.emptySettings'));
          }
          this.settings = window.walletSettings;
          this.fiatSymbol = window.walletSettings.fiatSymbol || '$';
          this.wallet = window.walletSettings.wallet;
        })
        .then(() => this.walletUtils.initWeb3(false, true))
        .then(() => {
          this.walletAddress = this.wallet.address;
        })
        .catch((error) => {
          if (String(error).indexOf(this.constants.ERROR_WALLET_NOT_CONFIGURED) < 0) {
            console.error('Error connecting to network', error);
            this.error = this.$t('exoplatform.wallet.warning.networkConnectionFailure');
          }
        })
        .then(() => {
          if (this.contractDetails) {
            return this.tokenUtils.reloadContractDetails(this.walletAddress).then(result => this.contractDetails = result);
          } else {
            this.contractDetails = this.tokenUtils.getContractDetails(this.walletAddress);
          }
        })
        .then(() => this.$refs.walletsTab.init())
        .catch((error) => {
          if (String(error).indexOf(this.constants.ERROR_WALLET_NOT_CONFIGURED) < 0) {
            console.error(error);
            if (!this.error) {
              this.error = String(error);
            }
          } else {
            this.error = this.$t('exoplatform.wallet.warning.walletNotConfigured');
          }
        })
        .catch((e) => {
          console.error('init method - error', e);
          this.error = String(e);
        })
        .finally(() => {
          this.loading = false;
          this.forceUpdate();
        });
    },
    walletUpdated(event) {
      if (event && event.detail && event.detail.string) {
        const updatedWalletAddress = event.detail.string.toLowerCase();
        if (this.walletAddress === updatedWalletAddress) {
          this.refreshWallet(this.wallet);
        }
        const updatedWallet = this.wallets.find(wallet => wallet && wallet.address && wallet.address.toLowerCase() === updatedWalletAddress);
        if (updatedWallet) {
          this.refreshWallet(updatedWallet);
        }
      }
    },
    reloadContract() {
      return this.tokenUtils.reloadContractDetails(this.walletAddress).then(result => this.contractDetails = result);
    },
    pendingTransaction(transaction) {
      const recipient = transaction.to.toLowerCase();
      const wallet = this.wallets.find((wallet) => wallet && wallet.address && wallet.address === recipient);
      if (wallet) {
        if (transaction.contractAddress) {
          this.$set(wallet, 'loadingTokenBalance', true);
          this.walletUtils.watchTransactionStatus(transaction.hash, () => {
            return this.refreshWallet(wallet).then(() => {
              this.$set(wallet, 'loadingTokenBalance', false);
            });
          });
        } else {
          this.$set(wallet, 'loadingBalance', true);
          this.walletUtils.watchTransactionStatus(transaction.hash, () => {
            return this.refreshWallet(wallet).then(() => {
              this.$set(wallet, 'loadingBalance', false);
            });
          });
        }
      }
    },
    refreshWallet(wallet) {
      return this.addressRegistry.refreshWallet(wallet).then(() => {
        wallet.fiatBalance = wallet.fiatBalance || (wallet.etherBalance && this.walletUtils.etherToFiat(wallet.etherBalance));
      });
    },
    forceUpdate() {
      this.refreshIndex++;
      this.$forceUpdate();
    },
    refreshSettings() {
      this.init();
    },
  },
};
</script>
