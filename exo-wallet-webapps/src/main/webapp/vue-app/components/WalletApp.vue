<template>
  <v-app
    id="WalletApp"
    :class="isMaximized ? 'maximized' : 'minimized'"
    color="transaprent"
    flat>
    <main v-if="isWalletEnabled" id="walletEnabledContent">
      <v-layout>
        <v-flex>
          <v-card :class="isMaximized && 'transparent'" flat>
            <v-toolbar
              :class="isMaximized ? 'mb-3' : 'no-padding'"
              class="walletAppToolbar"
              color="white"
              flat
              dense>
              <v-toolbar-title v-if="isSpace && isMaximized">
                Space Wallet
              </v-toolbar-title>
              <v-toolbar-title v-else-if="isMaximized">
                My Wallet
              </v-toolbar-title>
              <v-toolbar-title v-else class="head-container">
                Wallet
              </v-toolbar-title>
              <div v-if="displayEtherBalanceTooLow" id="etherTooLowWarningParent">
                <v-tooltip
                  content-class="etherTooLowWarning"
                  attach="#etherTooLowWarningParent"
                  absolute
                  top
                  class="ml-2">
                  <v-icon slot="activator" color="orange">
                    warning
                  </v-icon>
                  <span>
                    No enough funds to send transactions
                  </span>
                </v-tooltip>
              </div>

              <v-spacer />

              <wallet-app-menu
                ref="walletAppMenu"
                :is-space="isSpace"
                :is-maximized="isMaximized"
                :is-space-administrator="isSpaceAdministrator"
                @refresh="init()"
                @maximize="maximize()"
                @modify-settings="showSettingsModal = true" />

              <wallet-settings-modal
                ref="walletSettingsModal"
                :is-space="isSpace"
                :open="showSettingsModal"
                :app-loading="loading"
                :fiat-symbol="fiatSymbol"
                :display-reset-option="displayWalletResetOption"
                :accounts-details="accountsDetails"
                :overview-accounts="overviewAccounts"
                :principal-account-address="principalAccount"
                @copied="$refs.walletSetup && $refs.walletSetup.hideBackupMessage()"
                @close="showSettingsModal = false"
                @settings-changed="init()" />
            </v-toolbar>

            <v-toolbar
              class="additionalToolbar"
              color="white"
              flat
              dense>
              <wallet-setup
                ref="walletSetup"
                :is-space="isSpace"
                :wallet-address="walletAddress"
                :refresh-index="refreshIndex"
                :loading="loading"
                :is-minimized="!isMaximized"
                @loading="loading = true"
                @end-loading="loading = false"
                @refresh="init()"
                @error="
                  loading = false;
                  error = $event;
                " />
            </v-toolbar>

            <!-- Body -->
            <v-card
              v-if="displayAccountsList"
              id="walletAppBody"
              class="text-xs-center"
              flat>
              <div v-if="error && !loading" class="alert alert-error">
                <i class="uiIconError"></i> {{ error }}
              </div>

              <v-progress-circular
                v-if="loading"
                color="primary"
                class="mt-4 mb-4"
                indeterminate />

              <wallet-summary
                v-if="walletAddress && !loading && accountsDetails[walletAddress]"
                ref="walletSummary"
                :is-maximized="isMaximized"
                :is-space="isSpace"
                :is-space-administrator="isSpaceAdministrator"
                :accounts-details="accountsDetails"
                :overview-accounts="overviewAccountsToDisplay"
                :principal-account="principalAccount"
                :refresh-index="refreshIndex"
                :network-id="networkId"
                :wallet-address="walletAddress"
                :ether-balance="etherBalance"
                :total-balance="totalBalance"
                :total-fiat-balance="totalFiatBalance"
                :is-read-only="isReadOnly"
                :fiat-symbol="fiatSymbol"
                :loading="loading"
                @refresh-balance="refreshBalance"
                @refresh-token-balance="refreshTokenBalance"
                @error="error = $event" />

              <wallet-accounts-list
                v-if="isMaximized && !loading"
                ref="walletAccountsList"
                :is-read-only="isReadOnly"
                :accounts-details="accountsDetails"
                :principal-account="principalAccount"
                :overview-accounts="overviewAccountsToDisplay"
                :wallet-address="walletAddress"
                :network-id="networkId"
                :refresh-index="refreshIndex"
                :fiat-symbol="fiatSymbol"
                @account-details-selected="openAccountDetail"
                @refresh-contracts="forceUpdate"
                @transaction-sent="$refs && $refs.walletSummary && $refs.walletSummary.loadPendingTransactions()"
                @error="error = $event" />
            </v-card>

            <!-- The selected account detail -->
            <v-navigation-drawer
              id="accountDetailsDrawer"
              v-model="seeAccountDetails"
              fixed
              right
              stateless
              temporary
              width="700"
              max-width="100vw">
              <account-detail
                ref="accountDetail"
                :fiat-symbol="fiatSymbol"
                :is-read-only="isReadOnly"
                :network-id="networkId"
                :wallet-address="walletAddress"
                :contract-details="selectedAccount"
                :selected-transaction-hash="selectedTransactionHash"
                @transaction-sent="$refs && $refs.walletSummary && $refs.walletSummary.loadPendingTransactions()"
                @back="back()" />
            </v-navigation-drawer>
          </v-card>
        </v-flex>
      </v-layout>
      <div id="walletDialogsParent">
      </div>
    </main>
    <main v-else-if="isMaximized && !loading" id="walletDisabledContent">
      <v-layout>
        <v-flex>
          <v-card-title class="transparent" flat>
            <v-spacer />
            <div class="alert alert-warning">
              <i class="uiIconWarning"></i> You don't have enough access rights to use Ethereum Wallet application
            </div>
            <v-spacer />
          </v-card-title>
        </v-flex>
      </v-layout>
    </main>
  </v-app>
</template>

<script>
import WalletAppMenu from './WalletAppMenu.vue';
import WalletSetup from './WalletSetup.vue';
import WalletSummary from './WalletSummary.vue';
import WalletAccountsList from './WalletAccountsList.vue';
import AccountDetail from './AccountDetail.vue';
import WalletSettingsModal from './WalletSettingsModal.vue';

import * as constants from '../js/Constants.js';
import {getContractsDetails, retrieveContractDetails} from '../js/TokenUtils.js';
import {initWeb3, initSettings, computeBalance, etherToFiat, gasToEther} from '../js/WalletUtils.js';

export default {
  components: {
    WalletAppMenu,
    WalletSummary,
    WalletSetup,
    WalletAccountsList,
    AccountDetail,
    WalletSettingsModal,
  },
  props: {
    isSpace: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
  },
  data() {
    return {
      isWalletEnabled: false,
      loading: true,
      useMetamask: false,
      isReadOnly: true,
      isSpaceAdministrator: false,
      seeAccountDetails: false,
      seeAccountDetailsPermanent: false,
      overviewAccounts: [],
      overviewAccountsToDisplay: [],
      principalAccount: null,
      showSettingsModal: false,
      gasPriceInEther: null,
      networkId: null,
      browserWalletExists: false,
      walletAddress: null,
      selectedTransactionHash: null,
      selectedAccount: null,
      fiatSymbol: '$',
      accountsDetails: {},
      refreshIndex: 1,
      error: null,
    };
  },
  computed: {
    displayAccountsList() {
      return this.walletAddress;
    },
    displayWalletResetOption() {
      return !this.loading && !this.error && this.walletAddress && !this.useMetamask && this.browserWalletExists;
    },
    displayEtherBalanceTooLow() {
      return !this.loading && !this.error && (!this.isSpace || this.isSpaceAdministrator) && this.walletAddress && !this.isReadOnly && this.etherBalance < gasToEther(window.walletSettings.userPreferences.defaultGas, this.gasPriceInEther);
    },
    etherBalance() {
      if (this.refreshIndex > 0 && this.walletAddress && this.accountsDetails && this.accountsDetails[this.walletAddress]) {
        let balance = this.accountsDetails[this.walletAddress].balance;
        balance = balance ? Number(balance) : 0;
        return balance;
      }
      return 0;
    },
    totalFiatBalance() {
      return Number(etherToFiat(this.totalBalance));
    },
    totalBalance() {
      let balance = 0;
      if (this.refreshIndex > 0 && this.walletAddress && this.accountsDetails) {
        Object.keys(this.accountsDetails).forEach((key) => {
          const accountDetail = this.accountsDetails[key];
          balance += Number((accountDetail.isContract ? accountDetail.balanceInEther : accountDetail.balance) || 0);
        });
      }
      return balance;
    },
  },
  watch: {
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
  created() {
    if ((!eXo && eXo.env) || !eXo.env.portal || !eXo.env.portal.userName || !eXo.env.portal.userName.length) {
      this.isWalletEnabled = false;
      return;
    }

    if (eXo.env.portal.profileOwner && eXo.env.portal.profileOwner !== eXo.env.portal.userName) {
      this.isWalletEnabled = false;
      return;
    }

    if (this.isSpace && !(window.walletSpaceGroup && window.walletSpaceGroup.length)) {
      this.isWalletEnabled = false;
      return;
    }

    const thiss = this;

    $(document).on('keydown', (event) => {
      if (event.which === 27 && thiss.seeAccountDetailsPermanent && !$('.v-dialog:visible').length) {
        thiss.back();
      }
    });

    this.$nextTick(() => {
      // Init application
      this.init()
        .then((result, error) => {
          if (this.$refs.walletSummary) {
            this.$refs.walletSummary.init(this.isReadOnly);
          }
          if (this.$refs.walletAccountsList) {
            this.$refs.walletAccountsList.checkOpenTransaction();
          }
          this.forceUpdate();
        })
        .catch((error) => {
          console.debug('An error occurred while on initialization', error);

          if (this.useMetamask) {
            this.error = `You can't send transaction because Metamask is disconnected`;
          } else {
            this.error = `You can't send transaction because your wallet is disconnected`;
          }
        });
    });
  },
  methods: {
    init() {
      this.loading = true;
      this.error = null;
      this.seeAccountDetails = false;
      this.selectedAccount = null;
      this.accountsDetails = {};
      this.walletAddress = null;

      return initSettings(this.isSpace)
        .then((result, error) => {
          this.handleError(error);
          if (!window.walletSettings || !window.walletSettings.isWalletEnabled) {
            this.isWalletEnabled = false;
            this.forceUpdate();
            throw new Error('Wallet disabled for current user');
          } else {
            this.isWalletEnabled = true;
            this.initMenuApp();
            this.useMetamask = window.walletSettings.userPreferences.useMetamask;
            this.isSpaceAdministrator = window.walletSettings.isSpaceAdministrator;
            if (window.walletSettings.userPreferences.walletAddress || this.useMetamask) {
              this.forceUpdate();
            } else {
              throw new Error(constants.ERROR_WALLET_NOT_CONFIGURED);
            }
          }
        })
        .then((result, error) => {
          this.handleError(error);
          return initWeb3(this.isSpace);
        })
        .then((result, error) => {
          this.handleError(error);
          this.networkId = window.walletSettings.currentNetworkId;
          this.walletAddress = window.localWeb3.eth.defaultAccount.toLowerCase();

          this.isReadOnly = window.walletSettings.isReadOnly;
          this.browserWalletExists = window.walletSettings.browserWalletExists;
          this.overviewAccounts = window.walletSettings.userPreferences.overviewAccounts || [];
          this.overviewAccountsToDisplay = window.walletSettings.userPreferences.overviewAccountsToDisplay;

          this.principalAccount = window.walletSettings.defaultPrincipalAccount;
          this.fiatSymbol = window.walletSettings ? window.walletSettings.fiatSymbol : '$';
          this.gasPriceInEther = this.gasPriceInEther || window.localWeb3.utils.fromWei(String(window.walletSettings.normalGasPrice), 'ether');

          if (window.walletSettings.maxGasPrice) {
            window.walletSettings.maxGasPriceEther = window.walletSettings.maxGasPriceEther || window.localWeb3.utils.fromWei(String(window.walletSettings.maxGasPrice), 'ether').toString();
          }

          return this.refreshBalance();
        })
        .then((result, error) => {
          this.handleError(error);
          return this.reloadContracts();
        })
        .then((result, error) => {
          this.handleError(error);
          this.loading = false;
          this.forceUpdate();
        })
        .then(() => this.$refs.walletSetup && this.$refs.walletSetup.init())
        .catch((e) => {
          console.debug('init method - error', e);
          const error = `${e}`;

          if (error.indexOf(constants.ERROR_WALLET_NOT_CONFIGURED) >= 0) {
            if (!this.useMetamask) {
              this.browserWalletExists = window.walletSettings.browserWalletExists = false;
              this.walletAddress = null;
            }
          } else if (error.indexOf(constants.ERROR_WALLET_SETTINGS_NOT_LOADED) >= 0) {
            this.error = 'Failed to load user settings';
          } else if (error.indexOf(constants.ERROR_WALLET_DISCONNECTED) >= 0) {
            this.error = 'Failed to connect to network';
          } else {
            this.error = error;
          }
          this.loading = false;
          this.forceUpdate();
        });
    },
    forceUpdate() {
      this.refreshIndex++;
      this.$forceUpdate();
    },
    refreshBalance() {
      const walletAddress = String(this.walletAddress);
      return computeBalance(walletAddress)
        .then((balanceDetails, error) => {
          if (error) {
            this.$set(this.accountsDetails, walletAddress, {
              title: 'ether',
              icon: 'warning',
              balance: '0',
              symbol: 'ether',
              isContract: false,
              address: walletAddress,
              error: `Error retrieving balance of wallet: ${error}`,
            });
            this.forceUpdate();
            this.handleError(error);
          }
          const accountDetails = {
            title: 'ether',
            icon: 'fab fa-ethereum',
            symbol: 'ether',
            isContract: false,
            address: walletAddress,
            balance: balanceDetails && balanceDetails.balance ? balanceDetails.balance : '0',
            balanceFiat: balanceDetails && balanceDetails.balanceFiat ? balanceDetails.balanceFiat : '0',
          };
          this.$set(this.accountsDetails, walletAddress, accountDetails);
          this.forceUpdate();
          return accountDetails;
        })
        .catch((e) => {
          console.debug('refreshBalance method - error', e);
          this.$set(this.accountsDetails, walletAddress, {
            title: 'ether',
            icon: 'warning',
            balance: 0,
            symbol: 'ether',
            isContract: false,
            address: walletAddress,
            error: `Error retrieving balance of wallet ${e}`,
          });
          throw e;
        });
    },
    refreshTokenBalance(accountDetail) {
      if (accountDetail) {
        return retrieveContractDetails(this.walletAddress, accountDetail, false).then(() => this.forceUpdate());
      }
    },
    reloadContracts() {
      return getContractsDetails(this.walletAddress, this.networkId, false, false)
        .then((contractsDetails, error) => {
          this.handleError(error);
          if (contractsDetails && contractsDetails.length) {
            contractsDetails.forEach((contractDetails) => {
              if (contractDetails && contractDetails.address) {
                if (this.accountsDetails[this.walletAddress]) {
                  contractDetails.etherBalance = this.accountsDetails[this.walletAddress].balance;
                }
                this.$set(this.accountsDetails, contractDetails.address, contractDetails);
              }
            });
            this.forceUpdate();
          }
        });
    },
    openAccountDetail(accountDetails, hash) {
      if(!accountDetails) {
        console.error(`Can't open empty account details`);
        return;
      }
      if (!accountDetails.error) {
        this.selectedAccount = accountDetails;
        this.selectedTransactionHash = hash;
      }
      this.seeAccountDetails = true;

      this.$nextTick(() => {
        const thiss = this;
        $('.v-overlay').on('click', (event) => {
          thiss.back();
        });
      });
    },
    back() {
      this.seeAccountDetails = false;
      this.seeAccountDetailsPermanent = false;
      this.selectedAccount = null;
    },
    maximize() {
      window.location.href = `${eXo.env.portal.context}/${eXo.env.portal.portalName}/wallet`;
    },
    handleError(error) {
      if(error) {
        throw error;
      }
    },
    initMenuApp() {
      if (!this.isWalletEnabled || this.isSpace) {
        return;
      }
      this.$nextTick(() => {
        if ($('#myWalletTad').length) {
          return;
        }
        if (!$('.userNavigation .item').length) {
          setTimeout(this.initMenuApp, 500);
          return;
        }
        $('.userNavigation').append(` \
          <li id='myWalletTad' class='item${this.isMaximized ? ' active' : ''}'> \
            <a href='${eXo.env.portal.context}/${eXo.env.portal.portalName}/wallet'> \
              <div class='uiIconAppWallet uiIconDefaultApp' /> \
              <span class='tabName'>My Wallet</span> \
            </a> \
          </li>`);
        $(window).trigger('resize');
      });
    },
  },
};
</script>
