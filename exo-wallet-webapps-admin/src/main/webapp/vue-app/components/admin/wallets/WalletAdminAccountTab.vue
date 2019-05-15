<template>
  <v-flex v-if="principalContract && principalContract.contractType >= 2" flat>
    <confirm-dialog
      ref="deleteAdminWalletConfirm"
      message="Would you like to delete admin wallet?"
      title="Delete admin wallet confirmation"
      ok-label="Delete"
      cancel-label="Cancel"
      @ok="removeAdminWallet()"
      @closed="useAdminWallet = true" />
    <div v-if="error" class="alert alert-error v-content">
      <i class="uiIconError"></i>{{ error }}
    </div>
    <v-container>
      <v-layout>
        <v-flex class="text-xs-left">
          <v-switch
            v-model="useAdminWallet"
            :disabled="creatingWallet || removingWallet || loading"
            label="Use admin wallet to administrate wallets" />
        </v-flex>
      </v-layout>
      <v-layout>
        <v-flex v-if="creatingWallet">
          <v-progress-circular
            color="primary"
            indeterminate
            size="20" />
          Creating admin wallet...
        </v-flex>
        <v-flex v-else-if="removingWallet">
          <v-progress-circular
            color="primary"
            indeterminate
            size="20" />
          Removing admin wallet...
        </v-flex>
        <v-flex v-else-if="loading">
          <v-progress-circular
            color="primary"
            indeterminate
            size="20" />
          Loading admin wallet...
        </v-flex>
        <v-flex v-else-if="useAdminWallet && !adminWalletExists" class="text-xs-left">
          <v-radio-group v-model="adminWalletCreationAction">
            <v-radio
              value="CREATE"
              label="Create new admin wallet" />
            <v-flex v-if="adminWalletCreationAction === 'CREATE'" class="mb-2">
              <button class="btn btn-primary" @click="createAdminWallet()">
                Create wallet
              </button>
            </v-flex>
            <v-radio
              value="IMPORT"
              label="Import admin wallet private key" />
            <v-layout v-if="adminWalletCreationAction === 'IMPORT'" class="mb-2">
              <v-flex xs6>
                <v-form ref="adminWalletImportForm">
                  <v-text-field
                    v-if="adminWalletCreationAction === 'IMPORT'"
                    v-model="walletPrivateKey"
                    :append-icon="walletPrivateKeyShow ? 'visibility_off' : 'visibility'"
                    :rules="[rules.priv]"
                    :type="walletPrivateKeyShow ? 'text' : 'password'"
                    :disabled="loading"
                    name="walletPrivateKey"
                    placeholder="Enter wallet admin private key"
                    autocomplete="off"
                    autofocus
                    required
                    @click:append="walletPrivateKeyShow = !walletPrivateKeyShow" />
                </v-form>
              </v-flex>
              <v-flex xs6 class="text-xs-left">
                <button class="btn btn-primary mt-2 ml-2" @click="importAminWallet()">
                  Import wallet
                </button>
              </v-flex>
            </v-layout>
          </v-radio-group>
        </v-flex>
      </v-layout>
      <v-layout v-if="adminWalletExists">
        <v-flex md6 xs12>
          <v-list>
            <v-subheader>Admin account properties</v-subheader>
            <v-list-tile>
              <v-list-tile-title>
                Address
              </v-list-tile-title>
              <v-list-tile-title>
                {{ adminWallet.address }}
              </v-list-tile-title>
            </v-list-tile>
            <v-list-tile>
              <v-list-tile-title>
                Admin level
                <warning-bubble v-if="adminWallet.level < 4">
                  <template slot="bubble-content">
                    Admin wallet should having admin level 4 at least on token {{ principalContract.name }}
                  </template>
                  <template slot="content">
                    Admin wallet should having admin level 4 at least on token {{ principalContract.name }}
                  </template>
                </warning-bubble>
              </v-list-tile-title>
              <v-list-tile-title>
                {{ adminWallet.level }}
              </v-list-tile-title>
            </v-list-tile>
            <v-list-tile>
              <v-list-tile-title>
                Token balance
                <warning-bubble v-if="adminWallet.balanceToken < 100">
                  <template slot="bubble-content">
                    No enough funds to manage wallets
                  </template>
                  <template slot="content">
                    No enough funds to manage wallets
                  </template>
                </warning-bubble>
              </v-list-tile-title>
              <v-list-tile-title>
                {{ adminWallet.balanceToken }} {{ principalContract.symbol }}
              </v-list-tile-title>
            </v-list-tile>
            <v-list-tile>
              <v-list-tile-title>
                Ether balance
                <warning-bubble v-if="adminWallet.balanceEther < 0.05">
                  <template slot="bubble-content">
                    No enough funds to manage wallets
                  </template>
                  <template slot="content">
                    No enough funds to manage wallets
                  </template>
                </warning-bubble>
              </v-list-tile-title>
              <v-list-tile-title :title="`${adminWallet.balanceEther} ether / ${adminWallet.balanceFiat} ${symbol}`">
                {{ adminWallet.balanceEther }} ether / {{ adminWallet.balanceFiat }} {{ symbol }}
              </v-list-tile-title>
            </v-list-tile>
            <v-list-tile>
              <v-flex v-if="pendingTransaction">
                <v-progress-circular
                  color="primary"
                  indeterminate
                  size="20" />
                Transaction in progress...
              </v-flex>
            </v-list-tile>
          </v-list>
        </v-flex>
      </v-layout>
    </v-container>
  </v-flex>
  <v-flex v-else>
    <div class="alert alert-warning v-content">
      <i class="uiIconWarning"></i>
      No token or Type ERT Token V2 is added as principal contract
    </div>
  </v-flex>
</template>

<script>

export default {
  props: {
    networkId: {
      type: String,
      default: function() {
        return null;
      },
    },
    symbol: {
      type: String,
      default: function() {
        return '$';
      },
    },
    walletAddress: {
      type: String,
      default: function() {
        return null;
      },
    },
    principalContract: {
      type: Object,
      default: function() {
        return {};
      },
    },
  },
  data() {
    return {
      error: null,
      useAdminWallet: false,
      adminWallet: null,
      adminWalletExists: false,
      walletPrivateKey: '',
      walletPrivateKeyShow: false,
      pendingTransaction: false,
      loading: false,
      creatingWallet: false,
      removingWallet: false,
      adminWalletCreationAction: 'CREATE',
      rules: {
        priv: (v) => (v && (v.length === 66 || v.length === 64)) || 'Exactly 64 or 66 (with "0x") characters are required',
      },
    };
  },
  watch: {
    principalContract() {
      if (this.principalContract) {
        this.init();
      }
    },
    adminWalletCreationAction() {
      this.walletPrivateKey = '';
      this.walletPrivateKeyShow = false;
    },
    useAdminWallet() {
      if (!this.useAdminWallet && this.adminWalletExists) {
        this.$refs.deleteAdminWalletConfirm.open();
      }
    },
  },
  methods: {
    init() {
      this.error = null;
      if (!this.principalContract || this.principalContract.contractType < 2) {
        return;
      }
      this.loading = true;
      return this.addressRegistry.searchWalletByTypeAndId('admin', 'ADMIN')
        .then((wallet) => {
          this.adminWallet = wallet;
          this.useAdminWallet = this.adminWalletExists = !!(wallet && wallet.address);
          if (this.useAdminWallet) {
            const loadAdminLevelPromise = this.principalContract.contract.methods.getAdminLevel(this.adminWallet.address).call()
              .then((level) => {
                this.$set(this.adminWallet, 'level', level);
              });

            const loadApprovalPromise = this.principalContract.contract.methods.isApprovedAccount(this.adminWallet.address).call()
              .then((approved) => {
                this.$set(this.adminWallet, 'approved', approved);
              });

            const loadAdminTokenBalance = this.principalContract.contract.methods.balanceOf(this.adminWallet.address).call()
              .then((balance) => {
                this.$set(this.adminWallet, 'balanceToken', this.walletUtils.convertTokenAmountReceived(balance, this.principalContract.decimals));
              });

            const loadAdminEtherBalance = this.walletUtils.computeBalance(this.adminWallet.address)
              .then((balanceDetails) => {
                if (balanceDetails) {
                  this.$set(this.adminWallet, 'balanceEther', this.walletUtils.toFixed(balanceDetails.balance));
                  this.$set(this.adminWallet, 'balanceFiat', this.walletUtils.toFixed(balanceDetails.balanceFiat));
                } else {
                  this.$set(this.adminWallet, 'balanceEther', 0);
                  this.$set(this.adminWallet, 'balanceFiat', 0);
                }
              });
            return Promise.all([loadAdminLevelPromise, loadApprovalPromise, loadAdminTokenBalance, loadAdminEtherBalance]);
          }
        }).catch((error) => {
          this.error = String(error);
        })
        .finally(() => {
          this.loading = false;
        });
    },
    removeAdminWallet() {
      this.error = null;
      this.removingWallet = true;
      return fetch('/portal/rest/wallet/api/account/removeAdminWallet', {
        method: 'GET',
        credentials: 'include',
      }).then((resp) => {
        if (!resp || !resp.ok) {
          throw new Error('Error deleting admin wallet from server');
        }
        return this.init(); 
      }).catch((error) => {
        this.error = String(error);
      })
      .finally(() => {
        this.removingWallet = false;
      });
    },
    createAdminWallet(privateKey) {
      this.error = null;
      this.creatingWallet = true;
      return fetch('/portal/rest/wallet/api/account/createAdminAccount', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: $.param({
          privateKey: privateKey || '',
        }),
      }).then((resp) => {
        if (!resp || !resp.ok) {
          throw new Error('Error creating admin wallet on server');
        }
        return this.init(); 
      }).catch((error) => {
        this.error = String(error);
      })
      .finally(() => {
        this.creatingWallet = false;
      });
    },
    importAminWallet() {
      if (!this.$refs.adminWalletImportForm.validate()) {
        return;
      }
      return this.createAdminWallet(this.walletPrivateKey);
    },
  },
};
</script>
