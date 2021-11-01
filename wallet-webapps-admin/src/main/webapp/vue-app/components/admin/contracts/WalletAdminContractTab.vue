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
  <v-flex
    v-if="contractDetails && contractDetails"
    id="accountDetail"
    class="text-center white layout column">
    <v-card-title class="align-start accountDetailSummary">
      <v-layout column>
        <v-flex
          id="accountDetailTitle"
          class="mt-3">
          <h3 v-if="contractDetails.fiatBalance" class="font-weight-light">
            {{ contractDetails.name }} - {{ $t('exoplatform.wallet.label.version') }}: {{ contractDetails.contractType }} - {{ $t('exoplatform.wallet.label.balance') }}: {{ walletUtils.toFixed(contractDetails.fiatBalance) }} {{ fiatSymbol }} / {{ walletUtils.toFixed(contractDetails.etherBalance) }} ether
          </h3>
          <h4 v-if="contractDetails.owner" class="grey--text font-weight-light no-wrap">
            {{ $t('exoplatform.wallet.label.owner') }}: <wallet-reward-wallet-address :value="contractDetails.owner" display-label />
          </h4>
        </v-flex>

        <v-flex id="accountDetailActions">
          <!-- Send ether -->
          <wallet-send-ether-modal
            :account="walletAddress"
            :balance="contractDetails.balance"
            :recipient="contractDetails.address"
            use-navigation
            disabled-recipient
            @success="successSendingEther"
            @sent="newTransactionPending"
            @error="transactionError" />

          <wallet-contract-admin-modal
            v-if="contractDetails && contractDetails.isOwner"
            ref="transferOwnership"
            :contract-details="contractDetails"
            :wallet="userWallet"
            :title="$t('exoplatform.wallet.button.transferOwnership')"
            :autocomplete-label="$t('exoplatform.wallet.label.newTokenOwner')"
            :autocomplete-placeholder="$t('exoplatform.wallet.label.newTokenOwnerPlaceholder')"
            method-name="transferOwnership"
            @sent="newTransactionPending"
            @success="successTransaction"
            @error="transactionError" />

          <wallet-initialize-modal
            ref="initializeAccount"
            :contract-details="contractDetails"
            :wallet="userWallet"
            method-name="transferOwnership"
            @sent="newTransactionPending"
            @success="successTransaction"
            @error="transactionError" />
        </v-flex>
      </v-layout>
    </v-card-title>

    <v-tabs v-model="selectedTab" grow>
      <v-tabs-slider color="primary" />
      <v-tab
        key="transactions"
        href="#transactions">
        {{ $t('exoplatform.wallet.title.transactions') }}{{ totalTransactionsCount ? ` (${totalTransactionsCount})` : '' }}
      </v-tab>
    </v-tabs>
    <v-tabs-items v-model="selectedTab">
      <v-tab-item
        id="transactions"
        value="transactions"
        eager>
        <v-layout column>
          <v-flex xs12>
            <wallet-reward-transactions-list
              id="transactionsList"
              ref="transactionsList"
              :contract-details="contractDetails"
              :fiat-symbol="fiatSymbol"
              :error="error"
              display-full-transaction
              administration
              @loaded="computeTransactionsCount"
              @error="error = $event" />
          </v-flex>
        </v-layout>
      </v-tab-item>
    </v-tabs-items>
  </v-flex>
</template>

<script>
export default {
  props: {
    walletAddress: {
      type: String,
      default: function() {
        return null;
      },
    },
    userWallet: {
      type: Object,
      default: function() {
        return null;
      },
    },
    contractDetails: {
      type: Object,
      default: function() {
        return null;
      },
    },
    fiatSymbol: {
      type: String,
      default: function() {
        return null;
      },
    },
    wallets: {
      type: Array,
      default: function() {
        return null;
      },
    },
    addressEtherscanLink: {
      type: String,
      default: function() {
        return null;
      },
    },
  },
  data() {
    return {
      selectedTab: -1,
      totalTransactionsCount: 0,
      adminWallets: [],
      checkingPendingTransactions: false,
      error: null,
    };
  },
  computed: {
    totalSupply() {
      return this.contractDetails && this.contractDetails.totalSupply ? this.walletUtils.convertTokenAmountReceived(this.contractDetails.totalSupply, this.contractDetails.decimals) : 0;
    },
  },
  created() {
    this.$nextTick(() => this.selectedTab = 'transactions');
  },
  methods: {
    refreshTransactionList() {
      if (this.$refs.transactionsList) {
        this.$refs.transactionsList.init();
      }
    },
    successSendingEther() {
      this.refreshBalance()
        .then(() => {
          this.$emit('success');
          this.$forceUpdate();
        });
    },
    retrieveAccountDetails(wallet, ignoreApproved) {
      const promises = [];
      if (!ignoreApproved && !wallet.hasOwnProperty('approved')) {
        promises.push(
          this.contractDetails.contract.methods
            .isApprovedAccount(wallet.address)
            .call()
            .then((approved) => {
              this.$set(wallet, 'approved', approved);
            })
            .catch((e) => {
              console.error('Error getting approval of account', wallet.address, e);
              delete wallet['approved'];
            })
        );
      }
      return Promise.all(promises);
    },
    newTransactionPending(transaction) {
      this.$emit('pending-transaction', transaction);
      if (this.$refs.transactionsList) {
        this.$refs.transactionsList.init(true);
      }
      if (this.contractDetails && transaction.value > 0) {
        this.$set(this.contractDetails, 'loadingBalance', true);
      }
      this.$forceUpdate();
    },
    transactionError(error) {
      this.error = String(error);
      this.$forceUpdate();
    },
    computeTransactionsCount(transactions, count) {
      this.totalTransactionsCount = count;
    },
    refreshBalance() {
      return this.tokenUtils.getSavedContractDetails(this.contractDetails.address).then((contractDetails) => {
        Object.assign(this.contractDetails, contractDetails);
      })
        .finally(() => {
          this.$set(this.contractDetails, 'loadingBalance', false);
        });
    },
    loadApprovedWalletsFromContract() {
      this.approvedWalletsLoadedFromContract = true;
      this.loadWalletsFromContract('ApprovedAccount', 'target', this.approvedWallets)
        .then(() => this.approvedWalletsLoadedFromContract = false);
    },
    loadAdminWalletsFromContract() {
      this.loadWalletsFromContract('AddedAdmin', 'target', this.adminWallets);
    },
    loadWalletsFromContract(eventName, paramName, walletsArray) {
      this.loadingApprovedWalletsFromContract = true;
      this.loadingAdminWalletsFromContract = true;
      try {
        return this.contractDetails.contract.getPastEvents(eventName, {
          fromBlock: 0,
          toBlock: 'latest',
          filter: {
            isError: 0,
            txreceipt_status: 1,
          },
        })
          .then((events) => {
            if (events && events.length) {
              const promises = [];
              events.forEach((event) => {
                if (event.returnValues && event.returnValues[paramName]) {
                  const address = event.returnValues[paramName].toLowerCase();
                  let wallet = this.wallets.find(walletTmp => walletTmp.address && walletTmp.address.toLowerCase() === address);
                  if (!wallet) {
                    wallet = {
                      address: address,
                    };
                  }
                  if (this.contractDetails.owner && address.toLowerCase() === this.contractDetails.owner.toLowerCase()) {
                    wallet.owner = true;
                    wallet.name = 'Admin';
                  }
                  walletsArray.unshift(wallet);
                  promises.push(
                    Promise.resolve(this.retrieveAccountDetails(wallet))
                  );
                }
              });
              return Promise.all(promises);
            }
          })
          .catch((e) => {
            console.error('Error loading wallets from Contract', e);
          })
          .finally(() => {
            this.loadingApprovedWalletsFromContract = false;
            this.loadingAdminWalletsFromContract = false;
          });
      } catch (e) {
        this.loadingApprovedWalletsFromContract = false;
        this.loadingAdminWalletsFromContract = false;
        console.error('Error loading wallets from Contract', e);
        return Promise.reject(e);
      }
    },
    successTransaction(hash, methodName, autoCompleteValue) {
      if (methodName === 'disapproveAccount') {
        const index = this.approvedWallets.findIndex(wallet => wallet.address === autoCompleteValue);
        if (index >= 0) {
          this.approvedWallets.splice(index, 1);
        }
      }
    },
  },
};
</script>
