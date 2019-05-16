<template>
  <v-card id="walletAccountsList" flat>
    <!-- Ether account actions -->
    <send-ether-modal
      :account="walletAddress"
      :balance="selectedItem && selectedItem.balance"
      :open="sendEtherModal"
      no-button
      @sent="addSendEtherTransaction"
      @close="sendEtherModal = false"
      @error="$emit('error', $event)" />
    <!-- Contract actions -->
    <send-tokens-modal
      :account="walletAddress"
      :contract-details="selectedItem"
      :open="sendTokenModal"
      no-button
      @sent="addSendTokenTransaction"
      @close="sendTokenModal = false"
      @error="$emit('error', $event)" />
    <delegate-tokens-modal
      :contract="selectedItem && selectedItem.contract"
      :contract-details="selectedItem"
      :open="delegateTokenModal"
      no-button
      @sent="addSendTokenTransaction"
      @close="delegateTokenModal = false"
      @error="$emit('error', $event)" />
    <send-delegated-tokens-modal
      :wallet-address="walletAddress"
      :contract="selectedItem && selectedItem.contract"
      :contract-details="selectedItem"
      :open="sendDelegatedTokenModal"
      no-button
      @sent="addSendTokenTransaction"
      @close="sendDelegatedTokenModal = false"
      @error="$emit('error', $event)" />
    <v-container
      v-if="accountsList && accountsList.length"
      id="accountListContainer"
      flat
      fluid
      grid-list-lg>
      <v-layout row wrap>
        <template v-for="(item, index) in accountsList">
          <div
            v-if="(item.isContract && overviewAccounts.indexOf(item.address) > -1) || (!item.isContract && (overviewAccounts.indexOf('ether') > -1 || overviewAccounts.indexOf('fiat') > -1))"
            :key="index"
            class="accountItemContainer">
            <v-hover>
              <v-card
                slot-scope="{hover}"
                :class="`elevation-${hover ? 9 : 2}`"
                width="400px"
                max-width="100%"
                height="210px">
                <v-card-title
                  dark
                  class="primary"
                  data-app>
                  <v-icon
                    :class="!item.error && 'clickable'"
                    dark
                    @click="!item.error && $emit('account-details-selected', item)">
                    {{ item.icon }}
                  </v-icon>
                  <v-spacer />
                  <div :class="item.error ? 'errorHeadline' : 'headline clickable'" @click="!item.error && $emit('account-details-selected', item)">
                    {{ item.title }}
                  </div>
                  <v-spacer v-if="!item.error" />
                  <v-menu
                    v-if="!item.error"
                    :ref="`walletAccountCard${index}`"
                    :attach="`.walletAccountMenuItem${index}`"
                    :class="`walletAccountMenuItem${index}`"
                    content-class="walletAccountMenu">
                    <v-btn
                      slot="activator"
                      :disabled="isReadOnly"
                      dark
                      icon>
                      <v-icon v-if="!isReadOnly">
                        more_vert
                      </v-icon>
                    </v-btn>
                    <v-list v-if="!isReadOnly" class="pt-0 pb-0">
                      <v-list-tile
                        v-if="!item.isContract && item.balance && item.balance !== '0'"
                        @click="
                          selectedItem = item;
                          sendEtherModal = true;
                        ">
                        <v-list-tile-title>
                          Send Ether
                        </v-list-tile-title>
                      </v-list-tile>
                      <v-list-tile
                        v-if="item.isContract && item.balance > 0 && item.etherBalance > 0"
                        @click="
                          selectedItem = item;
                          sendTokenModal = true;
                        ">
                        <v-list-tile-title>
                          Send token
                        </v-list-tile-title>
                      </v-list-tile>
                      <v-list-tile
                        v-if="enableDelegation && item.isContract && item.balance > 0 && item.etherBalance > 0"
                        @click="
                          selectedItem = item;
                          delegateTokenModal = true;
                        ">
                        <v-list-tile-title>
                          Delegate tokens
                        </v-list-tile-title>
                      </v-list-tile>
                      <v-list-tile
                        v-if="enableDelegation && item.isContract && item.balance > 0 && item.etherBalance > 0"
                        @click="
                          selectedItem = item;
                          sendDelegatedTokenModal = true;
                        ">
                        <v-list-tile-title>
                          Send delegated tokens
                        </v-list-tile-title>
                      </v-list-tile>
                      <v-list-tile v-if="!isSpace && item.isContract && !item.isDefault" @click="deleteContract(item, $event)">
                        <v-list-tile-title>
                          Remove from list
                        </v-list-tile-title>
                      </v-list-tile>
                    </v-list>
                  </v-menu>
                </v-card-title>
                <v-card-title
                  :class="!item.error && 'clickable'"
                  class="accountItemContent"
                  @click="!item.error && $emit('account-details-selected', item)">
                  <v-spacer />
                  <div class="text-xs-center">
                    <h4 v-if="item.error" class="mb-0">
                      {{ item.error }}
                    </h4> <h3 v-if="!item.error && (item.balanceFiat === 0 || item.balanceFiat) && overviewAccounts.indexOf('fiat') > -1" class="headline mb-0">
                      {{ `${toFixed(item.balanceFiat)} ${fiatSymbol}` }}
                    </h3> <h4 v-if="!item.error && (item.balance === 0 || item.balance)">
                      {{ `${toFixed(item.balance)} ${item && item.symbol}` }}
                    </h4>
                  </div>
                  <v-spacer v-if="!item.error" />
                  <v-btn
                    v-if="!item.error"
                    icon
                    class="mr-2"
                    @click="!item.error && $emit('account-details-selected', item)">
                    <v-icon>
                      fa-angle-right
                    </v-icon>
                  </v-btn>
                </v-card-title>
              </v-card>
            </v-hover>
          </div>
        </template>
      </v-layout>
    </v-container>
  </v-card>
</template>

<script>
import DelegateTokensModal from './DelegateTokensModal.vue';
import SendDelegatedTokensModal from './SendDelegatedTokensModal.vue';
import SendTokensModal from './SendTokensModal.vue';
import SendEtherModal from './SendEtherModal.vue';

export default {
  components: {
    DelegateTokensModal,
    SendDelegatedTokensModal,
    SendTokensModal,
    SendEtherModal,
  },
  props: {
    isReadOnly: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    accountsDetails: {
      type: Object,
      default: function() {
        return {};
      },
    },
    overviewAccounts: {
      type: Array,
      default: function() {
        return [];
      },
    },
    fiatSymbol: {
      type: String,
      default: function() {
        return null;
      },
    },
    principalAccount: {
      type: String,
      default: function() {
        return null;
      },
    },
    walletAddress: {
      type: String,
      default: function() {
        return null;
      },
    },
    networkId: {
      type: Number,
      default: function() {
        return 0;
      },
    },
    refreshIndex: {
      type: Number,
      default: function() {
        return 1;
      },
    },
  },
  data() {
    return {
      sendEtherModal: false,
      sendTokenModal: false,
      delegateTokenModal: false,
      sendDelegatedTokenModal: false,
      enableDelegation: true,
      selectedItem: null,
    };
  },
  computed: {
    accountsList() {
      // A trick to force Refresh list
      if (!this.refreshIndex) {
        return [];
      }

      if(!this.overviewAccounts) {
        return [];
      }
      const accountsList = [];
      let etherAccountAdded = false;
      this.overviewAccounts.forEach((selectedValue) => {
        if (selectedValue === 'fiat' || selectedValue === 'ether') {
          if (!etherAccountAdded) {
            const accountDetails = Object.assign({}, this.accountsDetails[this.walletAddress]);
            accountsList.push(accountDetails);
            etherAccountAdded = true;
          }
        } else if (this.accountsDetails[selectedValue]) {
          accountsList.push(this.accountsDetails[selectedValue]);
        }
      });

      return accountsList;
    },
  },
  created() {
    this.enableDelegation = window.walletSettings.userPreferences.enableDelegation;
  },
  methods: {
    addSendEtherTransaction(transaction) {
      this.$emit('transaction-sent', transaction);
    },
    checkOpenTransaction() {
      if (document.location.search && document.location.search.length) {
        const search = document.location.search.substring(1);
        const parameters = JSON.parse(
          `{"${decodeURI(search)
            .replace(/"/g, '\\"')
            .replace(/&/g, '","')
            .replace(/=/g, '":"')}"}`
        );
        if (this.walletAddress && this.accountsDetails && parameters && parameters.hash) {
          if (parameters.contract && this.accountsDetails[parameters.contract]) {
            this.$emit('account-details-selected', this.accountsDetails[parameters.contract], parameters.hash);
          } else if (parameters.principal) {
            this.$emit('account-details-selected', this.accountsDetails[this.principalAccount], parameters.hash);
          } else if (this.accountsDetails[this.walletAddress]) {
            this.$emit('account-details-selected', this.accountsDetails[this.walletAddress], parameters.hash);
          } else if (Object.keys(this.accountsDetails).length) {
            this.$emit('account-details-selected', this.accountsDetails[Object.keys(this.accountsDetails)[0]], parameters.hash);
          }
        }
      }
    },
    addSendTokenTransaction(transaction, contract) {
      this.$emit('transaction-sent', transaction);
    },
  },
};
</script>
