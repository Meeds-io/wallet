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
            {{ toFixed(props.item.contractBalance) }} ether
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
        <td class="text-xs-right">
          <v-progress-circular
            v-if="props.item.isPending"
            :width="3"
            indeterminate
            color="primary" />
          <v-btn
            v-else-if="isAdmin"
            icon
            ripple
            @click="deleteContract(props.item, $event)">
            <i class="uiIconTrash uiIconBlue"></i>
          </v-btn>
        </td>
      </template>
    </v-data-table>
    <v-divider />
    <div class="text-xs-center pt-2 pb-2">
      <deploy-new-contract
        v-if="isAdmin"
        :account="walletAddress"
        :network-id="networkId"
        :fiat-symbol="fiatSymbol"
        @list-updated="updateList($event)" />
      <button
        v-if="isAdmin"
        class="btn mt-3"
        @click="showAddContractModal = true">
        Add Existing contract Address
      </button>
      <add-contract-modal
        v-if="isAdmin"
        :net-id="networkId"
        :account="walletAddress"
        :open="showAddContractModal"
        is-default-contract
        @added="contractsModified"
        @close="showAddContractModal = false" />
    </div>
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
        :network-id="networkId"
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
import DeployNewContract from './modals/WalletAdminDeployNewContract.vue';
import AddContractModal from './modals/WalletAdminAddContractModal.vue';
import ContractDetail from './WalletAdminContractDetail.vue';

export default {
  components: {
    DeployNewContract,
    AddContractModal,
    ContractDetail,
  },
  props: {
    networkId: {
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
        {
          text: '',
          align: 'center',
          sortable: false,
          value: 'action',
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
    init(avoidReloading) {
      const previouslyRetrievedContracts = this.contracts;
      this.contracts = [];
      return (avoidReloading ? Promise.resolve(previouslyRetrievedContracts) : this.tokenUtils.getContractsDetails(this.walletAddress, this.networkId, true, true))
        .then((contracts) => (this.contracts = contracts ? contracts.filter((contract) => contract.isDefault) : []))
        .then(() => this.tokenUtils.getContractDeploymentTransactionsInProgress(this.networkId))
        .then((contractsInProgress) => {
          Object.keys(contractsInProgress).forEach((hash) => {
            const contractInProgress = contractsInProgress[hash];
            this.walletUtils.getTransactionReceipt(contractInProgress.hash).then((receipt) => {
              if (!receipt) {
                // pending transaction
                this.contracts.push({
                  name: contractInProgress.name,
                  hash: contractInProgress.hash,
                  address: 'Transaction in progress...',
                  isPending: true,
                });
                const thiss = this;
                this.walletUtils.watchTransactionStatus(contractInProgress.hash, () => {
                  thiss.init();
                });
              } else if (receipt.status && receipt.contractAddress) {
                const contractAddress = receipt.contractAddress.toLowerCase();
                // success transaction
                // Add contract as default if not yet present
                if (contractInProgress.isDefault && !this.contracts.find((contract) => contract.address === contractAddress)) {
                  // This may happen when the contract is already added in //
                  if (window.walletSettings.defaultContractsToDisplay.indexOf(contractAddress)) {
                    this.newTokenAddress = contractAddress;
                    this.tokenUtils.removeContractDeploymentTransactionsInProgress(this.networkId, contractInProgress.hash);
                    this.contractsModified();
                  } else {
                    // Save newly created contract as default
                    return this.tokenUtils.saveContractAddress(this.walletAddress, contractAddress, this.networkId, contractInProgress.isDefault)
                      .then((added, error) => {
                        if (error) {
                          throw error;
                        }
                        if (added) {
                          this.newTokenAddress = contractAddress;
                          this.tokenUtils.removeContractDeploymentTransactionsInProgress(this.networkId, contractInProgress.hash);
                          this.contractsModified();
                        } else {
                          this.error = `Address ${contractAddress} is not recognized as ERC20 Token contract's address`;
                        }
                        this.loadingContracts = false;
                      })
                      .catch((err) => {
                        console.debug('saveContractAddress method - error', err);
                        this.loadingContracts = false;
                        this.error = `${err}`;
                      });
                  }
                } else {
                  // The contract was already saved
                  this.tokenUtils.removeContractDeploymentTransactionsInProgress(this.networkId, contractInProgress.hash);
                }
              } else {
                // failed transaction
                this.contracts.push({
                  name: contractInProgress.name,
                  hash: contractInProgress.hash,
                  address: '',
                  error: `Transaction failed on contract ${contractInProgress.name}`,
                });
              }
            });
          });
        })
        .then(() => this.$emit('contracts-loaded', this.contracts));
    },
    contractsModified() {
      this.init()
        .then(() => (this.loadingContracts = false))
        .catch((e) => {
          console.debug('init method - error', e);
          this.loadingContracts = false;
          this.error = `Error adding new contract address: ${e}`;
        });
    },
    deleteContract(item, event) {
      if (!item || !item.address) {
        this.error = "Contract doesn't have an address";
      }
      this.loadingContracts = true;
      if (item.hash) {
        this.tokenUtils.removeContractDeploymentTransactionsInProgress(this.networkId, item.hash);
        this.contractsModified();
      } else {
        this.tokenUtils.removeContractAddressFromDefault(item.address)
          .then((resp, error) => {
            if (error) {
              this.error = 'Error deleting contract as default';
            } else {
              return this.init();
            }
          })
          .then(() => (this.loadingContracts = false))
          .catch((e) => {
            console.debug('removeContractAddressFromDefault method - error', e);
            this.loadingContracts = false;
            this.error = 'Error deleting contract as default';
          });
      }
      event.preventDefault();
      event.stopPropagation();
    },
    updateList(address) {
      this.loadingContracts = true;
      if (address) {
        this.newTokenAddress = address;
      }
      this.init()
        .then(() => (this.loadingContracts = false))
        .catch((e) => {
          console.debug('init method - error', e);
          this.loadingContracts = false;
          this.error = `Error encountered: ${e}`;
        });
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
