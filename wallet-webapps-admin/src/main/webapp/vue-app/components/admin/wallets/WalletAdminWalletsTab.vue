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
    <div v-if="!loading && !useWalletAdmin" class="alert alert-warning v-content">
      <i class="uiIconWarning"></i>
      {{ $t('exoplatform.wallet.warning.adminWalletNotInitialized') }}
    </div>
    <v-container>
      <v-layout>
        <v-flex md3 xs12>
          <v-btn-toggle
            v-model="walletTypes"
            class="walletFilterButtons"
            mandatory
            multiple
            flat>
            <v-btn value="user">
              {{ $t('exoplatform.wallet.label.users') }}
            </v-btn>
            <v-btn value="space">
              {{ $t('exoplatform.wallet.label.spaces') }}
            </v-btn>
            <v-btn value="admin">
              {{ $t('exoplatform.wallet.label.admin') }}
            </v-btn>
          </v-btn-toggle>
        </v-flex>
        <v-flex
          md3
          offset-md1
          xs12>
          <v-btn-toggle
            v-model="walletStatuses"
            class="walletFilterButtons"
            multiple
            flat>
            <v-btn value="disabled">
              {{ $t('exoplatform.wallet.label.disabled') }}
            </v-btn>
            <v-btn value="disapproved">
              {{ $t('exoplatform.wallet.label.disapproved') }}
            </v-btn>
            <v-btn value="deletedIdentity">
              {{ $t('exoplatform.wallet.label.deleted') }}
            </v-btn>
          </v-btn-toggle>
        </v-flex>
        <v-flex
          md3
          offset-md2
          xs12>
          <v-text-field
            v-model="search"
            :label="$t('exoplatform.wallet.label.searchInWalletPlaceholder')"
            append-icon="search"
            class="pt-0" />
        </v-flex>
      </v-layout>
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
                display-no-address
                no-status />
            </td>
            <td class="clickable text-xs-center" @click="openAccountDetail(props.item)">
              <template v-if="!loadingWallets">
                <template v-if="props.item.deletedUser">{{ $t('exoplatform.wallet.label.deletedIdentity') }}</template>
                <template v-else-if="props.item.disabledUser">{{ $t('exoplatform.wallet.label.disabledUser') }}</template>
                <template v-else-if="!props.item.enabled">{{ $t('exoplatform.wallet.label.disabledWallet') }}</template>
                <template v-else-if="props.item.initializationState === 'NEW'">{{ $t('exoplatform.wallet.label.newWallet') }}</template>
                <template v-else-if="props.item.disapproved">{{ $t('exoplatform.wallet.label.disapprovedWallet') }}</template>
                <template v-else-if="Number(props.item.balance) === 0 || (etherAmount && walletUtils.toFixed(props.item.balance) < Number(etherAmount))">
                  <v-icon color="orange">
                    warning
                  </v-icon>
                  {{ $t('exoplatform.wallet.warning.lowEtherAmount') }}
                </template>
                <template v-else-if="Number(props.item.tokenBalance) === 0">
                  <v-icon color="orange">
                    warning
                  </v-icon>
                  {{ $t('exoplatform.wallet.warning.lowTokenAmount', {0: contractDetails && contractDetails.name}) }}
                </template>
                <v-icon
                  v-else
                  :title="$t('exoplatform.wallet.button.ok')"
                  color="green">
                  fa-check-circle
                </v-icon>
              </template>
            </td>
            <td
              v-if="contractDetails"
              class="clickable text-xs-center"
              @click="openAccountDetail(props.item)">
              <v-progress-circular
                v-if="props.item.loadingTokenBalance"
                :title="loadingWallets ? 'Loading balance' : 'A transaction is in progress'"
                color="primary"
                indeterminate
                size="20" />
              <span
                v-else-if="loadingWallets && props.item.loadingTokenBalance !== false"
                :title="$t('exoplatform.wallet.label.loadingBalance')">
                {{ $t('exoplatform.wallet.label.loading') }}...
              </span>
              <template v-else-if="props.item.tokenBalance">
                {{ walletUtils.toFixed(props.item.tokenBalance) }} {{ contractDetails && contractDetails.symbol ? contractDetails.symbol : '' }}
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
                :title="$t('exoplatform.wallet.label.loadingBalance')">
                {{ $t('exoplatform.wallet.label.loading') }}...
              </span>
              <template v-else>
                {{ walletUtils.toFixed(props.item.balance) }} eth
              </template>
            </td>
            <td
              v-if="contractDetails && contractDetails.contractType && contractDetails.contractType > 1"
              class="clickable"
              @click="openAccountDetail(props.item)">
              <template v-if="props.item.type === 'user' || props.item.type === 'space'">
                <v-icon
                  v-if="props.item.initializationState === 'NEW'"
                  :title="$t('exoplatform.wallet.label.newWallet')"
                  color="primary">
                  fa-hands
                </v-icon>
                <v-icon
                  v-else-if="props.item.initializationState === 'MODIFIED'"
                  :title="$t('exoplatform.wallet.label.modifiedWallet')"
                  color="orange">
                  fa-hands
                </v-icon>
                <v-icon
                  v-else-if="props.item.initializationState === 'DENIED'"
                  :title="$t('exoplatform.wallet.label.deniedWallet')"
                  color="orange">
                  fa-times-circle
                </v-icon>
                <v-icon
                  v-else-if="props.item.initializationState === 'PENDING'"
                  :title="$t('exoplatform.wallet.label.initializationInProgressForWallet')"
                  color="primary">
                  fa-dot-circle
                </v-icon>
                <v-icon
                  v-else-if="props.item.initializationState === 'INITIALIZED'"
                  :title="$t('exoplatform.wallet.label.initializedWallet')"
                  color="green">
                  fa-check-circle
                </v-icon>
              </template>
            </td>
            <td class="text-xs-center">
              <v-progress-circular
                v-if="props.item.pendingTransaction"
                :title="$t('exoplatform.wallet.message.transactionInProgress')"
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
                    <v-list-tile-title>{{ $t('exoplatform.wallet.button.refresh') }}</v-list-tile-title>
                  </v-list-tile>
                  <v-divider />

                  <template v-if="props.item.type === 'user' || props.item.type === 'space'">
                    <template v-if="useWalletAdmin">
                      <template v-if="contractDetails && contractDetails.contractType && contractDetails.contractType > 1 && props.item.initializationState === 'NEW' || props.item.initializationState === 'MODIFIED' || props.item.initializationState === 'DENIED'">
                        <v-list-tile @click="openAcceptInitializationModal(props.item)">
                          <v-list-tile-title>{{ $t('exoplatform.wallet.button.initializeWallet') }}</v-list-tile-title>
                        </v-list-tile>
                        <v-list-tile v-if="props.item.initializationState !== 'DENIED'" @click="openDenyInitializationModal(props.item)">
                          <v-list-tile-title>{{ $t('exoplatform.wallet.button.rejectWallet') }}</v-list-tile-title>
                        </v-list-tile>
                        <v-divider />
                      </template>
                      <template v-else-if="Number(props.item.balance) === 0 || (etherAmount && walletUtils.toFixed(props.item.balance) < Number(etherAmount))">
                        <v-list-tile @click="openSendEtherModal(props.item)">
                          <v-list-tile-title>{{ $t('exoplatform.wallet.button.sendEther') }}</v-list-tile-title>
                        </v-list-tile>
                        <v-divider />
                      </template>

                      <v-list-tile v-if="tokenAmount > 0 && props.item.tokenBalance < tokenAmount" @click="openSendTokenModal(props.item)">
                        <v-list-tile-title>{{ $t('exoplatform.wallet.button.sendToken', {0: contractDetails && contractDetails.name}) }}</v-list-tile-title>
                      </v-list-tile>
                      <v-divider />
                    </template>

                    <v-list-tile v-if="props.item.enabled" @click="openDisableWalletModal(props.item)">
                      <v-list-tile-title>{{ $t('exoplatform.wallet.button.disableWallet') }}</v-list-tile-title>
                    </v-list-tile>
                    <v-list-tile v-else-if="!props.item.disabledUser && !props.item.deletedUser" @click="enableWallet(props.item, true)">
                      <v-list-tile-title>{{ $t('exoplatform.wallet.button.enableWallet') }}</v-list-tile-title>
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
        {{ $t('exoplatform.wallet.button.loadMore') }}
      </v-btn>
    </v-flex>

    <initialize-account-modal
      ref="initAccountModal"
      @sent="walletPendingTransaction" />

    <send-token-modal
      ref="sendTokenModal"
      :contract-details="contractDetails"
      @sent="walletPendingTransaction" />
    <send-ether-modal
      ref="sendEtherModal"
      @sent="walletPendingTransaction" />

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
import SendEtherModal from './modals/WalletAdminSendEtherModal.vue';
import SendTokenModal from './modals/WalletAdminSendTokenModal.vue';

export default {
  components: {
    InitializeAccountModal,
    SendEtherModal,
    SendTokenModal,
  },
  props: {
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
    contractDetails: {
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
      walletTypes: ['user', 'admin'],
      walletStatuses: ['disapproved'],
    };
  },
  computed: {
    walletHeaders() {
      return [
        {
          text: '',
          align: 'center',
          sortable: false,
          value: 'avatar',
        },
        {
          text: this.$t('exoplatform.wallet.label.name'),
          align: 'left',
          sortable: true,
          value: 'name',
        },
        {
          text: this.$t('exoplatform.wallet.label.walletStatus'),
          align: 'center',
          sortable: true,
          value: 'walletStatus',
        },
        {
          text: this.$t('exoplatform.wallet.label.tokenBalance', {0: this.contractDetails && this.contractDetails.name}),
          align: 'center',
          value: 'tokenBalance',
        },
        {
          text: this.$t('exoplatform.wallet.label.etherBalance'),
          align: 'center',
          value: 'balance',
        },
        {
          text: this.$t('exoplatform.wallet.label.initializationstatus'),
          align: 'center',
          sortable: true,
          value: 'initializationState',
        },
        {
          text: '',
          align: 'center',
          sortable: false,
          value: '',
        },
      ];
    },
    walletAdmin() {
      return this.wallets && this.wallets.find(wallet => wallet && wallet.type === 'admin');
    },
    useWalletAdmin() {
      return this.walletAdmin && this.walletAdmin.adminLevel >= 2 && this.walletAdmin.balance && Number(this.walletAdmin.balance) >= 0.002 && this.walletAdmin.tokenBalance && Number(this.walletAdmin.tokenBalance) >= 0.02;
    },
    displayUsers() {
      return this.walletTypes && this.walletTypes.includes('user');
    },
    displaySpaces() {
      return this.walletTypes && this.walletTypes.includes('space');
    },
    displayAdmin() {
      return this.walletTypes && this.walletTypes.includes('admin');
    },
    displayDisapprovedWallets() {
      return this.walletStatuses && this.walletStatuses.includes('disapproved');
    },
    displayDisabledWallets() {
      return this.walletStatuses && this.walletStatuses.includes('disabled');
    },
    displayDeletedIdentities() {
      return this.walletStatuses && this.walletStatuses.includes('deletedIdentity');
    },
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
    walletTableHeaders() {
      const walletTableHeaders = this.walletHeaders.slice();
      if (!this.isAdmin) {
        walletTableHeaders.splice(walletTableHeaders.length - 1, 1);
      }
      if (!this.contractDetails) {
        walletTableHeaders.splice(4, 1);
      }
      if (!this.contractDetails || this.contractDetails.contractType < 2) {
        walletTableHeaders.splice(2, 1);
      }
      return walletTableHeaders;
    },
    showLoadMore() {
      return this.displayedWallets.length === this.limit;
    },
    displayedWallets() {
      return this.wallets.filter(this.isDisplayWallet).slice(0, this.limit);
    },
    filteredWallets() {
      if(this.displayedWallets && this.displayedWallets.length) {
        const lastElement = this.displayedWallets[this.displayedWallets.length - 1];
        const limit = this.wallets.findIndex(wallet => wallet.technicalId === lastElement.technicalId) + 1;
        // Set 'displayedWallet' attribute on wallets, used to know whether to retrieve
        // wallet data from blockchain or not
        this.wallets.forEach((wallet, index) => {
          wallet.displayedWallet = index < limit && this.displayedWallets.filter(displayedWallet => displayedWallet.technicalId === wallet.technicalId) && true;
        });
        return this.displayedWallets.slice(0, limit);
      } else {
        return [];
      }
    }
  },
  watch: {
    filteredWallets(value, oldValue) {
      if(value.length > 0 && (value.length !== oldValue.length || value[value.length -1].id !== oldValue[oldValue.length -1].id)) {
        return this.retrieveWalletsBalances(this.wallets);
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

      const initialFunds = window.walletSettings.initialFunds || {};
      this.etherAmount = initialFunds.etherAmount || 0;
      this.tokenAmount = initialFunds.tokenAmount || 0;

      return this.walletUtils.getWallets()
        .then((wallets) => {
          this.wallets = wallets.sort(this.sortByName);
          // *async* approval retrieval
          this.retrieveWalletsApproval(this.wallets);
          return this.$nextTick();
        })
        .then(() => {
          if (this.walletAdmin) {
            return this.refreshWallet(this.walletAdmin);
          }
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
      const sortA = a.name && a.name.toLocaleLowerCase();
      const sortB = b.name && b.name.toLocaleLowerCase();
      return (sortA > sortB && 1) || (sortA < sortB && (-1)) || 0; // NOSONAR
    },
    retrieveWalletsApproval(wallets, i) {
      if(!wallets || !wallets.length) {
        return;
      }
      if(!i) {
        i = 0;
      }
      if(i >= wallets.length) {
        return;
      }
      return this.loadWalletApproval(wallets[i])
        .then(() => this.retrieveWalletsApproval(wallets, ++i))
        // Stop loading wallets only when the first call is finished
        .finally(() => this.loadingWallets = !i && true);
    },
    isDisplayWallet(wallet) {
      return wallet && wallet.address
        && (this.displayUsers || wallet.type !== 'user')
        && (this.displaySpaces || wallet.type !== 'space')
        && (this.displayAdmin || wallet.type !== 'admin')
        && (this.displayDisabledWallets || wallet.enabled)
        && (this.displayDeletedIdentities || !wallet.deletedUser)
        && (this.displayDisapprovedWallets || !wallet.disapproved)
        && (!this.search
            || wallet.initializationState.toLowerCase().indexOf(this.search.toLowerCase()) >= 0
            || wallet.name.toLowerCase().indexOf(this.search.toLowerCase()) >= 0
            || wallet.address.toLowerCase().indexOf(this.search.toLowerCase()) >= 0);
    },
    retrieveWalletsBalances(wallets, i) {
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
        promise = this.computeBalance(wallet);
      } else {
        promise = Promise.resolve(null);
      }
      return promise.then(() => this.retrieveWalletsBalances(wallets, ++i))
        // Stop loading wallets only when the first call is finished
        .finally(() => this.loadingWallets = !i && true);
    },
    refreshWallet(wallet) {
      return this.addressRegistry.refreshWallet(wallet)
        .then(() => this.loadWalletApproval(wallet))
        .then(() => this.loadWalletInitialization(wallet))
        .then(() => this.computeBalance(wallet));
    },
    loadWalletApproval(wallet) {
      if(!this.contractDetails || !this.contractDetails.contract || !this.contractDetails.contract || !wallet || !wallet.address) {
        return Promise.resolve(null);
      }
      return this.contractDetails.contract.methods.isApprovedAccount(wallet.address).call()
        .then((approved) => {
          this.$set(wallet, 'disapproved', !approved);
          this.$forceUpdate();

          if(approved) {
            return this.contractDetails.contract.methods.getAdminLevel(wallet.address).call();
          }
        })
        .then((adminLevel) => this.$set(wallet, 'adminLevel', Number(adminLevel) || 0));
    },
    loadWalletInitialization(wallet) {
      if(!this.contractDetails || !this.contractDetails.contract || !this.contractDetails.contract || !wallet || !wallet.address) {
        return Promise.resolve(null);
      }

      return this.contractDetails.contract.methods.isInitializedAccount(wallet.address).call()
        .then((initialized) => {
          // Consider wallet as initialized if it's approved or initialized
          initialized = initialized || !wallet.disapproved;

          if (initialized && wallet.initializationState !== 'INITIALIZED') {
            this.changeWalletInitializationStatus(wallet, 'INITIALIZED');
            this.$set(wallet, 'initializationState', 'INITIALIZED');
          } else if (!initialized && wallet.initializationState === 'INITIALIZED') {
            this.changeWalletInitializationStatus(wallet, 'MODIFIED');
            this.$set(wallet, 'initializationState', 'MODIFIED');
          }
        });
    },
    computeBalance(wallet, ignoreUpdateLoadingBalanceParam) {
      if(!wallet.address || !wallet.displayedWallet) {
        return Promise.resolve(null);
      }
      this.$set(wallet, 'loadingBalance', true);
      return this.walletUtils.computeBalance(wallet.address)
        .then((balanceDetails, error) => {
          if (error) {
            this.$set(wallet, 'icon', 'warning');
            this.$set(wallet, 'error', this.$t('exoplatform.wallet.error.errorRetrievingBalanceOfWallet', {0: wallet.name, 1: String(error)}));
          } else {
            this.$set(wallet, 'balance', balanceDetails && balanceDetails.balance ? balanceDetails.balance : 0);
            this.$set(wallet, 'balanceFiat', balanceDetails && balanceDetails.balanceFiat ? balanceDetails.balanceFiat : 0);
          }
        })
        .then(() => {
          // check if we should reload contract balance too
          if (this.contractDetails && this.contractDetails.contract) {
            this.$set(wallet, 'loadingTokenBalance', true);
            return this.contractDetails.contract.methods
              .balanceOf(wallet.address)
              .call()
              .then((balance, error) => {
                if (error) {
                  throw new Error(this.$t('exoplatform.wallet.error.invalidContractAddress'));
                }
                balance = String(balance);
                balance = this.walletUtils.convertTokenAmountReceived(balance, this.contractDetails.decimals);
                this.$set(wallet, 'tokenBalance', balance);
              })
              .finally(() => {
                if (!ignoreUpdateLoadingBalanceParam) {
                  this.$set(wallet, 'loadingTokenBalance', false);
                }
              });
          }
        })
        .catch((error) => {
          this.$set(wallet, 'icon', 'warning');
          this.$set(wallet, 'error', this.$t('exoplatform.wallet.error.errorRetrievingBalanceOfWallet', {0: wallet.name, 1: String(error)}));
          this.error = String(error);
        })
        .finally(() => {
          if (!ignoreUpdateLoadingBalanceParam) {
            this.$set(wallet, 'loadingBalance', false);
            if (wallet.loadingTokenBalance && this.contractDetails && this.contractDetails.contract && this.contractDetails.contract) {
              this.$set(wallet, 'loadingTokenBalance', false);
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
    enableWallet(wallet, enable) {
      this.error = null;
      return this.walletUtils.enableWallet(wallet.address, enable)
        .then((result) => {
          if(result) {
            return this.addressRegistry.refreshWallet(wallet);
          } else {
            this.error = (enable && this.$t('exoplatform.wallet.error.errorEnablingWallet', {0: wallet.name})) || this.$t('exoplatform.wallet.error.errorDisablingWallet', {0: wallet.name});
          }
        })
        .catch(e => this.error = String(e));
    },
    walletPendingTransaction(hash) {
      this.error = null;
      if (hash) {
        this.$set(this.walletToProcess, 'pendingTransaction', (this.walletToProcess.pendingTransaction || 0) + 1);
        this.watchWalletTransaction(this.walletToProcess, hash);
      }
    },
    openAcceptInitializationModal(wallet) {
      this.walletToProcess = wallet;
      this.$refs.initAccountModal.open(wallet, window.walletSettings.initialFunds.requestMessage, this.etherAmount, this.tokenAmount);
    },
    openSendTokenModal(wallet) {
      this.walletToProcess = wallet;
      this.$refs.sendTokenModal.open(wallet, window.walletSettings.initialFunds.requestMessage, this.tokenAmount);
    },
    openSendEtherModal(wallet) {
      this.walletToProcess = wallet;
      const etherAmount = this.walletUtils.toFixed(this.etherAmount) - Number(wallet && wallet.balance);
      if (etherAmount && etherAmount > 0 && Number.isFinite(etherAmount)) {
        this.$refs.sendEtherModal.open(wallet, window.walletSettings.initialFunds.requestMessage, etherAmount);
      }
    },
    openDenyInitializationModal(wallet) {
      this.walletToProcess = wallet;
      this.informationTitle = this.$t('exoplatform.wallet.title.rejectWalletInitializationConfirmationModal');
      this.informationMessage = this.$t('exoplatform.wallet.message.rejectWalletInitializationConfirmation', {0: wallet.type, 1: `<strong>${wallet.name}</strong>`});
      this.hideConfirmActions = false;
      this.confirmAction = 'deny';
      this.$refs.informationModal.open();
    },
    openDisableWalletModal(wallet) {
      this.walletToProcess = wallet;
      this.informationTitle = this.$t('exoplatform.wallet.title.disableWalletConfirmationModal');
      this.informationMessage = this.$t('exoplatform.wallet.message.disableWalletConfirmation', {0: wallet.type, 1: `<strong>${wallet.name}</strong>`});
      this.hideConfirmActions = false;
      this.confirmAction = 'disable';
      this.$refs.informationModal.open();
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
    watchWalletTransaction(wallet, hash) {
      this.refreshWallet(wallet);

      this.walletUtils.watchTransactionStatus(hash, (receipt) => {
        this.$set(wallet, 'pendingTransaction', (wallet.pendingTransaction || 1) - 1);
        this.refreshWallet(wallet);
      });
    },
    proceessAction() {
      if (this.confirmAction === 'disable') {
        this.enableWallet(this.walletToProcess, false);
      } else if (this.confirmAction === 'deny') {
        this.changeWalletInitializationStatus(this.walletToProcess, 'DENIED');
      }
    }
  },
};
</script>