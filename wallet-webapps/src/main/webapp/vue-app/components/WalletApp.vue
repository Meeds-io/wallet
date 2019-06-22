<template>
  <v-app
    id="WalletApp"
    color="transaprent"
    flat>
    <main v-if="isWalletEnabled" id="walletEnabledContent">
      <v-layout>
        <v-flex>
          <v-card transparent flat>
            <v-toolbar
              class="walletAppToolbar mb-3"
              color="white"
              flat
              dense>
              <v-toolbar-title v-if="isSpace">
                Space Wallet
              </v-toolbar-title>
              <v-toolbar-title v-else>
                My Wallet
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

              <toolbar-menu
                ref="walletAppMenu"
                :is-space="isSpace"
                :is-space-administrator="isSpaceAdministrator"
                @refresh="init()"
                @modify-settings="showSettingsModal = true" />

              <settings-modal
                ref="walletSettingsModal"
                :is-space="isSpace"
                :open="showSettingsModal"
                :app-loading="loading"
                :display-reset-option="displayWalletResetOption"
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
              flat>
              <div v-if="error && !loading" class="alert alert-error">
                <i class="uiIconError"></i> {{ error }}
              </div>

              <v-layout
                row
                wrap
                class="ml-0 mr-0">
                <v-flex :class="(!isSpace || isSpaceAdministrator) && 'md8'" xs12>
                  <v-layout
                    row
                    wrap
                    class="ml-0 mr-0">
                    <v-flex xs12>
                      <wallet-summary
                        v-if="walletAddress && contractDetails"
                        ref="walletSummary"
                        :wallet-address="walletAddress"
                        :contract-details="contractDetails"
                        @display-transactions="openAccountDetail"
                        @refresh-token-balance="refreshTokenBalance"
                        @error="error = $event" />
                    </v-flex>
                    <v-flex xs12>
                      <transaction-history-chart-summary
                        v-if="!loading"
                        ref="chartPeriodicityButtons"
                        :periodicity-label="periodicityLabel"
                        @periodicity-changed="periodicity = $event"
                        @error="error = $event" />
                    </v-flex>
                    <v-flex xs12>
                      <transaction-history-chart
                        ref="transactionHistoryChart"
                        class="transactionHistoryChart"
                        :periodicity="periodicity"
                        :wallet-address="walletAddress"
                        :contract-details="contractDetails"
                        @periodicity-label="periodicityLabel = $event"
                        @error="error = $event" />
                    </v-flex>
                  </v-layout>
                </v-flex>
                <v-flex
                  v-if="!isSpace || isSpaceAdministrator"
                  md4
                  xs12
                  text-md-center
                  mt-1>
                  <summary-buttons
                    v-if="walletAddress && !loading && contractDetails"
                    ref="walletSummaryActions"
                    :is-space="isSpace"
                    :is-space-administrator="isSpaceAdministrator"
                    :contract-details="contractDetails"
                    :wallet-address="walletAddress"
                    :is-read-only="isReadOnly"
                    @display-transactions="openAccountDetail"
                    @refresh-token-balance="refreshTokenBalance"
                    @transaction-sent="$refs && $refs.walletSummary && $refs.walletSummary.loadPendingTransactions()"
                    @error="error = $event" />
                </v-flex>
              </v-layout>
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
                :wallet-address="walletAddress"
                :contract-details="selectedAccount"
                :selected-transaction-hash="selectedTransactionHash"
                :selected-contract-method-name="selectedContractMethodName"
                @transaction-sent="$refs && $refs.walletSummary && $refs.walletSummary.loadPendingTransactions()"
                @back="back()" />
            </v-navigation-drawer>
          </v-card>
        </v-flex>
      </v-layout>
      <div id="walletDialogsParent">
      </div>
    </main>
    <main v-else-if="!loading" id="walletDisabledContent">
      <v-layout>
        <v-flex>
          <v-card-title class="transparent" flat>
            <v-spacer />
            <div class="alert alert-warning">
              <i class="uiIconWarning"></i> You don't have enough privileges to use Wallet application.
            </div>
            <v-spacer />
          </v-card-title>
        </v-flex>
      </v-layout>
    </main>
  </v-app>
</template>

<script>
import ToolbarMenu from './wallet-app/ToolbarMenu.vue';
import WalletSummary from './wallet-app/Summary.vue';
import SummaryButtons from './wallet-app/SummaryButtons.vue';
import SettingsModal from './wallet-app/SettingsModal.vue';
import TransactionHistoryChart from './wallet-app/TransactionHistoryChart.vue';
import TransactionHistoryChartSummary from './wallet-app/TransactionHistoryChartSummary.vue';

export default {
  components: {
    ToolbarMenu,
    WalletSummary,
    SettingsModal,
    SummaryButtons,
    TransactionHistoryChart,
    TransactionHistoryChartSummary,
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
      disabledYear: true,
      disabledMonth: false,
      isReadOnly: true,
      isSpaceAdministrator: false,
      seeAccountDetails: false,
      seeAccountDetailsPermanent: false,
      periodicityLabel: null,
      showSettingsModal: false,
      gasPriceInEther: null,
      browserWalletExists: false,
      walletAddress: null,
      selectedTransactionHash: null,
      selectedContractMethodName: null,
      selectedAccount: null,
      fiatSymbol: '$',
      refreshIndex: 1,
      error: null,
      periodicity: 'month',
    };
  },
  computed: {
    displayAccountsList() {
      return this.walletAddress;
    },
    displayWalletResetOption() {
      return !this.loading && !this.error && this.walletAddress && this.browserWalletExists;
    },
    displayEtherBalanceTooLow() {
      return !this.loading && !this.error && (!this.isSpace || this.isSpaceAdministrator) && this.walletAddress && !this.isReadOnly && this.etherBalance < this.walletUtils.gasToEther(window.walletSettings.network.gasLimit, this.gasPriceInEther);
    },
    etherBalance() {
      return this.contractDetails && this.contractDetails.etherBalance || 0;
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
            this.$refs.walletSummary.init();
          }
          if (this.$refs.walletSummaryActions) {
            this.$refs.walletSummaryActions.init(this.isReadOnly);
          }
          if (this.$refs.walletAccountsList) {
            this.$refs.walletAccountsList.checkOpenTransaction();
          }
          this.forceUpdate();
        })
        .catch((error) => {
          console.debug('An error occurred while on initialization', error);
          this.error = `You can't send transaction because your wallet is disconnected`;
        });
    });
  },
  methods: {
    init() {
      this.loading = true;
      this.error = null;
      this.seeAccountDetails = false;
      this.selectedAccount = null;
      this.walletAddress = null;

      return this.walletUtils.initSettings(this.isSpace)
        .then((result, error) => {
          this.handleError(error);
          if (!window.walletSettings || !window.walletSettings.walletEnabled) {
            this.isWalletEnabled = false;
            this.forceUpdate();
            throw new Error('Wallet disabled for current user');
          } else {
            this.isWalletEnabled = true;
            this.initMenuApp();
            this.isSpaceAdministrator = window.walletSettings.wallet.isSpaceAdministrator;
            if (window.walletSettings.wallet.address) {
              this.forceUpdate();
            } else {
              throw new Error(this.constants.ERROR_WALLET_NOT_CONFIGURED);
            }
          }
        })
        .then((result, error) => {
          this.handleError(error);
          return this.walletUtils.initWeb3(this.isSpace);
        })
        .then((result, error) => {
          this.handleError(error);
          this.walletAddress = window.localWeb3.eth.defaultAccount.toLowerCase();

          this.isReadOnly = window.walletSettings.isReadOnly;
          this.browserWalletExists = window.walletSettings.browserWalletExists;

          this.fiatSymbol = window.walletSettings ? window.walletSettings.fiatSymbol : '$';
          this.gasPriceInEther = this.gasPriceInEther || window.localWeb3.utils.fromWei(String(window.walletSettings.network.normalGasPrice), 'ether');

          if (window.walletSettings.network.maxGasPrice) {
            window.walletSettings.network.maxGasPriceEther = window.walletSettings.network.maxGasPriceEther || window.localWeb3.utils.fromWei(String(window.walletSettings.network.maxGasPrice), 'ether').toString();
          }
        })
        .then((result, error) => {
          this.handleError(error);
          return this.reloadContract();
        })
        .then((result, error) => {
          this.handleError(error);
          this.forceUpdate();
        })
        .then(() => this.$refs.walletSetup && this.$refs.walletSetup.init())
        .then(() => this.$nextTick())
        .then(() => this.$refs.transactionHistoryChart && this.$refs.transactionHistoryChart.initializeChart())
        .catch((e) => {
          console.debug('init method - error', e);
          const error = `${e}`;

          if (error.indexOf(this.constants.ERROR_WALLET_NOT_CONFIGURED) >= 0) {
            this.browserWalletExists = window.walletSettings.browserWalletExists = false;
            this.walletAddress = null;
          } else if (error.indexOf(this.constants.ERROR_WALLET_SETTINGS_NOT_LOADED) >= 0) {
            this.error = 'Failed to load user settings';
          } else if (error.indexOf(this.constants.ERROR_WALLET_DISCONNECTED) >= 0) {
            this.error = 'Failed to connect to network';
          } else {
            this.error = error;
          }
        })
      .finally(() => {
        this.loading = false;
        this.forceUpdate();
      });
    },
    forceUpdate() {
      this.refreshIndex++;
      this.$forceUpdate();
    },
    refreshTokenBalance() {
      return this.tokenUtils.refreshTokenBalance(this.walletAddress, this.contractDetails);
    },
    reloadContract() {
      return this.tokenUtils.getContractDetails(this.walletAddress, false, false)
        .then((contractDetails, error) => {
          this.handleError(error);
          this.contractDetails = contractDetails;
        });
    },
    openAccountDetail(accountDetails, hash, methodName) {
      this.error = null;
      if(!accountDetails) {
        console.error(`Can't open empty account details`);
        return;
      }
      if (accountDetails.error) {
        this.error = 'Error displaying transactions list';
      } else {
        this.selectedAccount = this.contractDetails;
        this.selectedTransactionHash = hash;
        this.selectedContractMethodName = methodName;
        this.seeAccountDetails = true;

        this.$nextTick(() => {
          const thiss = this;
          $('.v-overlay').on('click', (event) => {
            thiss.back();
          });
        });
      }
    },
    back() {
      this.seeAccountDetails = false;
      this.seeAccountDetailsPermanent = false;
      this.selectedAccount = null;
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
          <li id='myWalletTad' class='item active'> \
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
