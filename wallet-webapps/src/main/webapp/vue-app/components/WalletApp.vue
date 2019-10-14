<template>
  <v-app
    :id="appId"
    color="transaprent"
    class="VuetifyApp"
    flat>
    <main v-if="isWalletEnabled" id="walletEnabledContent">
      <v-layout>
        <v-flex>
          <v-card flat class="transparent">
            <v-toolbar
              class="walletAppToolbar mb-3"
              color="white"
              flat
              dense>
              <v-toolbar-title v-if="isSpace">
                {{ $t('exoplatform.wallet.title.spaceWallet') }}
              </v-toolbar-title>
              <v-toolbar-title v-else>
                {{ $t('exoplatform.wallet.title.myWallet') }}
              </v-toolbar-title>
              <div
                v-if="displayWarnings"
                id="etherTooLowWarningParent"
                class="ml-2">
                <v-icon :title="$t('exoplatform.wallet.warning.noEnoughFunds')" color="orange">
                  warning
                </v-icon>
                <v-icon
                  v-if="displayDisapprovedWallet"
                  slot="activator"
                  :title="$t('exoplatform.wallet.warning.yourWalletIsDisparroved')"
                  color="orange">
                  warning
                </v-icon>
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
                :wallet="wallet"
                :app-loading="loading"
                :display-reset-option="displayWalletResetOption"
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
                :wallet="wallet"
                :initialization-state="initializationState"
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
                <v-flex :class="!walletReadonly && 'md8'" xs12>
                  <v-layout
                    row
                    wrap
                    class="ml-0 mr-0 px-0">
                    <v-flex xs12>
                      <wallet-summary
                        v-if="wallet && contractDetails"
                        ref="walletSummary"
                        :wallet="wallet"
                        :is-space="isSpace"
                        :is-space-administrator="isSpaceAdministrator"
                        :initialization-state="initializationState"
                        :contract-details="contractDetails"
                        @refresh="init()"
                        @display-transactions="openAccountDetail"
                        @error="error = $event" />
                    </v-flex>
                    <template v-if="initializationState !== 'DENIED'">
                      <v-flex xs12>
                        <transaction-history-chart-summary
                          v-if="!loading"
                          ref="chartPeriodicityButtons"
                          :periodicity-label="periodicityLabel"
                          @period-changed="periodChanged"
                          @error="error = $event" />
                      </v-flex>
                      <v-flex xs12>
                        <transaction-history-chart
                          ref="transactionHistoryChart"
                          class="transactionHistoryChart"
                          :transaction-statistics="transactionStatistics" />
                      </v-flex>
                    </template>
                  </v-layout>
                </v-flex>
                <v-flex
                  v-if="!walletReadonly"
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
                    :wallet="wallet"
                    :is-read-only="isReadOnly"
                    @display-transactions="openAccountDetail"
                    @transaction-sent="newPendingTransaction"
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
                :wallet="wallet"
                :contract-details="selectedAccount"
                :selected-transaction-hash="selectedTransactionHash"
                :selected-contract-method-name="selectedContractMethodName"
                @back="back()" />
            </v-navigation-drawer>
          </v-card>
        </v-flex>
      </v-layout>
      <div id="walletDialogsParent">
      </div>
    </main>
    <main v-else-if="!isApplicationEnabled" id="walletDisabledContent">
      <v-layout wrap class="mt-7">
        <v-flex class="mx-auto text-center title" xs12>
          {{ $t('exoplatform.wallet.info.walletApplicationDisabledPart1') }}
        </v-flex>
        <v-flex class="mt-2 mx-auto text-center title" xs12>
          {{ $t('exoplatform.wallet.info.walletApplicationDisabledPart2') }}
        </v-flex>
        <v-flex class="mx-auto text-center title mt-7" xs12>
          <a
            href="https://www.exoplatform.com/rewarding-program"
            target="_blank"
            rel="noopener noreferrer"
            class="no-wrap requestFundsLink">
            {{ $t('exoplatform.wallet.info.walletApplicationDisabledLink') }}
          </a>
        </v-flex>
      </v-layout>
    </main>
    <main v-else-if="!loading" id="walletDisabledContent">
      <v-layout>
        <v-flex>
          <v-card-title class="transparent" flat>
            <v-spacer />
            <div class="alert alert-warning">
              <i class="uiIconWarning"></i>
              {{ $t('exoplatform.wallet.warning.noEnoughPrivileges') }}
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
      isApplicationEnabled: true,
      loading: true,
      disabledYear: true,
      disabledMonth: false,
      isReadOnly: true,
      isSpaceAdministrator: false,
      seeAccountDetails: false,
      seeAccountDetailsPermanent: false,
      showSettingsModal: false,
      gasPriceInEther: null,
      browserWalletExists: false,
      wallet: null,
      selectedTransactionHash: null,
      selectedContractMethodName: null,
      contractDetails: null,
      selectedAccount: null,
      initializationState: 'NONE',
      fiatSymbol: '$',
      settings: null,
      error: null,
      transactionStatistics: null,
      periodicity: 'month',
      selectedDate: new Date().toISOString().substr(0, 7),
    };
  },
  computed: {
    appId() {
      return this.isSpace ? 'SpaceWalletApp' : 'WalletApp';
    },
    walletReadonly() {
      return this.initializationState === 'DENIED' || (this.isSpace && !this.isSpaceAdministrator);
    },
    walletAddress() {
      return this.wallet && this.wallet.address;
    },
    displayAccountsList() {
      return this.walletAddress;
    },
    displayWalletResetOption() {
      return !this.loading && !this.error && this.walletAddress && this.browserWalletExists;
    },
    displayWarnings() {
      return this.displayDisapprovedWallet || this.displayEtherBalanceTooLow;
    },
    displayDisapprovedWallet() {
      return this.wallet && this.wallet.isInitialized && !this.wallet.isApproved
    },
    displayEtherBalanceTooLow() {
      return this.browserWalletExists
          && !this.loading
          && !this.error
          && (!this.isSpace || this.isSpaceAdministrator)
          && (!this.etherBalance || this.etherBalance < this.walletUtils.gasToEther(this.settings.network.gasLimit, this.gasPriceInEther));
    },
    etherBalance() {
      return this.wallet && this.wallet.etherBalance || 0;
    },
    periodicityLabel() {
      return this.transactionStatistics && this.transactionStatistics.periodicityLabel;
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

    document.addEventListener('exo.wallet.modified', this.walletUpdated);
    document.addEventListener('exo.contract.modified', this.reloadContract);

    this.$nextTick(() => {
      // Init application
      this.init()
        .then((result, error) => {
          if (this.$refs.walletSummaryActions) {
            this.$refs.walletSummaryActions.init(this.isReadOnly);
          }
          if (this.$refs && this.$refs.walletSummary) {
            this.$refs.walletSummary.loadPendingTransactions();
          }
          this.checkOpenTransaction();
          this.$forceUpdate();
        })
        .catch((error) => {
          console.debug('An error occurred while on initialization', error);
          this.error = this.$t('exoplatform.wallet.warning.walletDisconnected');
        });
    });
  },
  methods: {
    init() {
      this.loading = true;
      this.error = null;
      this.seeAccountDetails = false;
      this.selectedAccount = null;
      this.wallet = null;

      return this.walletUtils.initSettings(this.isSpace, true)
        .then((result, error) => {
          this.handleError(error);
          this.settings = window.walletSettings || {wallet: {}, network: {}};
          this.wallet = this.settings.wallet;
          this.isApplicationEnabled = this.settings.enabled;

          if (!this.settings.walletEnabled || !this.isApplicationEnabled) {
            this.isWalletEnabled = false;
            this.$forceUpdate();
            throw new Error(this.$t('exoplatform.wallet.warning.walletDisconnected'));
          } else {
            this.isWalletEnabled = true;
            this.isSpaceAdministrator = this.settings.wallet.spaceAdministrator;

            if (this.settings.wallet.address) {
              this.$forceUpdate();
            } else {
              throw new Error(this.constants.ERROR_WALLET_NOT_CONFIGURED);
            }
          }
        })
        .then((result, error) => {
          this.handleError(error);
          if (this.isReadOnly || !window.localWeb3) {
            return this.walletUtils.initWeb3(this.isSpace);
          }
        })
        .then((result, error) => {
          this.handleError(error);
          if (this.contractDetails) {
            return this.reloadContract();
          } else {
            this.contractDetails = this.tokenUtils.getContractDetails(this.walletAddress);
          }
        })
        .then((result, error) => {
          this.handleError(error);

          this.isReadOnly = this.settings.isReadOnly || !this.wallet || !this.wallet.isApproved;
          this.browserWalletExists = this.settings.browserWalletExists;
          this.initializationState = this.settings.wallet.initializationState;
          this.fiatSymbol = this.settings.fiatSymbol || '$';
          this.gasPriceInEther = this.gasPriceInEther || window.localWeb3.utils.fromWei(String(this.settings.network.minGasPrice), 'ether');
          return this.$nextTick();
        })
        .then(() => this.periodChanged(this.periodicity))
        .catch((e) => {
          const error = `${e}`;

          if (error.indexOf(this.constants.ERROR_WALLET_NOT_CONFIGURED) >= 0) {
            this.browserWalletExists = this.settings.browserWalletExists = false;
          } else if (error.indexOf(this.constants.ERROR_WALLET_SETTINGS_NOT_LOADED) >= 0) {
            this.error = this.$t('exoplatform.wallet.warning.walletInitializationFailure');
          } else if (error.indexOf(this.constants.ERROR_WALLET_DISCONNECTED) >= 0) {
            this.error = this.$t('exoplatform.wallet.warning.networkConnectionFailure');
          } else {
            console.debug('init method - error', e);
            this.error = error;
          }
        })
        .then(() => this.$refs.walletSetup && this.$refs.walletSetup.init())
        .then(this.$nextTick)
        .finally(() => {
          this.loading = false;
          this.$forceUpdate();
        });
    },
    walletUpdated(event) {
      if(event && event.detail && event.detail.string && this.walletAddress === event.detail.string.toLowerCase()) {
        this.addressRegistry.refreshWallet(this.wallet);
      }
    },
    reloadContract() {
      return this.tokenUtils.reloadContractDetails(this.walletAddress).then(result => this.contractDetails = result);
    },
    openAccountDetail(methodName, hash) {
      this.error = null;
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
    newPendingTransaction(transaction) {
      if (this.$refs && this.$refs.walletSummary) {
        this.$refs.walletSummary.loadPendingTransactions();
      }
      this.walletUtils.watchTransactionStatus(transaction.hash, () => {
        this.reloadContract().then(this.$refs.transactionHistoryChart.initializeChart);
      });
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
        if (this.walletAddress && this.contractDetails && parameters && parameters.hash) {
          this.openAccountDetail(null, parameters.hash);
        }
      }
    },
    periodChanged(periodicity, selectedDate) {
      this.periodicity = periodicity;
      this.selectedDate = selectedDate;

      if (this.$refs.transactionHistoryChart) {
        this.transactionUtils.getTransactionsAmounts(this.walletAddress, this.periodicity, this.selectedDate)
        .then((transactionsData) => {
          this.transactionStatistics = transactionsData;
        })
        .catch(e => this.error = String(e));
      }
    },
  },
};
</script>
