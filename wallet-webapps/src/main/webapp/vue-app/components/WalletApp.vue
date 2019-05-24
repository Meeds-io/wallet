<template>
  <div>
    <v-flex xs12>
      <v-card class="cardstyle">
        <v-card-title primary-title>
          <div
            slot="header"
            class="panel-title walletheader">
           <span style="padding:5px;"> My Wallet </span>
              <v-flex md9 />
         <wallet-app-menu/>
          </div>
        </v-card-title>
      </v-card>
    </v-flex>


    <v-flex xs12>
      <v-card class="cardbody">
          <div class="btndisplay">
          <div class="carddisplay">
        <v-card class="cardbalance">
          <div >
            <span class="headlinecard">
              Current balance
            </span>
            <span class="headlinefound " >
              75.00 Ȼ
            </span>
          </div>
        </v-card>

          <v-card class="cardbalance">
              <div class="cardcontent">
            <span class="headlinecardearnedcauris ">
             Total Earned Cauris
            </span>
                  <span class="headlinecauris " >
              152 Ȼ
                   <v-icon color="blue" size="28px" class="arrow-up-right">
                     zoom_in
                  </v-icon>
            </span>
              </div>
          </v-card>

          <v-card class="cardbalance">
              <div class="cardcontent" >
            <span class="headlinecard ">
              Last Transactions
            </span>
                  <span class="headlinecauris " >
              152 Ȼ

                  <v-icon  color="blue" size="28px" class="arrow-up-right">
                     zoom_in
                  </v-icon>
                  </span>
              </div>
          </v-card>
              </div>


              <v-flex md2/>
              <div class="btndisplayblock">
              <v-flex md2>
                  <v-btn class="btn">

                      Send
                  </v-btn>
              </v-flex>

              <v-flex md2>
                  <v-btn class="btn ">

                      Request
                  </v-btn>
              </v-flex>
             </div>
      </div>

   <div class="history">

            <span style="font-weight: 600; padding:8px;">HISTORY</span>

            <v-flex md2/>

    <div class="centerBlock">
      <v-toolbar class="uiGrayLightBox">

        <v-tabs right v-model="selectedTab" class="tabColor">


            <v-tab key="invited" href="#invited" color="transparent">Last Week</v-tab>
            <v-tab key="notYetInvited" href="#notYetInvited">Last month </v-tab>

        </v-tabs>
      </v-toolbar>
    </div>
   </div>
          <v-flex md2/>


   <chart-app></chart-app>

      </v-card>
    </v-flex>
  </div>
</template>


<script>
import WalletAppMenu from './WalletAppMenu.vue';
import ChartApp from './ChartApp.vue';

export default {
  components: {
    WalletAppMenu,
    ChartApp
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

      value: [
        423,
        446,
        675,
        510,
        590,
        610,
        760
      ],
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
      return !this.loading && !this.error && (!this.isSpace || this.isSpaceAdministrator) && this.walletAddress && !this.isReadOnly && this.etherBalance < this.walletUtils.gasToEther(window.walletSettings.userPreferences.defaultGas, this.gasPriceInEther);
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
      return Number(this.walletUtils.etherToFiat(this.totalBalance));
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

      return this.walletUtils.initSettings(this.isSpace)
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

          if (error.indexOf(this.constants.ERROR_WALLET_NOT_CONFIGURED) >= 0) {
            if (!this.useMetamask) {
              this.browserWalletExists = window.walletSettings.browserWalletExists = false;
              this.walletAddress = null;
            }
          } else if (error.indexOf(this.constants.ERROR_WALLET_SETTINGS_NOT_LOADED) >= 0) {
            this.error = 'Failed to load user settings';
          } else if (error.indexOf(this.constants.ERROR_WALLET_DISCONNECTED) >= 0) {
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
      return this.walletUtils.computeBalance(walletAddress)
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
        return this.tokenUtils.retrieveContractDetails(this.walletAddress, accountDetail, false).then(() => this.forceUpdate());
      }
    },
    reloadContracts() {
      return this.tokenUtils.getContractsDetails(this.walletAddress, this.networkId, false, false)
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





<style>
body{
font-family:"Source Sans Pro";

}

@import url('https://fonts.googleapis.com/css?family=Source+Sans+Pro:300,400,600,700,700i');

.theme--light.application {
  background: transparent !important;
  color: transparent !important;
  border: none;
  border-radius: 0 !important;
  box-shadow: none !important;
  z-index: 21;
  visibility: inherit;
  width: 100%;
  margin: 0 !important;
}
.panel {
  transform: rotate(0deg);
  box-sizing: border-box;
  padding: 10px;
  margin: 10px;
  background: rgb(255, 255, 255);
  left: 302px;
    border: 1px none rgb(0, 0, 0);
    border-radius: 2px;
    box-shadow: 0 1px 5px 1px rgba(102, 97, 91, 0.15);
}


.theme--light.v-expansion-panel .v-expansion-panel__container{
    border: 1px none rgb(0, 0, 0);
    border-radius: 2px !important;
    box-shadow: 0 1px 5px 1px rgba(102, 97, 91, 0.15);
}


.panel-title {
  font-weight: bold;
}

.container {
    flex: 1 1 100%;
    margin: auto;
    padding: 0px!important;
    width: 100%;
}

 .container.fill-height, .v-expansion-panel {
    width: 96% !important;
    margin: 20px auto;
    }





    .v-expansion-panel__body .container.fill-height {
        align-items: center;
        display: flex;
        margin: 0;
        padding: 15px;
         width: 96% !important;
        max-width: 96% !important;
    }

    v-expansion-panel theme--light{
    padding:0;
    }
    ul.v-expansion-panel:first-of-type {
        border: 1px none rgb(0, 0, 0);
        border-radius: 2px;
        box-shadow: 0 1px 5px 1px rgba(102, 97, 91, 0.15);
    }


ul.v-expansion-panel:first-of-type {
     border: 0.5px none rgb(0, 0, 0);
     box-shadow: none;


}


i.uiIconWikiWiki.uiIconWikiWhite {
    padding: 10px;
}
.v-card.theme--light {
       border: 1px solid #c6c6c6!important;
}
.v-toolbar theme--light{
height: 55px;
}
.v-toolbar__content{
height: 35px!important;
}
.v-toolbar__content, .v-toolbar__extension {
    align-items: center;
    display: flex;
    padding: 0 7px!important;
}
.panel {
    transform: rotate(0deg);
    box-sizing: border-box;
    border-radius: 3px!important;

}

 .v-expansion-panel__body .v-card {
    box-shadow: 0 2px 0px -1px rgba(0,0,0,.2), 0 4px 5px 0 rgba(0,0,0,.14), 0 1px 10px 0 rgba(0,0,0,.12)!important;
    border-radius: 3px!important;}


    li.v-expansion-panel__container.panel {
        height: 60px !important;
    }
    li.v-expansion-panel__container.panel.v-expansion-panel__container--active {
        height: auto !important;
    }
    .v-expansion-panel__header {
        padding: 0 !important;
    }
    .container.panel.fluid.fill-height {
        height: 100px;
        padding:10px;

        height: 100px;
    }

    .v-card {
    display: block;
    border-radius: 2px;
    width: 97%!important;

    position: relative;
    text-decoration: none;
    transition: .3s cubic-bezier(.25,.8,.5,1);
    box-shadow: 0 2px 1px -1px rgba(0,0,0,.2), 0 1px 1px 0 rgba(0,0,0,.14), 0 1px 3px 0 rgba(0,0,0,.12);
}
.v-card.theme--light {
    border: 1px solid #c6c6c6!important;
    margin-left: 20px!important;
}
.v-card__title {
    align-items: center;
    display: block!important;
    flex-wrap: wrap;
    padding: 16px;
}
.dispostion_flex_1{
    display:flex;
    padding: 40px;
}
.dispostion_flex{
    display:flex;
}
.v-btn__content {
    align-items: center;
    border-radius: inherit;
    display: flex;
    font-weight: 500;
    flex: 1 0 auto;
    justify-content: center;
    margin: 0 auto;
    position: relative;
    transition: .3s cubic-bezier(.25,.8,.5,1);
    white-space: nowrap;
    width: 200px!important;
}

.decalage {
    margin-left: 40px;
}

.v-icon {
    align-items: center;
    display: inline-flex;
    -webkit-font-feature-settings: "liga";
    font-feature-settings: "liga";

    justify-content: center;
    line-height: 1;
    font-weight: 600;
    transition: .3s cubic-bezier(.25,.8,.5,1);
    vertical-align: text-bottom;
}
.flex {
    flex: 1 1 auto;
    padding: 10px!important;
}

.walletheader{
    display: inline-flex;
    width: 97%;
font-weight: 600;
color: rgb(0, 0, 0);
margin:0 15px;
text-align: -webkit-auto;
font-size: 18px;

}
.walletbalance{
text-align: -webkit-center;
}
.v-icon .v-icon v-icon--right material-icons theme--light{
font-size: 20px!important;

}
.v-btn .v-icon--right {
    margin-left: auto;
}
.btn.btn-primary, .btn-primary {
    font-family: Helvetica, arial, sans-serif;
    font-weight: normal;
    color: #ffffff;
    border: none;
    box-shadow: none;
    padding: 0px 10px!important;
}
.v-btn .v-icon--right {
    margin-left: 0px!important;
}
.theme--light.v-btn:not(.v-btn--icon):not(.v-btn--flat) {
   background-color: none!important;
}
.v-btn--icon {
    border: none;

}
.v-card__title--primary {
    padding-top: 10px!important;
}
.v-btn {
    margin:0px!important;

}
.flex.md9 {

    max-width: 79%!important;
}
.cardstyle.v-card.theme--light {
    height: 50px;
}

.headlinecard {
    font-size: 20px!important;

    padding: 20px;
    line-height: 30px!important;
}

.headlinecardearnedcauris {
    font-size: 20px!important;

    padding: 13px;
    line-height: 30px!important;
}

.cardbalance.v-card.theme--light {
    height: 75px;
   box-shadow: 0 2px 1px -1px rgba(0,0,0,.2), 0 0px 1px 0 rgba(0,0,0,.14), 0 1px 14px 3px rgba(0,0,0,.12);
    width: 180px!important;
        padding: 6px;
}






.cardbody{
padding-top: 20px;

}
.headlinefound{

font-size: 40px!important;
    font-weight: 700;
    padding: 20px;
    color: blue;
    line-height: 35px!important;
}

.carddisplay {
    display: inline-flex;
    padding: 15px;
}

.headlinecauris {

   font-size: 28px!important;
   font-weight: 600;
    line-height: 30px!important;
}

.cardcontent {
    display: inline-block;
    text-align: -webkit-center;
}

.btndisplay {
    display: inline-flex;
}
.v-btn:not(.v-btn--depressed):not(.v-btn--flat):active {
    box-shadow: none!important;}

 .theme--light.v-btn:not(.v-btn--icon):not(.v-btn--flat) {
    background-color: #ffffff!important;
}

.history {
       margin-top: 25px;

}

.btndisplayinlineblock {
    display: inline-flex;
}
.v-toolbar {
    transition: none;
    box-shadow: 0 2px 4px -1px rgba(0,0,0,.2), 0 4px 5px 0 rgba(0,0,0,.14), 0 1px 10px 0 rgba(0,0,0,.12);
    position: relative;
    width: 24%!important;
    will-change: padding-left,padding-right;
}
.theme--light.v-toolbar {
    background-color: #ffffff!important;
    color: rgba(0,0,0,.87);
}
.centerBlock {
    text-align: -webkit-center;

}

</style>