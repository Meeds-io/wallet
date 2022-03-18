<template>
  <v-app
    :id="appId"
    color="transaprent"
    class="VuetifyApp"
    flat>
    <main v-if="isWalletEnabled && isWalletActivated" id="walletEnabledContent">
      <v-layout>
        <v-flex>
          <v-app class="mb-4 application-toolbar">
            <v-tabs
              v-model="tab"
              slider-size="4">
              <v-tab>{{ tabName }}</v-tab>
            </v-tabs>
            <v-tabs-items v-model="tab" class="tabs-content">
              <v-tab-item eager>
                <v-flex>
                  <v-layout
                    row
                    wrap
                    class="ms-0 me-0 pr-0">
                    <wallet-reward-summary
                      ref="walletSummary"
                      :wallet="wallet"
                      :is-space="isSpace"
                      :is-space-administrator="isSpaceAdministrator"
                      :initialization-state="initializationState"
                      :contract-details="contractDetails"
                      :fiat-symbol="fiatSymbol"
                      :selected-transaction-hash="selectedTransactionHash"
                      :selected-contract-method-name="selectedContractMethodName"
                      @refresh="init()"
                      @display-transactions="openAccountDetail"
                      @error="error = $event" />
                    <div class="my-8 walletRewardSetup">
                      <wallet-reward-setup
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
                    </div>
                    <template v-if="wallet && contractDetails && this.initializationState !== 'DELETED'">
                      <v-flex class="chartHistory WalletChart mt-6">
                        <wallet-reward-transaction-history-chart-summary
                          v-if="!loading"
                          ref="chartPeriodicityButtons"
                          :periodicity-label="periodicityLabel"
                          @period-changed="periodChanged"
                          @error="error = $event" />
                      </v-flex>
                      <v-flex class="WalletChart transactionHistoryChart mb-4">
                        <wallet-reward-transaction-history-chart
                          ref="transactionHistoryChart"
                          :class="periodicity"
                          :transaction-statistics="transactionStatistics" />
                      </v-flex>
                    </template>
                  </v-layout>
                </v-flex>
              </v-tab-item>
            </v-tabs-items>
          </v-app>
        </v-flex>
      </v-layout>
      <!-- The selected account detail -->
      <wallet-reward-account-detail
        ref="accountDetail"
        :fiat-symbol="fiatSymbol"
        :wallet="wallet"
        :contract-details="selectedAccount"
        :selected-transaction-hash="selectedTransactionHash"
        :selected-contract-method-name="selectedContractMethodName"
        @back="back()" />
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
            class="requestFundsLink">
            {{ $t('exoplatform.wallet.info.walletApplicationDisabledLink') }}
          </a>
        </v-flex>
      </v-layout>
    </main>
    <main v-else-if="isWalletActivated && !loading" id="walletUnactivatedContent">
      <v-layout>
        <v-flex>
          <v-card-title class="transparent" flat>
            <v-spacer />
            <div class="alert alert-warning">
              <i class="uiIconWarning"></i>
              {{ $t('exoplatform.wallet.warning.walletUnactivated') }}
            </div>
            <v-spacer />
          </v-card-title>
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
export default {
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
      isWalletActivated: true,
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
      periodicity: 'year',
      selectedDate: new Date().toISOString().substr(0, 7),
    };
  },
  computed: {
    tabName() {
      return this.isSpace ?  this.$t('exoplatform.wallet.title.spaceWallet')  :  this.$t('exoplatform.wallet.title.myWallet');
    },
    appId() {
      return this.isSpace ? 'SpaceWalletApp' : 'WalletApp';
    },
    walletReadonly() {
      return this.isSpace && !this.isSpaceAdministrator;
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
      return this.displayEtherBalanceTooLow;
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
        .then(() => {
          if (this.$refs.walletSummary) {
            this.$refs.walletSummary.prepareSendForm();
            this.$refs.walletSummary.loadPendingTransactions();
          }
          this.checkOpenTransaction();
          this.$forceUpdate();
        })
        .catch((error) => {
          console.error('An error occurred while on initialization', error);
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
          this.isWalletActivated = this.wallet && this.wallet.active || this.isSpace; // space wallets are always active

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

          this.isReadOnly = this.settings.isReadOnly || !this.wallet;
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
            console.error('init method - error', e);
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
      if (event && event.detail && event.detail.string && this.walletAddress === event.detail.string.toLowerCase()) {
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
        $('.v-overlay').on('click', () => {
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
      if (error) {
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
