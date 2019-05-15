<template>
  <v-flex flat>
    <confirm-dialog
      ref="informationModal"
      :loading="loading"
      :title="informationTitle"
      :message="informationMessage"
      :hide-actions="hideConfirmActions"
      width="400px"
      @ok="proceessAction" />
    <div v-if="error" class="alert alert-error v-content">
      <i class="uiIconError"></i>{{ error }}
    </div>
    <v-container>
      <v-layout>
        <v-flex md3 xs12>
          <v-switch v-model="displayUsers" label="Users" />
        </v-flex>
        <v-flex md3 xs12>
          <v-switch v-model="displaySpaces" label="Spaces" />
        </v-flex>
        <v-flex md3 xs12>
          <v-switch v-model="displayDisabledWallets" label="Disabled wallets" />
        </v-flex>
        <v-flex md3 xs12>
          <v-switch v-model="displayDisapprovedWallets" label="Disapproved wallets" />
        </v-flex>
      </v-layout>
      <v-flex>
        <v-text-field
          v-model="search"
          append-icon="search"
          label="Search in name, address"
          single-line
          hide-details />
      </v-flex>
    </v-container>
    <v-data-table
      :headers="walletTableHeaders"
      :items="filteredWallets"
      :loading="loadingWallets"
      hide-actions>
      <template slot="items" slot-scope="props">
        <transition name="fade">
          <tr v-show="props.item.displayedWallet">
            <td class="clickable" @click="openAccountDetail(props.item)">
              <v-avatar size="36px">
                <img
                  v-if="props.item.avatar"
                  :src="props.item.avatar"
                  onerror="this.src = '/eXoSkin/skin/images/system/SpaceAvtDefault.png'">
                <v-icon v-else size="36">fa-cog</v-icon>
              </v-avatar>
            </td>
            <td class="clickable text-xs-left" @click="openAccountDetail(props.item)">
              <profile-chip
                :address="props.item.address"
                :profile-id="props.item.id"
                :profile-technical-id="props.item.technicalId"
                :space-id="props.item.spaceId"
                :profile-type="props.item.type"
                :display-name="props.item.name"
                :enabled="props.item.enabled"
                :disapproved="props.item.disapproved"
                :deleted-user="props.item.deletedUser"
                :disabled-user="props.item.disabledUser"
                :avatar="props.item.avatar"
                display-no-address />
            </td>
            <td
              v-if="principalContract && principalContract.contractType && principalContract.contractType > 1"
              class="clickable"
              @click="openAccountDetail(props.item)">
              <template v-if="props.item.type === 'user' || props.item.type === 'space'">
                <v-icon
                  v-if="props.item.initializationState === 'NEW'"
                  color="primary"
                  title="New wallet">
                  fa-hands
                </v-icon>
                <v-icon
                  v-else-if="props.item.initializationState === 'MODIFIED'"
                  color="orange"
                  title="Modified wallet">
                  fa-hands
                </v-icon>
                <v-icon
                  v-else-if="props.item.initializationState === 'DENIED'"
                  color="orange"
                  title="Denied to access wallet">
                  fa-times-circle
                </v-icon>
                <v-icon
                  v-else-if="props.item.initializationState === 'PENDING'"
                  color="primary"
                  title="Initialization in progress">
                  fa-dot-circle
                </v-icon>
                <v-icon
                  v-else-if="props.item.initializationState === 'INITIALIZED'"
                  color="green"
                  title="Initialized wallet">
                  fa-check-circle
                </v-icon>
              </template>
            </td>
            <td class="clickable" @click="openAccountDetail(props.item)">
              <a
                v-if="addressEtherscanLink"
                :href="`${addressEtherscanLink}${props.item.address}`"
                target="_blank"
                title="Open on etherscan">
                {{ props.item.address }}
              </a>
              <span v-else>
                {{ props.item.address }}
              </span>
            </td>
            <td
              v-if="principalContract"
              class="clickable text-xs-center"
              @click="openAccountDetail(props.item)">
              <v-progress-circular
                v-if="props.item.loadingBalancePrincipal"
                :title="loadingWallets ? 'Loading balance' : 'A transaction is in progress'"
                color="primary"
                indeterminate
                size="20" />
              <span
                v-else-if="loadingWallets && props.item.loadingBalancePrincipal !== false"
                title="Loading balance...">
                loading...
              </span>
              <template v-else-if="props.item.balancePrincipal">
                {{ toFixed(props.item.balancePrincipal) }} {{ principalContract && principalContract.symbol ? principalContract.symbol : '' }}
              </template>
              <template v-else>
                -
              </template>
            </td>
            <td class="clickable text-xs-center" @click="openAccountDetail(props.item)">
              <v-progress-circular
                v-if="props.item.loadingBalance"
                :title="loadingWallets ? 'Loading balance' : 'A transaction is in progress'"
                color="primary"
                class="mr-4"
                indeterminate
                size="20" />
              <span
                v-else-if="loadingWallets && props.item.loadingBalance !== false"
                title="Loading balance...">
                loading...
              </span>
              <template v-else>
                {{ toFixed(props.item.balance) }} eth
              </template>
            </td>
            <td class="text-xs-center">
              <v-progress-circular
                v-if="props.item.pendingTransaction"
                title="A transaction is in progress"
                color="primary"
                class="mr-4"
                indeterminate
                size="20" />
              <v-menu v-else-if="isAdmin" offset-y>
                <v-btn
                  slot="activator"
                  icon
                  small>
                  <v-icon size="20px">fa-ellipsis-v</v-icon>
                </v-btn>
                <v-list flat class="pt-0 pb-0">
                  <v-list-tile @click="refreshWallet(props.item)">
                    <v-list-tile-title>Refresh</v-list-tile-title>
                  </v-list-tile>
                  <v-divider />

                  <template v-if="props.item.type === 'user' || props.item.type === 'space'">
                    <template v-if="useWalletAdmin">
                      <template v-if="principalContract && principalContract.contractType && principalContract.contractType > 1 && props.item.initializationState === 'NEW' || props.item.initializationState === 'MODIFIED' || props.item.initializationState === 'DENIED'">
                        <v-list-tile @click="openAcceptInitializationModal(props.item)">
                          <v-list-tile-title>Initialize wallet</v-list-tile-title>
                        </v-list-tile>
                        <v-list-tile v-if="props.item.initializationState !== 'DENIED'" @click="openDenyInitializationModal(props.item)">
                          <v-list-tile-title>Reject wallet</v-list-tile-title>
                        </v-list-tile>
                        <v-divider />
                      </template>
  
                      <template v-if="principalContract && principalContract.contractType && principalContract.contractType > 0 && (props.item.disapproved === true || props.item.disapproved === false)">
                        <v-list-tile v-if="props.item.disapproved === true" @click="openApproveModal(props.item)">
                          <v-list-tile-title>Approve wallet</v-list-tile-title>
                        </v-list-tile>
                        <v-list-tile v-else-if="props.item.disapproved === false" @click="openDisapproveModal(props.item)">
                          <v-list-tile-title>Disapprove wallet</v-list-tile-title>
                        </v-list-tile>
                        <v-divider />
                      </template>
                    </template>
  
                    <v-list-tile v-if="props.item.enabled" @click="openDisableWalletModal(props.item)">
                      <v-list-tile-title>Disable wallet</v-list-tile-title>
                    </v-list-tile>
                    <v-list-tile v-else-if="!props.item.disabledUser && !props.item.deletedUser" @click="enableWallet(props.item, true)">
                      <v-list-tile-title>Enable wallet</v-list-tile-title>
                    </v-list-tile>
  
                    <v-divider />
  
                    <v-list-tile @click="openRemoveWalletModal(props.item)">
                      <v-list-tile-title>Remove wallet</v-list-tile-title>
                    </v-list-tile>
                  </template>
                </v-list>
              </v-menu>
            </td>
          </tr>
        </transition>
      </template>
    </v-data-table>
    <v-flex v-if="showLoadMore" justify-center>
      <v-btn
        :loading="loading"
        color="primary"
        flat
        @click="limit += pageSize">
        Load More
      </v-btn>
    </v-flex>

    <initialize-account-modal
      ref="initAccountModal"
      @sent="walletInitialized" />

    <!-- The selected account detail -->
    <v-navigation-drawer
      id="accountDetailsDrawer"
      v-model="seeAccountDetails"
      fixed
      temporary
      right
      stateless
      width="700"
      max-width="100vw">
      <account-detail
        ref="accountDetail"
        :fiat-symbol="fiatSymbol"
        :network-id="networkId"
        :wallet-address="selectedWalletAddress"
        :contract-details="selectedWalletDetails"
        :selected-transaction-hash="selectedTransactionHash"
        :wallet="selectedWallet"
        is-read-only
        is-display-only
        is-administration
        @back="back()" />
    </v-navigation-drawer>
  </v-flex>
</template>

<script>
import InitializeAccountModal from './modals/WalletAdminInitializeAccountModal.vue';

export default {
  components: {
    InitializeAccountModal,
  },
  props: {
    networkId: {
      type: String,
      default: function() {
        return null;
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
        return null;
      },
    },
    walletAddress: {
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
    principalContract: {
      type: Object,
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
  },
  data() {
    return {
      search: null,
      loadingWallets: false,
      appInitialized: false,
      useWalletAdmin: false,
      displayUsers: true,
      displaySpaces: true,
      displayDisapprovedWallets: true,
      displayDisabledWallets: false,
      selectedTransactionHash: null,
      seeAccountDetails: false,
      seeAccountDetailsPermanent: false,
      selectedWalletAddress: null,
      selectedWallet: null,
      selectedWalletDetails: null,
      informationTitle: null,
      informationMessage: null,
      hideConfirmActions: null,
      confirmAction: null,
      error: null,
      wallets: [],
      walletToProcess: null,
      etherAmount: null,
      tokenAmount: null,
      limit: 10,
      pageSize: 10,
      walletHeaders: [
        {
          text: '',
          align: 'center',
          sortable: false,
          value: 'avatar',
        },
        {
          text: 'Name',
          align: 'left',
          sortable: true,
          value: 'name',
        },
        {
          text: 'Initialization status',
          align: 'center',
          sortable: true,
          value: 'initializationState',
        },
        {
          text: 'Address',
          align: 'center',
          sortable: false,
          value: 'address',
        },
        {
          text: 'Principal balance',
          align: 'center',
          value: 'balancePrincipal',
        },
        {
          text: 'Ether balance',
          align: 'center',
          value: 'balance',
        },
        {
          text: '',
          align: 'center',
          sortable: false,
          value: '',
        },
      ],
    };
  },
  computed: {
    etherAccountDetails() {
      return {
        title: 'ether',
        icon: 'ether',
        symbol: 'ether',
        isContract: false,
        balance: 10, //  Fake balance to not display unsufficient funds
        address: this.walletAddress,
      };
    },
    accountsDetails() {
      const accountsDetails = {};
      if (this.principalContract && this.principalContract.address) {
        accountsDetails[this.principalContract.address] = this.principalContract;
      }
      if (this.walletAddress) {
        accountsDetails[this.walletAddress] = this.etherAccountDetails;
      }
      return accountsDetails;
    },
    walletTableHeaders() {
      const walletTableHeaders = this.walletHeaders.slice();
      if (!this.isAdmin) {
        walletTableHeaders.splice(walletTableHeaders.length - 1, 1);
      }
      if (!this.principalContract) {
        walletTableHeaders.splice(4, 1);
      }
      if (!this.principalContract || this.principalContract.contractType < 2) {
        walletTableHeaders.splice(2, 1);
      }
      return walletTableHeaders;
    },
    showLoadMore() {
      return this.displayedWallets.length === this.limit;
    },
    displayedWallets() {
      if (this.displayUsers && this.displayDisapprovedWallets && this.displaySpaces && this.displayDisabledWallets && !this.search) {
        return this.wallets.filter(wallet => wallet && wallet.address).slice(0, this.limit);
      } else {
        return this.wallets.filter(wallet => wallet && wallet.address && (this.displayDisapprovedWallets || !wallet.disapproved) && (this.displayUsers || wallet.type !== 'user') && (this.displaySpaces || wallet.type !== 'space') && (this.displayDisabledWallets || wallet.enabled) && (!this.search || wallet.name.toLowerCase().indexOf(this.search.toLowerCase()) >= 0 || wallet.address.toLowerCase().indexOf(this.search.toLowerCase()) >= 0)).slice(0, this.limit);
      }
    },
    filteredWallets() {
      if(this.displayedWallets && this.displayedWallets.length) {
        const lastElement = this.displayedWallets[this.displayedWallets.length - 1];
        const limit = this.wallets.findIndex(wallet => wallet.technicalId === lastElement.technicalId) + 1;
        this.wallets.forEach((wallet, index) => {
          wallet.displayedWallet = index < limit && wallet.address && (this.displayDisapprovedWallets || !wallet.disapproved) && (this.displayUsers || wallet.type !== 'user') && (this.displaySpaces || wallet.type !== 'space') && (this.displayDisabledWallets || wallet.enabled) && (!this.search || wallet.name.toLowerCase().indexOf(this.search.toLowerCase()) >= 0 || wallet.address.toLowerCase().indexOf(this.search.toLowerCase()) >= 0);
        });
        return this.wallets.slice(0, limit);
      } else {
        return [];
      }
    }
  },
  watch: {
    filteredWallets(value, oldValue) {
      if(value.length > 0 && (value.length !== oldValue.length || value[value.length -1].id !== oldValue[oldValue.length -1].id)) {
        return this.retrieveWalletsBalances(this.principalContract, this.wallets);
      }
    },
    loadingWallets(value) {
      this.$emit('loading-wallets-changed', value);
    },
    seeAccountDetails() {
      if (this.seeAccountDetails) {
        $('body').addClass('hide-scroll');
        const thiss = this;
        setTimeout(() => {
          thiss.seeAccountDetailsPermanent = true;
        }, 200);
      } else {
        $('body').removeClass('hide-scroll');
        this.seeAccountDetailsPermanent = false;
      }
    },
  },
  methods: {
    init(appInitialized) {
      if(!appInitialized || this.appInitialized) {
        return;
      }

      this.appInitialized = this.appInitialized || appInitialized;
      if(this.loadingWallets) {
        return;
      }

      const initialFunds = window.walletSettings.initialFunds;
      if (initialFunds && initialFunds.length) {
        const etherInitialFund = initialFunds.find((initialFund) => initialFund.address === 'ether');
        this.etherAmount = (etherInitialFund && etherInitialFund.amount) || 0;
        if (this.principalContract && this.principalContract.address && this.principalContract.address.indexOf('0x') === 0) {
          const tokenInitialFund = initialFunds.find((initialFund) => initialFund.address && initialFund.address.toLowerCase() === this.principalContract.address.toLowerCase());
          this.tokenAmount = (tokenInitialFund && tokenInitialFund.amount) || 0;
        }
      }

      return this.walletUtils.getWallets()
        .then((wallets) => {
          this.wallets = wallets.sort(this.sortByName);
          this.useWalletAdmin = wallets.find(wallet => wallet && wallet.type && wallet.id && wallet.type.toLowerCase() === 'admin' && wallet.name.toLowerCase() === 'admin');
          // *async* approval retrieval
          this.retrieveWalletsApproval(this.principalContract, this.wallets);
        })
        .then(() => {
          this.$emit('wallets-loaded', this.wallets);
        })
        .finally(() => {
          this.loadingWallets = false;
        });
    },
    sortByName(a, b) {
      // To use same Vuetify datable sort algorithm
      const sortA = a.name.toLocaleLowerCase();
      const sortB = b.name.toLocaleLowerCase();
      return (sortA > sortB && 1) || (sortA < sortB && (-1)) || 0; // NOSONAR
    },
    retrieveWalletsApproval(accountDetails, wallets, i) {
      if(!wallets || !wallets.length) {
        return;
      }
      if(!i) {
        i = 0;
      }
      if(i >= wallets.length) {
        return;
      }
      return this.loadWalletApproval(accountDetails, wallets[i])
        .then(() => this.retrieveWalletsApproval(accountDetails, wallets, ++i))
        // Stop loading wallets only when the first call is finished
        .finally(() => this.loadingWallets = !i && true);
    },
    retrieveWalletsBalances(accountDetails, wallets, i) {
      if(!wallets || !wallets.length) {
        return;
      }
      if(!i) {
        i = 0;
      }
      if(i >= wallets.length) {
        return;
      }
      if(i === 0 && this.loadingWallets) {
        return;
      }
      this.loadingWallets = true;
      const wallet = wallets[i];
      let promise = null;
      if (wallet.loadingBalance !== true && wallet.loadingBalance !== false) {
        // Compute only not already computed balances
        promise = this.computeBalance(accountDetails, wallet);
      } else {
        promise = Promise.resolve(null);
      }
      return promise.then(() => this.retrieveWalletsBalances(accountDetails, wallets, ++i))
        // Stop loading wallets only when the first call is finished
        .finally(() => this.loadingWallets = !i && true);
    },
    refreshWallet(wallet) {
      return this.addressRegistry.refreshWallet(wallet)
        .then(() => this.loadWalletInitialization(this.principalContract, wallet))
        .then(() => this.loadWalletApproval(this.principalContract, wallet))
        .then(() => this.computeBalance(this.principalContract, wallet));
    },
    loadWalletApproval(accountDetails, wallet) {
      if(!accountDetails || !accountDetails.contract || !accountDetails.contract || !wallet || !wallet.address) {
        return Promise.resolve(null);
      }
      return accountDetails.contract.methods.isApprovedAccount(wallet.address).call()
        .then((approved) => {
          this.$set(wallet, 'disapproved', !approved);
          this.$forceUpdate();

          if(approved) {
            return accountDetails.contract.methods.getAdminLevel(wallet.address).call();
          }
        })
        .then((adminLevel) => this.$set(wallet, 'adminLevel', adminLevel || 0));
    },
    loadWalletInitialization(accountDetails, wallet) {
      if(!accountDetails || !accountDetails.contract || !accountDetails.contract || !wallet || !wallet.address) {
        return Promise.resolve(null);
      }
      return accountDetails.contract.methods.isInitializedAccount(wallet.address).call()
        .then((initialized) => {
          const oldInitializationState = wallet.initializationState;
          const newInitializationState = initialized ? 'INITIALIZED' : 'MODIFIED';

          if (!initialized && oldInitializationState === 'INITIALIZED') {
            this.changeWalletInitializationStatus(wallet, 'MODIFIED');
            this.$set(wallet, 'initializationState', newInitializationState);
          } else if(initialized && oldInitializationState !== 'INITIALIZED') {
            this.changeWalletInitializationStatus(wallet, 'INITIALIZED');
            this.$set(wallet, 'initializationState', newInitializationState);
          }
        });
    },
    computeBalance(accountDetails, wallet, ignoreUpdateLoadingBalanceParam) {
      if(!wallet.address || !wallet.displayedWallet) {
        return Promise.resolve(null);
      }
      this.$set(wallet, 'loadingBalance', true);
      return this.walletUtils.computeBalance(wallet.address)
        .then((balanceDetails, error) => {
          if (error) {
            this.$set(wallet, 'icon', 'warning');
            this.$set(wallet, 'error', `Error retrieving balance of wallet: ${error}`);
          } else {
            this.$set(wallet, 'balance', balanceDetails && balanceDetails.balance ? balanceDetails.balance : 0);
            this.$set(wallet, 'balanceFiat', balanceDetails && balanceDetails.balanceFiat ? balanceDetails.balanceFiat : 0);
          }
        })
        .then(() => {
          // check if we should reload contract balance too
          if (accountDetails && accountDetails.contract && accountDetails.contract) {
            this.$set(wallet, 'loadingBalancePrincipal', true);
            return accountDetails.contract.methods
              .balanceOf(wallet.address)
              .call()
              .then((balance, error) => {
                if (error) {
                  throw new Error('Invalid contract address');
                }
                balance = String(balance);
                balance = this.walletUtils.convertTokenAmountReceived(balance, accountDetails.decimals);
                this.$set(wallet, 'balancePrincipal', balance);
              })
              .finally(() => {
                if (!ignoreUpdateLoadingBalanceParam) {
                  this.$set(wallet, 'loadingBalancePrincipal', false);
                }
              });
          }
        })
        .catch((error) => {
          this.$set(wallet, 'icon', 'warning');
          this.$set(wallet, 'error', `Error retrieving balance of wallet: ${error}`);
          this.error = String(error);
        })
        .finally(() => {
          if (!ignoreUpdateLoadingBalanceParam) {
            this.$set(wallet, 'loadingBalance', false);
            if (wallet.loadingBalancePrincipal && accountDetails && accountDetails.contract && accountDetails.contract) {
              this.$set(wallet, 'loadingBalancePrincipal', false);
            }
          }
        });
    },
    openAccountDetail(wallet, hash) {
      this.selectedTransactionHash = hash;
      this.selectedWalletAddress = wallet.address;
      this.selectedWallet = wallet;
      this.computeWalletDetails(wallet);
      this.seeAccountDetails = true;

      this.$nextTick(() => {
        const thiss = this;
        $('.v-overlay')
          .off('click')
          .on('click', (event) => {
            thiss.back();
          });
      });
    },
    computeWalletDetails(wallet) {
      if (!this.selectedWalletAddress) {
        this.selectedWalletDetails = null;
        return;
      }
      this.selectedWalletDetails = {
        title: 'ether',
        icon: 'fab fa-ethereum',
        symbol: 'ether',
        address: this.selectedWalletAddress,
        balance: wallet.balance,
        balanceFiat: wallet.balanceFiat,
        details: wallet,
      };
    },
    back() {
      this.seeAccountDetails = false;
      this.seeAccountDetailsPermanent = false;
      this.selectedWalletAddress = null;
      this.selectedWalletDetails = null;
    },
    removeWalletAssociation(wallet) {
      this.error = null;
      return this.walletUtils.removeWalletAssociation(wallet.address)
        .then((result) => {
          if(result) {
            const index = this.wallets.indexOf(wallet);
            if(index >= 0) {
              this.wallets.splice(index, 1);
            }
          } else {
            this.error = `An error occurred while removing wallet of ${wallet.name}`;
          }
        })
        .catch(e => this.error = String(e));
    },
    enableWallet(wallet, enable) {
      this.error = null;
      return this.walletUtils.enableWallet(wallet.address, enable)
        .then((result) => {
          if(result) {
            return this.addressRegistry.refreshWallet(wallet);
          } else {
            this.error = `An error occurred while ${enable ? 'enabling' : 'disabling'} wallet of ${wallet.name}`;
          }
        })
        .catch(e => this.error = String(e));
    },
    walletInitialized(hash) {
      this.error = null;
      if (hash) {
        this.$set(this.walletToProcess, 'pendingTransaction', (this.walletToProcess.pendingTransaction || 0) + 1);
        this.watchWalletTransaction(this.walletToProcess, hash);
      }
    },
    openAcceptInitializationModal(wallet) {
      this.walletToProcess = wallet;
      this.$refs.initAccountModal.open(wallet, window.walletSettings.initialFundsRequestMessage, this.etherAmount, this.tokenAmount);
    },
    openDenyInitializationModal(wallet) {
      this.walletToProcess = wallet;
      this.informationTitle = 'Deny wallet confirmation';
      this.informationMessage = `Would you like to <strong>deny permission</strong> to wallet of ${wallet.type} <strong>${wallet.name}</strong>?`;
      this.hideConfirmActions = false;
      this.confirmAction = 'deny';
      this.$refs.informationModal.open();
    },
    openRemoveWalletModal(wallet) {
      this.walletToProcess = wallet;
      this.informationTitle = 'Delete wallet confirmation';
      this.informationMessage = `Would you like to <strong>delete</strong> wallet of ${wallet.type} <strong>${wallet.name}</strong>?`;
      this.hideConfirmActions = false;
      this.confirmAction = 'remove';
      this.$refs.informationModal.open();
    },
    openDisableWalletModal(wallet) {
      this.walletToProcess = wallet;
      this.informationTitle = 'Disable wallet confirmation';
      this.informationMessage = `Would you like to <strong>disable</strong> wallet of ${wallet.type} <strong>${wallet.name}</strong>?`;
      this.hideConfirmActions = false;
      this.confirmAction = 'disable';
      this.$refs.informationModal.open();
    },
    openApproveModal(wallet) {
      this.walletToProcess = wallet;
      this.informationTitle = 'Approve wallet confirmation';
      this.informationMessage = `Would you like to <strong>approve</strong> wallet of ${wallet.type} <strong>${wallet.name}</strong>?`;
      this.hideConfirmActions = false;
      this.confirmAction = 'approve';
      this.$refs.informationModal.open();
    },
    openDisapproveModal(wallet) {
      if(!wallet) {
        return;
      }
      if (wallet.adminLevel > 0) {
        this.walletToProcess = null;
        this.informationTitle = 'Disapproval denied';
        this.informationMessage = `${wallet.type} <strong>${wallet.name}</strong> is a token administrator, thus the wallet can't be disapproved.`;
        this.hideConfirmActions = true;
        this.confirmAction = null;
        this.$refs.informationModal.open();
      } else {
        this.walletToProcess = wallet;
        this.informationTitle = 'Disapprove wallet confirmation';
        this.informationMessage = `Would you like to <strong>disapprove</strong> wallet of ${wallet.type} <strong>${wallet.name}</strong>?`;
        this.hideConfirmActions = false;
        this.confirmAction = 'disapprove';
        this.$refs.informationModal.open();
      }
    },
    changeWalletInitializationStatus(wallet, status) {
      if(wallet) {
        return this.walletUtils.saveWalletInitializationStatus(wallet.address, status)
          .then(() => {
            return this.refreshWallet(wallet);
          }).catch(e => {
            this.error = String(e);
          });
      }
    },
    processTransaction(wallet, action) {
      this.error = null;

      this.$set(wallet, 'pendingTransaction', (wallet.pendingTransaction || 0) + 1);
      fetch('/portal/rest/wallet/api/admin/transaction', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: $.param({
          action: action,
          address: wallet.address,
        }),
      }).then((resp) => {
        if (resp && resp.ok) {
          return resp.text();
        } else {
          throw new Error(`Error processing action ${  action } on wallet ${  wallet.address}`);
        }
      }).then((hash) => {
        if (hash) {
          this.watchWalletTransaction(wallet, hash);
        }
      }).catch((error) => {
        this.error = String(error);
        this.$set(wallet, 'pendingTransaction', (wallet.pendingTransaction || 1) - 1);
      });
    },
    watchWalletTransaction(wallet, hash) {
      this.refreshWallet(wallet);

      this.walletUtils.watchTransactionStatus(hash, (receipt) => {
        this.$set(wallet, 'pendingTransaction', (wallet.pendingTransaction || 1) - 1);
        this.refreshWallet(wallet);
      });
    },
    sendFunds(wallet) {
      if (this.$refs.sendFundsModal) {
        this.$refs.sendFundsModal.prepareSendForm(wallet.id, wallet.type, this.tokenAmount, this.principalContract && this.principalContract.address);
      }
    },
    proceessAction() {
      if (this.confirmAction === 'approve') {
        this.processTransaction(this.walletToProcess, this.confirmAction);
      } else if (this.confirmAction === 'disapprove') {
        this.processTransaction(this.walletToProcess, this.confirmAction);
      } else if (this.confirmAction === 'remove') {
        this.removeWalletAssociation(this.walletToProcess);
      } else if (this.confirmAction === 'disable') {
        this.enableWallet(this.walletToProcess, false);
      } else if (this.confirmAction === 'deny') {
        this.changeWalletInitializationStatus(this.walletToProcess, 'DENIED');
      } else if (this.confirmAction === 'initialize') {
        this.processTransaction(this.walletToProcess, this.confirmAction);
      }
    },
  },
};
</script>
