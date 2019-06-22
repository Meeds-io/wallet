<template>
  <v-card
    class="text-xs-center pr-3 pl-3 pt-2"
    flat>
    <v-card-title>
      The following settings manages the funds holder and the amount of initial funds to send for a user that has created a new wallet for the first time. You can choose to set initial funds for a token to 0 so that no funds will be send. The funds holder will receive a notification per user per currency (ether and/or token).
    </v-card-title>
    <v-card-text>
      <v-flex id="fundsHolderAutoComplete" class="contactAutoComplete">
        <v-autocomplete
          ref="fundsHolderAutoComplete"
          v-model="fundsHolder"
          :items="fundsHolderOptions"
          :loading="isLoadingSuggestions"
          :search-input.sync="fundsHolderSearchTerm"
          attach="#fundsHolderAutoComplete"
          label="Wallet funds holder"
          class="contactAutoComplete"
          placeholder="Start typing to Search a user"
          content-class="contactAutoCompleteContent"
          max-width="100%"
          item-text="name"
          item-value="id"
          hide-details
          hide-selected
          chips
          cache-items
          dense
          flat>
          <template slot="no-data">
            <v-list-tile>
              <v-list-tile-title>
                Search for a <strong>
                  user
                </strong>
              </v-list-tile-title>
            </v-list-tile>
          </template>

          <template slot="selection" slot-scope="{item, selected}">
            <v-chip :selected="selected" class="autocompleteSelectedItem">
              <span>
                {{ item.name }}
              </span>
            </v-chip>
          </template>

          <template slot="item" slot-scope="{item}">
            <v-list-tile-avatar
              v-if="item.avatar"
              tile
              size="20">
              <img :src="item.avatar">
            </v-list-tile-avatar>
            <v-list-tile-title v-text="item.name" />
          </template>
        </v-autocomplete>
      </v-flex>

      <v-textarea
        id="requestMessage"
        v-model="requestMessage"
        name="requestMessage"
        label="Initial funds default message"
        placeholder="You can enter a default message to send with initial funds"
        class="mt-4 mb-0"
        rows="7"
        flat
        no-resize />
    </v-card-text>

    <v-card-text class="text-xs-left">
      <v-label light>
        Default amount of automatic initial funds request
      </v-label>
      <v-data-table
        :headers="initialFundsHeaders"
        :items="initialFunds"
        :sortable="false"
        hide-actions>
        <template slot="items" slot-scope="props">
          <td class="text-xs-left">
            {{ props.item.name ? props.item.name : props.item.address }}
          </td>
          <td>
            <v-text-field v-model="props.item.amount" single-line />
          </td>
        </template>
      </v-data-table>
    </v-card-text>
    <v-card-actions>
      <v-spacer />
      <button class="btn btn-primary mb-3" @click="save">
        Save
      </button>
      <v-spacer />
    </v-card-actions>
  </v-card>
</template>
<script>
export default {
  props: {
    loading: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    contractDetails: {
      type: Object,
      default: function() {
        return null;
      },
    },
  },
  data() {
    return {
      fundsHolder: '',
      fundsHolderType: 'user',
      fundsHolderOptions: [],
      fundsHolderSearchTerm: null,
      isLoadingSuggestions: false,
      initialFunds: [],
      initialFundsHeaders: [
        {
          text: 'Name',
          align: 'left',
          sortable: false,
          value: 'name',
        },
        {
          text: 'Amount',
          align: 'center',
          sortable: false,
          value: 'amount',
        },
      ],
    };
  },
  watch: {
    contractDetails() {
      this.reloadInitialFunds();
    },
    fundsHolderSearchTerm() {
      if (!this.fundsHolderSearchTerm || !this.fundsHolderSearchTerm.length) {
        return;
      }
      this.isLoadingSuggestions = true;
      this.addressRegistry.searchUsers(this.fundsHolderSearchTerm, true)
        .then((items) => {
          if (items) {
            this.fundsHolderOptions = items;
          } else {
            this.fundsHolderOptions = [];
          }
          this.isLoadingSuggestions = false;
        })
        .catch((e) => {
          console.debug('searchUsers method - error', e);
          this.isLoadingSuggestions = false;
        });
    },
    fundsHolder(newValue, oldValue) {
      if (oldValue) {
        this.fundsHolderSearchTerm = null;
      }
    },
  },
  methods: {
    init() {
      if (window.walletSettings.fundsHolder) {
        this.addressRegistry.searchUsers(this.fundsHolder, true).then((items) => {
          if (items) {
            this.fundsHolderOptions = items;
          } else {
            this.fundsHolderOptions = [];
          }
        });
      }
      const initialFunds = (window.walletSettings && window.walletSettings.initialFunds) || {};

      this.fundsHolder = initialFunds.fundsHolder;
      this.fundsHolderType = initialFunds.fundsHolderType;
      this.requestMessage = initialFunds.requestMessage;
      this.reloadInitialFunds();
    },
    reloadInitialFunds() {
      let initialFunds = (window.walletSettings && window.walletSettings.initialFunds) || {};
      initialFunds = Object.keys(initialFunds.funds).map(address => {return {address: address, amount: initialFunds.funds[address]};});

      const etherInitialFund = initialFunds.find((initialFund) => initialFund.address === 'ether');
      const etherAmount = (etherInitialFund && etherInitialFund.amount) || 0;
      this.initialFunds = [{
        name: 'ether',
        address: 'ether',
        amount: etherAmount
      }];

      if (this.contractDetails && this.contractDetails.address && this.contractDetails.address.indexOf('0x') === 0) {
        const tokenInitialFund = initialFunds.find((initialFund) => initialFund.address && initialFund.address.toLowerCase() === this.contractDetails.address.toLowerCase());
        const tokenAmount = (tokenInitialFund && tokenInitialFund.amount) || 0;
        this.initialFunds.push({
          name: this.contractDetails.name,
          address: this.contractDetails.address,
          amount: tokenAmount
        });
      }
    },
    save() {
      const initialFundsMap = {};
      if (this.initialFunds && this.initialFunds.length) {
        this.initialFunds.forEach((initialFund) => {
          initialFundsMap[initialFund.address] = initialFund.amount;
        });
      }
      const initialFunds = {
        fundsHolder: this.fundsHolder,
        fundsHolderType: this.fundsHolderType,
        requestMessage: this.requestMessage,
        funds: initialFundsMap,
      };

      this.loading = true;
      return fetch('/portal/rest/wallet/api/settings/saveInitialFunds', {
        method: 'POST',
        credentials: 'include',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(initialFunds),
      })
        .then((resp) => {
          if (resp && resp.ok) {
            return resp.text();
          } else {
            throw new Error('Error saving global settings');
          }
        })
        .then(() => this.$emit('saved', initialFunds))
        .catch((e) => {
          this.loading = false;
          console.debug('fetch settings - error', e);
          this.error = 'Error saving global settings';
        });
    },
  },
};
</script>
