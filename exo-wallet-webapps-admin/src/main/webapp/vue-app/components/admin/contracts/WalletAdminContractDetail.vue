<template>
  <v-flex
    v-if="contractDetails && contractDetails.title"
    id="accountDetail"
    class="text-xs-center white layout column">
    <v-card-title class="align-start accountDetailSummary">
      <v-layout column>
        <v-flex id="accountDetailTitle" class="mt-3">
          <div class="headline title align-start">
            <v-icon class="primary--text accountDetailIcon">
              {{ contractDetails.icon }}
            </v-icon>
            Contract Details: {{ contractDetails.title }}
          </div>
          <h3 v-if="contractDetails.contractBalanceFiat" class="font-weight-light">
            Contract balance: {{ toFixed(contractDetails.contractBalanceFiat) }} {{ fiatSymbol }} / {{ toFixed(contractDetails.contractBalance) }} ether
          </h3> <h4 v-if="contractDetails.owner" class="grey--text font-weight-light no-wrap">
            Owner: <wallet-address :value="contractDetails.owner" display-label />
          </h4> <h4 v-if="contractDetails.totalSupply" class="grey--text font-weight-light">
            Total supply: {{ toFixed(totalSupply) }} {{ contractDetails && contractDetails.symbol }}
          </h4>
        </v-flex>

        <v-flex v-if="!isDisplayOnly" id="accountDetailActions">
          <!-- Send ether -->
          <send-ether-modal
            v-if="contractDetails.isOwner"
            :account="walletAddress"
            :balance="contractDetails.balance"
            :recipient="contractDetails.address"
            use-navigation
            @success="successSendingEther"
            @sent="newTransactionPending"
            @error="transactionError" />

          <!-- add/remove admin -->
          <contract-admin-modal
            v-if="contractDetails.adminLevel >= 5"
            ref="addAdminModal"
            :contract-details="contractDetails"
            :wallet-address="walletAddress"
            method-name="addAdmin"
            title="Add administrator"
            autocomplete-label="Administrator account"
            autocomplete-placeholder="Choose an administrator account to add"
            input-label="Habilitation level"
            input-placeholder="Choose a value between 1 and 5"
            @sent="newTransactionPending"
            @success="successTransaction"
            @error="transactionError">
            <div class="alert alert-info">
              <i class="uiIconInfo"></i>Habilitation levels:
              <ul>
                <li>
                  <strong>
                    Level 2
                  </strong>: send rewards of wallets
                </li>
                <li>
                  <strong>
                    Level 3
                  </strong>: change vesting amount for wallets
                </li>
                <li>
                  <strong>
                    Level 4
                  </strong>: approve/disapprove and send initial funds for wallets
                </li>
                <li>
                  <strong>
                    Level 5
                  </strong>: set sell price, manage administrators, pause/unpause contract and send ether to contract.
                </li>
                <li>
                  <strong>
                    Owner
                  </strong>: Upgrade contract, transfer ownership and send funds to Cauri contract.
                </li>
              </ul>
            </div>
          </contract-admin-modal>
          <contract-admin-modal
            v-if="contractDetails.adminLevel >= 5"
            ref="removeAdminModal"
            :contract-details="contractDetails"
            :wallet-address="walletAddress"
            method-name="removeAdmin"
            title="Remove administrator"
            autocomplete-label="Administrator account"
            autocomplete-placeholder="Choose an administrator account to remove"
            @sent="newTransactionPending"
            @success="successTransaction"
            @error="transactionError" />

          <!-- approve/disapprove account -->
          <contract-admin-modal
            v-if="contractDetails.adminLevel >= 4"
            ref="approveAccountModal"
            :contract-details="contractDetails"
            :wallet-address="walletAddress"
            method-name="approveAccount"
            title="Approve account"
            autocomplete-label="Account"
            autocomplete-placeholder="Choose a user or space to approve"
            @sent="newTransactionPending"
            @success="successTransaction"
            @error="transactionError" />
          <contract-admin-modal
            v-if="contractDetails.adminLevel >= 4"
            ref="disapproveAccountModal"
            :contract-details="contractDetails"
            :wallet-address="walletAddress"
            method-name="disapproveAccount"
            title="Disapprove account"
            autocomplete-label="Account"
            autocomplete-placeholder="Choose a user or space to disapprove"
            @sent="newTransactionPending"
            @success="successTransaction"
            @error="transactionError" />

          <!-- pause/unpause contract -->
          <contract-admin-modal
            v-if="!contractDetails.isPaused && contractDetails.adminLevel >= 5"
            ref="pauseModal"
            :contract-details="contractDetails"
            :wallet-address="walletAddress"
            method-name="pause"
            title="Pause contract"
            @sent="newTransactionPending"
            @success="successTransaction"
            @error="transactionError" />
          <contract-admin-modal
            v-if="contractDetails.isPaused && contractDetails.adminLevel >= 5"
            ref="unPauseModal"
            :contract-details="contractDetails"
            :wallet-address="walletAddress"
            method-name="unPause"
            title="Unpause contract"
            @sent="newTransactionPending"
            @success="successTransaction"
            @error="transactionError" />

          <!-- set sell price -->
          <contract-admin-modal
            v-if="contractDetails.adminLevel >= 5"
            ref="setSellPriceModal"
            :contract-details="contractDetails"
            :wallet-address="walletAddress"
            method-name="setSellPrice"
            title="Set sell price"
            input-label="Token sell price"
            input-placeholder="Token sell price in ether"
            convert-wei
            @sent="newTransactionPending"
            @success="successTransaction"
            @error="transactionError" />

          <contract-admin-modal
            v-if="contractDetails.isOwner"
            ref="transferOwnership"
            :contract-details="contractDetails"
            :wallet-address="walletAddress"
            method-name="transferOwnership"
            title="Transfer ownership"
            autocomplete-label="New owner"
            autocomplete-placeholder="Choose a new owner of the contract"
            @sent="newTransactionPending"
            @success="successTransaction"
            @error="transactionError" />

          <upgrade-token-modal
            v-if="contractDetails.isOwner && contractDetails.contractType === 1"
            ref="upgrade"
            :contract-details="contractDetails"
            :wallet-address="walletAddress"
            @sent="newTransactionPending"
            @success="successTransaction"
            @error="transactionError" />
        </v-flex>
        <v-btn
          icon
          class="rightIcon"
          @click="$emit('back')">
          <v-icon>
            close
          </v-icon>
        </v-btn>
      </v-layout>
    </v-card-title>

    <v-tabs
      v-if="contractDetails.contractType > 0"
      v-model="selectedTab"
      grow>
      <v-tabs-slider color="primary" />
      <v-tab key="transactions">
        Transactions{{ totalTransactionsCount ? ` (${totalTransactionsCount})` : '' }}
      </v-tab>
      <v-tab v-if="contractDetails.contractType > 0" key="approvedAccounts">
        Approved accounts
      </v-tab>
      <v-tab v-if="contractDetails.contractType > 0" key="adminAccounts">
        Admin accounts
      </v-tab>
    </v-tabs>
    <v-tabs-items v-if="contractDetails.contractType > 0" v-model="selectedTab">
      <v-tab-item key="transactions">
        <transactions-list
          id="transactionsList"
          ref="transactionsList"
          :network-id="networkId"
          :account="contractDetails.address"
          :contract-details="contractDetails"
          :fiat-symbol="fiatSymbol"
          :error="error"
          display-full-transaction
          administration
          @loaded="computeTransactionsCount"
          @error="error = $event" />
      </v-tab-item>
      <v-tab-item v-if="contractDetails.contractType > 0" key="approvedAccountsTable">
        <v-flex v-if="!loading" justify-center>
          <v-btn
            :loading="loadingApprovedWalletsFromContract"
            color="primary"
            flat
            @click="loadApprovedWalletsFromContract">
            Load all from contract
          </v-btn>
        </v-flex>
        <v-data-table
          v-if="contractDetails"
          :items="approvedWallets"
          :loading="loadingApprovedWalletsFromContract"
          no-data-text=""
          hide-actions
          hide-headers>
          <template slot="items" slot-scope="props">
            <tr v-if="(props.item.approved && props.item.approved[contractDetails.address] === 'approved') || (props.item.accountAdminLevel && props.item.accountAdminLevel[contractDetails.address] != 'not admin' && props.item.accountAdminLevel[contractDetails.address] >= 1)">
              <td>
                <v-avatar size="36px">
                  <img :src="props.item.avatar ? props.item.avatar : '/eXoSkin/skin/images/system/UserAvtDefault.png'" onerror="this.src = '/eXoSkin/skin/images/system/UserAvtDefault.png'">
                </v-avatar>
              </td>
              <td>
                <profile-chip
                  :address="props.item.address"
                  :profile-id="props.item.id"
                  :profile-technical-id="props.item.technicalId"
                  :space-id="props.item.spaceId"
                  :profile-type="props.item.type"
                  :display-name="props.item.name"
                  :enabled="props.item.enabled"
                  :deleted-user="props.item.deletedUser"
                  :disabled-user="props.item.disabledUser"
                  :avatar="props.item.avatar" />
              </td>
              <td v-if="$refs.disapproveAccountModal">
                <span v-if="props.item.accountAdminLevel && props.item.accountAdminLevel[contractDetails.address] != 'not admin' && props.item.accountAdminLevel[contractDetails.address] >= 1">
                  Admin level: {{ props.item.accountAdminLevel[contractDetails.address] }}
                </span>
                <v-btn
                  v-else
                  icon
                  right
                  @click="$refs.disapproveAccountModal.preselectAutocomplete(props.item.id, props.item.type, props.item.address)">
                  <v-icon>
                    close
                  </v-icon>
                </v-btn>
              </td>
            </tr>
          </template>
        </v-data-table>
      </v-tab-item>
      <v-tab-item v-if="contractDetails.contractType > 0" key="adminAccountsTable">
        <v-flex v-if="!loading" justify-center>
          <v-btn
            :loading="loadingAdminWalletsFromContract"
            color="primary"
            flat
            @click="loadAdminWalletsFromContract">
            Load all from contract
          </v-btn>
        </v-flex>
        <v-data-table
          v-if="contractDetails"
          :items="adminWallets"
          :loading="loadingAdminWalletsFromContract"
          no-data-text=""
          hide-actions
          hide-headers>
          <template slot="items" slot-scope="props">
            <tr v-if="props.item.accountAdminLevel && props.item.accountAdminLevel[contractDetails.address] != 'not admin' && props.item.accountAdminLevel[contractDetails.address] >= 1">
              <td>
                <v-avatar size="36px">
                  <img :src="props.item.avatar ? props.item.avatar : '/eXoSkin/skin/images/system/UserAvtDefault.png'" onerror="this.src = '/eXoSkin/skin/images/system/UserAvtDefault.png'">
                </v-avatar>
              </td>
              <td>
                <profile-chip
                  :address="props.item.address"
                  :profile-id="props.item.id"
                  :profile-technical-id="props.item.technicalId"
                  :space-id="props.item.spaceId"
                  :profile-type="props.item.type"
                  :display-name="props.item.name"
                  :enabled="props.item.enabled"
                  :deleted-user="props.item.deletedUser"
                  :disabled-user="props.item.disabledUser"
                  :avatar="props.item.avatar" />
              </td>
              <td>
                {{ props.item.accountAdminLevel[contractDetails.address] }} level
              </td>
              <td v-if="$refs.removeAdminModal">
                <v-btn
                  icon
                  right
                  @click="$refs.removeAdminModal.preselectAutocomplete(props.item.id, props.item.type, props.item.address)">
                  <v-icon>
                    close
                  </v-icon>
                </v-btn>
              </td>
            </tr>
          </template>
        </v-data-table>
      </v-tab-item>
    </v-tabs-items>
    <transactions-list
      v-if="contractDetails.contractType === 0"
      id="transactionsList"
      ref="transactionsList"
      :network-id="networkId"
      :account="contractDetails.address"
      :contract-details="contractDetails"
      :fiat-symbol="fiatSymbol"
      :error="error"
      display-full-transaction
      @loaded="computeTransactionsCount"
      @error="error = $event" />
  </v-flex>
</template>

<script>
import ContractAdminModal from '../common/WalletAdminOperationModal.vue';
import UpgradeTokenModal from './modals/WalletAdminUpgradeTokenModal.vue';

export default {
  components: {
    ContractAdminModal,
    UpgradeTokenModal,
  },
  props: {
    isDisplayOnly: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    networkId: {
      type: Number,
      default: function() {
        return 0;
      },
    },
    refreshIndex: {
      type: Number,
      default: function() {
        return 0;
      },
    },
    walletAddress: {
      type: String,
      default: function() {
        return null;
      },
    },
    fiatSymbol: {
      type: String,
      default: function() {
        return null;
      },
    },
    contractDetails: {
      type: Object,
      default: function() {
        return {};
      },
    },
    addressEtherscanLink: {
      type: String,
      default: function() {
        return null;
      },
    },
  },
  data() {
    return {
      selectedTab: 0,
      totalTransactionsCount: 0,
      approvedWallets: [],
      adminWallets: [],
      approvedWalletsLoadedFromContract: false,
      loadingApprovedWalletsFromContract: false,
      loadingAdminWalletsFromContract: false,
      error: null,
    };
  },
  computed: {
    totalSupply() {
      return this.contractDetails && this.contractDetails.totalSupply ? this.walletUtils.convertTokenAmountReceived(this.contractDetails.totalSupply, this.contractDetails.decimals) : 0;
    },
  },
  watch: {
    contractDetails() {
      this.error = null;
      this.totalTransactionsCount = 0;
    },
  },
  methods: {
    successSendingEther() {
      this.refreshBalance().then(() => {
        this.$emit('success', this.contractDetails);
        this.$forceUpdate();
      });
    },
    retrieveAccountDetails(wallet, ignoreApproved, ignoreAdmin) {
      if (!wallet.approved) {
        wallet.approved = {};
      }
      if (!wallet.accountAdminLevel) {
        wallet.accountAdminLevel = {};
      }

      const promises = [];
      if (!ignoreApproved && !wallet.approved.hasOwnProperty(this.contractDetails.address)) {
        wallet.approved[this.contractDetails.address] = false;
        promises.push(
          this.contractDetails.contract.methods
            .isApprovedAccount(wallet.address)
            .call()
            .then((approved) => {
              this.$set(wallet.approved, this.contractDetails.address, approved ? 'approved' : 'disapproved');
              return this.addressRegistry.refreshWallet(wallet);
            })
            .catch((e) => {
              console.debug('Error getting approval of account', wallet.address, e);
              this.$set(wallet.approved, this.contractDetails.address, 'disapproved');
            })
        );
      }
      if (!ignoreAdmin && !wallet.accountAdminLevel.hasOwnProperty(this.contractDetails.address)) {
        wallet.accountAdminLevel[this.contractDetails.address] = 0;
        promises.push(
          this.contractDetails.contract.methods
            .getAdminLevel(wallet.address)
            .call()
            .then((level) => {
              level = Number(level);
              this.$set(wallet.accountAdminLevel, this.contractDetails.address, level ? level : 'not admin');
            })
            .catch((e) => {
              console.debug('Error getting admin level of account', wallet.address, e);
              this.$set(wallet.accountAdminLevel, this.contractDetails.address, 'not admin');
            })
        );
      }
      return Promise.all(promises);
    },
    newTransactionPending(transaction, contractDetails) {
      if (!contractDetails) {
        contractDetails = this.contractDetails;
      }
      if (this.$refs.transactionsList) {
        this.$refs.transactionsList.init(true);
      }
      if (this.contractDetails && transaction.value > 0) {
        this.$set(this.contractDetails, 'loadingBalance', true);
      }
      this.$emit('pending-transaction', transaction, contractDetails);
      this.$forceUpdate();
    },
    transactionError(error) {
      this.error = String(error);
      this.$forceUpdate();
    },
    computeTransactionsCount(transactions, count) {
      this.totalTransactionsCount = count;
    },
    refreshBalance() {
      this.$set(this.contractDetails, 'loadingBalance', false);
      return this.walletUtils.computeBalance(this.contractDetails.address).then((contractBalance) => {
        if (contractBalance) {
          this.$set(this.contractDetails, 'contractBalance', contractBalance.balance);
          this.$set(this.contractDetails, 'contractBalanceFiat', contractBalance.balanceFiat);
        }
      });
    },
    loadApprovedWalletsFromContract() {
      this.approvedWalletsLoadedFromContract = true;
      this.loadWalletsFromContract('ApprovedAccount', 'target', this.approvedWallets)
      .then((result, error) => {
        this.approvedWalletsLoadedFromContract = false;
      });
    },
    loadAdminWalletsFromContract() {
      this.loadWalletsFromContract('AddedAdmin', 'target', this.adminWallets);
    },
    loadWalletsFromContract(eventName, paramName, walletsArray) {
      this.loadingApprovedWalletsFromContract = true;
      this.loadingAdminWalletsFromContract = true;
      try {
        return this.contractDetails.contract.getPastEvents(eventName, {
          fromBlock: 0,
          toBlock: 'latest',
          filter: {
            isError: 0,
            txreceipt_status: 1,
          },
        })
          .then((events) => {
            if(events && events.length) {
              const promises = [];
              events.forEach((event) => {
                if (event.returnValues && event.returnValues[paramName]) {
                  const address = event.returnValues[paramName];
                  const wallet = {
                    address: address,
                  };
                  if(this.contractDetails.owner && address.toLowerCase() === this.contractDetails.owner.toLowerCase()) {
                    wallet.owner = true;
                    wallet.name = "Admin";
                  }
                  walletsArray.unshift(wallet);
                  promises.push(
                      Promise.resolve(this.retrieveAccountDetails(wallet))
                  );
                }
              });
              return Promise.all(promises);
            }
          })
          .catch((e) => {
            console.debug('Error loading wallets from Contract', e);
          })
          .finally(() => {
            this.loadingApprovedWalletsFromContract = false;
            this.loadingAdminWalletsFromContract = false;
          });
      } catch(e) {
        this.loadingApprovedWalletsFromContract = false;
        this.loadingAdminWalletsFromContract = false;
        console.debug('Error loading wallets from Contract', e);
        return Promise.reject(e);
      }
    },
    successTransaction(hash, contractDetails, methodName, autoCompleteValue, inputValue) {
      if(methodName === 'disapproveAccount') {
        const index = this.approvedWallets.findIndex(wallet => wallet.address === autoCompleteValue);
        if(index >= 0) {
          this.approvedWallets.splice(index, 1);
        }
      } else if(methodName === 'upgrade') {
        this.tokenUtils.retrieveContractDetails(this.walletAddress, contractDetails, true);
      }
    },
  },
};
</script>
