<template>
  <v-card flat>
    <div v-if="newTokenAddress" class="alert alert-success v-content">
      <i class="uiIconSuccess"></i> Contract created under address:
      <wallet-address :value="newTokenAddress" />
    </div>
    <div v-if="error && !loading" class="alert alert-error v-content">
      <i class="uiIconError"></i>{{ error }}
    </div>
    <v-data-table
      :headers="headers"
      :items="contracts"
      :loading="loadingContracts"
      :sortable="false"
      class="elevation-1 mr-3 ml-3"
      hide-actions>
      <template slot="items" slot-scope="props">
        <td
          :class="props.item.error ? 'red--text' : ''"
          class="clickable"
          @click="openContractDetails(props.item)">
          {{ props.item.error ? props.item.error : props.item.name }}
        </td>
        <td class="clickable text-xs-center" @click="openContractDetails(props.item)">
          <v-progress-circular
            v-if="props.item.loadingBalance"
            color="primary"
            indeterminate
            size="20" />
          <span v-else>
            {{ walletUtils.toFixed(props.item.contractBalance) }} ether
          </span>
        </td>
        <td class="clickable text-xs-center" @click="openContractDetails(props.item)">
          {{ props.item.contractType > 0 && props.item.sellPrice ? `${props.item.sellPrice} ether` : '-' }}
        </td> <td class="clickable text-xs-center" @click="openContractDetails(props.item)">
          {{ props.item.contractTypeLabel }}
        </td>
        <td v-if="props.item.error" class="text-xs-right">
          <del>
            {{ props.item.address }}
          </del>
        </td>
        <td v-else class="text-xs-center">
          <a
            v-if="tokenEtherscanLink"
            :href="`${tokenEtherscanLink}${props.item.address}`"
            target="_blank"
            title="Open on etherscan">
            {{ props.item.address }}
          </a> <span v-else>
            {{ props.item.address }}
          </span>
        </td>
      </template>
    </v-data-table>

    <!-- The selected account detail -->
    <v-navigation-drawer
      id="contractDetailsDrawer"
      v-model="seeContractDetails"
      fixed
      temporary
      right
      stateless
      width="700"
      max-width="100vw">
      <contract-detail
        ref="contractDetail"
        :wallet-address="walletAddress"
        :contract-details="selectedContractDetails"
        :is-display-only="!selectedContractDetails || !selectedContractDetails.isAdmin"
        :fiat-symbol="fiatSymbol"
        :wallets="wallets"
        :address-etherscan-link="addressEtherscanLink"
        @back="back()"
        @pending-transaction="watchPendingTransaction" />
    </v-navigation-drawer>
  </v-card>
</template>
<script>
import ContractDetail from './WalletAdminContractDetail.vue';

export default {
  components: {
    ContractDetail,
  },
  props: {
    walletAddress: {
      type: String,
      default: function() {
        return null;
      },
    },
    isAdmin: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    loading: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    fiatSymbol: {
      type: String,
      default: function() {
        return '$';
      },
    },
    tokenEtherscanLink: {
      type: String,
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
    wallets: {
      type: Object,
      default: function() {
        return null;
      },
    },
  },
  data() {
    return {
      loadingContracts: false,
      newTokenAddress: null,
      showAddContractModal: false,
      seeContractDetails: false,
      seeContractDetailsPermanent: false,
      selectedContractDetails: null,
      contracts: [],
      error: null,
      headers: [
        {
          text: 'Token name',
          align: 'left',
          sortable: false,
          value: 'name',
        },
        {
          text: 'Contract balance',
          align: 'center',
          sortable: false,
          value: 'contractBalance',
        },
        {
          text: 'Token sell price',
          align: 'center',
          sortable: false,
          value: 'sellPrice',
        },
        {
          text: 'Contract type',
          align: 'center',
          sortable: false,
          value: 'contractType',
        },
        {
          text: 'Contract address',
          align: 'center',
          sortable: false,
          value: 'address',
        },
      ],
    };
  },
  watch: {
    loading() {
      if (this.loading) {
        this.back();
      }
    },
    seeContractDetails() {
      if (this.seeContractDetails) {
        $('body').addClass('hide-scroll');
        const thiss = this;
        setTimeout(() => {
          thiss.seeContractDetailsPermanent = true;
        }, 200);
      } else {
        $('body').removeClass('hide-scroll');
        this.seeContractDetailsPermanent = false;
      }
    },
    contracts() {
      this.$emit('contract-list-modified');
    },
  },
  methods: {
    init() {
      this.contracts = [];
      return this.tokenUtils.getContractsDetails(this.walletAddress, true, true)
        .then((contracts) => (this.contracts = contracts ? contracts.filter((contract) => contract.isDefault) : []))
        .then(() => this.$emit('contracts-loaded', this.contracts));
    },
    openContractDetails(contractDetails) {
      if (contractDetails.error) {
        return;
      }
      this.selectedContractDetails = contractDetails;
      this.seeContractDetails = true;

      this.$nextTick(() => {
        const thiss = this;
        $('.v-overlay')
          .off('click')
          .on('click', (event) => {
            thiss.back();
          });
      });
    },
    back() {
      this.seeContractDetails = false;
      this.seeContractDetailsPermanent = false;
      this.selectedContractDetails = null;
    },
    watchPendingTransaction(...args) {
      this.$emit('pending-transaction', ...args);
    },
  },
};
</script>
