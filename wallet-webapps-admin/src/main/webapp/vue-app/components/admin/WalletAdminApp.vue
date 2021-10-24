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
    <main>
      <v-layout column>
        <v-app class="mb-4 application-toolbar">
          <v-tabs
            v-model="selectedTab"
            slider-size="4">
            <v-tab
              key="wallets"
              href="#wallets">
              {{ $t('exoplatform.wallet.title.walletAdministrationTab') }}
            </v-tab>
            <v-tab
              key="funds"
              href="#funds">
              {{ $t('exoplatform.wallet.title.initialFundsTab') }}
            </v-tab>
            <v-tab
              v-if="contractDetails"
              key="contract"
              href="#contract">
              {{ adminLevel ? $t('exoplatform.wallet.title.contractTab') : $t('exoplatform.wallet.title.transactionHistoryTab') }}
            </v-tab>
          </v-tabs>
          <v-tabs-items v-model="selectedTab" class="tabs-content">
            <v-tab-item
              id="wallets"
              value="wallets"
              eager>
              <v-flex class="white text-center  mt-4" flat>
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
              </v-flex>
              <wallet-tab
                ref="walletsTab"
                :wallet-address="walletAddress"
                :loading="loading"
                :fiat-symbol="fiatSymbol"
                :refresh-index="refreshIndex"
                :address-etherscan-link="addressEtherscanLink"
                :contract-details="contractDetails"
                @pending="$refs && $refs.contractDetail && $refs.contractDetail.refreshTransactionList()"
                @wallets-loaded="wallets = $event" />
            </v-tab-item>
            <v-tab-item
              id="funds"
              value="funds"
              eager>
              <wallet-initial-funds-tab
                ref="fundsTab"
                :loading="loading"
                :settings="settings"
                :contract-details="contractDetails"
                @saved="refreshSettings" />
            </v-tab-item>
            <v-tab-item
              v-if="contractDetails"
              id="contract"
              value="contract"
              eager>
              <wallet-contract-tab
                ref="contractDetail"
                :wallet-address="walletAddress"
                :user-wallet="wallet"
                :wallets="wallets"
                :contract-details="contractDetails"
                :admin-level="adminLevel"
                :fiat-symbol="fiatSymbol"
                :address-etherscan-link="addressEtherscanLink"
                @back="back()"
                @success="reloadContract"
                @pending-transaction="pendingTransaction" />
            </v-tab-item>
          </v-tabs-items>
        </v-app>
      </v-layout>
      <div id="walletDialogsParent"></div>
    </main>
  </v-app>
</template>

<script>
export default {
  data() {
    return {
      loading: false,
      selectedTab: 'wallets',
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
    adminLevel() {
      return this.contractDetails.administrator === eXo.env.portal.userName || true ;
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
      this.showAddContractModal = false;
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
