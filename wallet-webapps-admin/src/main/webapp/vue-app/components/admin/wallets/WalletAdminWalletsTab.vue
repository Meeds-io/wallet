<!--
This file is part of the Meeds project (https://meeds.io/).
Copyright (C) 2020 Meeds Association
contact@meeds.io
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3 of the License, or (at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.
You should have received a copy of the GNU Lesser General Public License
along with this program; if not, write to the Free Software Foundation,
Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
-->
<template>
  <v-flex flat>
    <confirm-dialog
      ref="informationModal"
      :loading="loading"
      :title="informationTitle"
      :message="informationMessage"
      :hide-default-footer="hideConfirmActions"
      width="400px"
      @ok="proceessAction" />
    <div v-if="error" class="alert alert-error v-content">
      <i class="uiIconError"></i>{{ error }}
    </div>
    <div v-if="!loading && !useWalletAdmin" class="red--text wrap title">
      <div>{{ $t('exoplatform.wallet.warning.adminWalletNotInitializedPart1') }}</div>
      <div>{{ $t('exoplatform.wallet.warning.adminWalletNotInitializedPart2') }}</div>
    </div>
    <wallet-admin-wallet
      v-if="!loading"
      :admin-wallet="walletAdmin"
      :initial-token-amount="tokenAmount"
      :contract-details="contractDetails"
      @refresh-balance="refreshWallet(walletAdmin, true)" />
    <v-toolbar
      color="transparent"
      flat
      class="border-box-sizing mx-0 infoMenu">
      <v-spacer />
      <v-text-field
        v-model="search"
        :placeholder="$t('exoplatform.wallet.label.searchInWalletPlaceholder')"
        prepend-inner-icon="fa-filter"
        class="pt-0 border-box-sizing walletTextField text-truncate mb-n4" />
      <v-menu
        v-model="showMenu"
        offset-y
        class="border-box-sizing dropDownFilter">
        <template v-slot:activator="{ on }">
          <button
            class="btn mx-4"
            v-on="on"
            @blur="closeMenu">
            {{ newsFilterLabel }}
            <i class="uiIconMiniArrowDown uiIconLightGray"></i>
          </button>
        </template>
        <v-list>
          <v-list-item @mousedown="$event.preventDefault()">
            <v-list-item-title class="filterLabel" @click="filterWallet ='all'">{{ $t('exoplatform.wallet.label.All') }}</v-list-item-title>
          </v-list-item>
          <v-list-item @mousedown="$event.preventDefault()">
            <v-list-item-title class="filterLabel" @click="filterWallet ='DisabledWallets'">{{ $t('exoplatform.wallet.label.disabledWalletsByAdmin') }}</v-list-item-title>
          </v-list-item>
          <v-list-item @mousedown="$event.preventDefault()">
            <v-list-item-title class="filterLabel" @click="filterWallet ='DisabledUsersWallets'"> {{ $t('exoplatform.wallet.label.disabledUsersWallets') }}  </v-list-item-title>
          </v-list-item>
          <v-list-item @mousedown="$event.preventDefault()">
            <v-list-item-title class="filterLabel" @click="filterWallet ='RejectedWallets'">{{ $t('exoplatform.wallet.label.rejectedWallets') }}</v-list-item-title>
          </v-list-item>
        </v-list>
      </v-menu>
    </v-toolbar>
    <v-data-table
      :headers="walletTableHeaders"
      :items="displayedWallets"
      :items-per-page="limit"
      :loading="loadingWallets">
      <template slot="item" slot-scope="props">
        <transition name="fade">
          <tr v-show="wallets">
            <td class="text-truncate">
              <v-avatar
                size="29"
                class="mx-1 clickable"
                @click="openAccountDetail(props.item)">
                <img
                  v-if="props.item.avatar"
                  :src="props.item.avatar"
                  onerror="this.src = '/eXoSkin/skin/images/system/SpaceAvtDefault.png'">
                <v-icon v-else size="29">fa-cog</v-icon>
              </v-avatar>
              <profile-chip
                :address="props.item.address"
                :profile-id="props.item.id"
                :profile-technical-id="props.item.technicalId"
                :space-id="props.item.spaceId"
                :profile-type="props.item.type"
                :display-name="props.item.name"
                :enabled="props.item.enabled"
                :disapproved="!props.item.isApproved"
                :deleted-user="props.item.deletedUser"
                :disabled-user="props.item.disabledUser"
                :avatar="props.item.avatar"
                display-no-address
                no-status />
            </td>
            <td class="clickable text-center" @click="openAccountDetail(props.item)">
              <template>
                <template v-if="props.item.deletedUser">{{ $t('exoplatform.wallet.label.deletedIdentity') }}</template>
                <template v-else-if="props.item.disabledUser">{{ $t('exoplatform.wallet.label.disabledUser') }}</template>
                <template v-else-if="!props.item.enabled">{{ $t('exoplatform.wallet.label.disabledWallet') }}</template>
                <template v-else-if="props.item.initializationState === 'NEW'">{{ $t('exoplatform.wallet.label.newWallet') }}</template>
                <template v-else-if="props.item.initializationState === 'DENIED'">{{ $t('exoplatform.wallet.label.rejectedWallet') }}</template>
                <template v-else-if="!props.item.isApproved">{{ $t('exoplatform.wallet.label.disapprovedWallet') }}</template>
                <template v-else>
                  <template v-if="Number(props.item.etherBalance) === 0 || (etherAmount && walletUtils.toFixed(props.item.etherBalance) < Number(etherAmount))">
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
                  <template v-else>
                    {{ $t('exoplatform.wallet.label.initializedWallet') }}
                  </template>
                </template>
              </template>
            </td>
            <td
              v-if="contractDetails"
              class="clickable text-center"
              @click="openAccountDetail(props.item)">
              <v-progress-circular
                v-if="props.item.loading"
                color="primary"
                class="me-4"
                indeterminate
                size="20" />
              <template v-else>
                {{ walletUtils.toFixed(props.item.tokenBalance) || 0 }} {{ contractDetails && contractDetails.symbol ? contractDetails.symbol : '' }}
              </template>
            </td>
            <td class="clickable text-center" @click="openAccountDetail(props.item)">
              <v-progress-circular
                v-if="props.item.loading"
                color="primary"
                class="me-4"
                indeterminate
                size="20" />
              <template v-else>
                {{ walletUtils.toFixed(props.item.etherBalance) || 0 }} E
              </template>
            </td>
            <td class="text-center">
              <v-progress-circular
                v-if="props.item.pendingTransaction || props.item.loading"
                :title="$t('exoplatform.wallet.message.transactionInProgress')"
                color="primary"
                class="me-4"
                indeterminate
                size="20" />
              <v-menu
                v-else
                offset-y
                :value="actionSelectedWallet === props.item"
                attach>
                <template
                  v-slot:activator="{ on }">
                  <v-btn
                    icon
                    small
                    v-on="on"
                    class="actionsButton"
                    @click="actionSelectedWallet = props.item"
                    @blur="closeActionsMenu">
                    <v-icon size="20px">fa-ellipsis-v</v-icon>
                  </v-btn>
                </template>
                <v-list flat class="pt-0 pb-0">
                  <template>
                    <v-list-item @mousedown="$event.preventDefault()">
                      <v-list-item-title class="options" @click="refreshWallet(props.item, true)">{{ $t('exoplatform.wallet.button.refreshWallet') }}</v-list-item-title>
                    </v-list-item>
                    <v-divider />
                    <template v-if="(props.item.type === 'user' || props.item.type === 'space')">
                      <template v-if="useWalletAdmin">
                        <template v-if="contractDetails && contractDetails.contractType && contractDetails.contractType > 1 && (props.item.initializationState === 'NEW' || props.item.initializationState === 'MODIFIED' || props.item.initializationState === 'DENIED') && !props.item.disabledUser && !props.item.deletedUser && props.item.enabled">
                          <v-list-item :disabled="adminNotHavingEnoughToken" @mousedown="$event.preventDefault()">
                            <v-list-item-title class="options" @click="openAcceptInitializationModal(props.item)">{{ $t('exoplatform.wallet.button.initializeWallet') }}</v-list-item-title>
                          </v-list-item>
                          <v-list-item v-if="props.item.initializationState !== 'DENIED'" @mousedown="$event.preventDefault()">
                            <v-list-item-title class="options" @click="openDenyInitializationModal(props.item)">{{ $t('exoplatform.wallet.button.rejectWallet') }}</v-list-item-title>
                          </v-list-item>
                          <v-divider />
                        </template>
                        <template v-else-if="props.item.isApproved && !props.item.disabledUser && !props.item.deletedUser && props.item.enabled && (Number(props.item.etherBalance) === 0 || (etherAmount && walletUtils.toFixed(props.item.etherBalance) < Number(etherAmount)))">
                          <v-list-item :disabled="adminNotHavingEnoughEther" @mousedown="$event.preventDefault()">
                            <v-list-item-title class="options" @click="openSendEtherModal(props.item)">{{ $t('exoplatform.wallet.button.sendEther') }}</v-list-item-title>
                          </v-list-item>
                          <v-divider />
                        </template>
                        <v-list-item
                          v-if="contractDetails && !contractDetails.isPaused && !props.item.disabledUser && !props.item.deletedUser && props.item.enabled && props.item.isApproved && tokenAmount > 0"
                          :disabled="adminNotHavingEnoughToken"
                          @mousedown="$event.preventDefault()">
                          <v-list-item-title class="options" @click="openSendTokenModal(props.item)">{{ $t('exoplatform.wallet.button.sendToken', {0: contractDetails && contractDetails.name}) }}</v-list-item-title>
                        </v-list-item>
                        <v-divider />
                      </template>

                      <v-list-item v-if="props.item.enabled" @mousedown="$event.preventDefault()">
                        <v-list-item-title class="options" @click="openDisableWalletModal(props.item)">{{ $t('exoplatform.wallet.button.disableWallet') }}</v-list-item-title>
                      </v-list-item>
                      <v-list-item v-else-if="!props.item.disabledUser && !props.item.deletedUser" @mousedown="$event.preventDefault()">
                        <v-list-item-title class="options" @click="enableWallet(props.item, true)">{{ $t('exoplatform.wallet.button.enableWallet') }}</v-list-item-title>
                      </v-list-item>
                    </template>
                  </template>
                </v-list>
              </v-menu>
            </td>
          </tr>
        </transition>
      </template>
    </v-data-table>

    <wallet-initialize-account-modal
      ref="initAccountModal"
      @sent="walletPendingTransaction" />

    <wallet-send-token-modal
      ref="sendTokenModal"
      :contract-details="contractDetails"
      @sent="walletPendingTransaction" />
    <wallet-send-ether-modal
      ref="sendEtherModal"
      @sent="walletPendingTransaction" />

    <!-- The selected account detail -->
    <account-detail
      ref="accountDetail"
      :fiat-symbol="fiatSymbol"
      :wallet="selectedWallet"
      :contract-details="selectedWalletDetails"
      :selected-transaction-hash="selectedTransactionHash"
      is-read-only
      is-display-only
      is-administration
      @back="back()" />
  </v-flex>
</template>

<script>
export default {
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
  },
  data() {
    return {
      search: null,
      newsFilterLabel: '',
      filterWallet: '',
      showMenu: false,
      loadingWallets: false,
      selectedTransactionHash: null,
      seeAccountDetails: false,
      seeAccountDetailsPermanent: false,
      selectedWalletAddress: null,
      selectedWallet: null,
      selectedWalletDetails: null,
      actionSelectedWallet: {},
      informationTitle: null,
      informationMessage: null,
      hideConfirmActions: null,
      confirmAction: null,
      error: null,
      wallets: [],
      walletToProcess: null,
      etherAmount: null,
      tokenAmount: null,
      limit: 5,
      walletTypes: ['user','space','admin'],
      walletStatuses: ['disapproved'],
    };
  },
  computed: {
    walletHeaders() {
      return [
        {
          text: this.$t('exoplatform.wallet.label.name'),
          align: 'center',
          sortable: true,
          value: 'name',
        },
        {
          text: this.$t('exoplatform.wallet.label.walletStatus'),
          align: 'center',
          sortable: false,
          value: 'isApproved',
        },
        {
          text: this.$t('exoplatform.wallet.label.tokenBalance', {0: this.contractDetails && this.contractDetails.name}),
          align: 'center',
          value: 'tokenBalance',
        },
        {
          text: this.$t('exoplatform.wallet.label.etherBalance'),
          align: 'center',
          value: 'etherBalance',
        },
        {
          text: this.$t('exoplatform.wallet.label.actions'),
          align: 'center',
          sortable: false,
          value: '',
        },
      ];
    },
    walletAdmin() {
      return this.wallets && this.wallets.find(wallet => wallet && wallet.type === 'admin');
    },
    adminNotHavingEnoughToken() {
      return this.walletAdmin && this.walletAdmin.tokenBalance < this.tokenAmount;
    },
    adminNotHavingEnoughEther() {
      return this.walletAdmin && this.walletAdmin.etherBalance < this.etherAmount;
    },
    useWalletAdmin() {
      return this.walletAdmin && this.walletAdmin.adminLevel >= 2 && this.walletAdmin.etherBalance && Number(this.walletAdmin.etherBalance) >= 0.002 && this.walletAdmin.tokenBalance && Number(this.walletAdmin.tokenBalance) >= 0.02;
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
      return this.walletStatuses && this.walletStatuses.includes('disabledWallet');
    },
    displayDisabledUserWallets() {
      return this.walletStatuses && this.walletStatuses.includes('disabledUser');
    },
    displayRejectedWallets() {
      return this.walletStatuses && this.walletStatuses.includes('rejectedWallet');
    },
    displayAllWallets() {
      return this.walletStatuses && this.walletStatuses.includes('all');
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
      if (!this.contractDetails) {
        walletTableHeaders.splice(4, 1);
      }
      if (!this.contractDetails || this.contractDetails.contractType < 2) {
        walletTableHeaders.splice(2, 1);
      }
      return walletTableHeaders;
    },
    displayedWallets() {
      return this.wallets.filter(this.isDisplayWallet);
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
    filterWallet() {
      if (this.filterWallet === 'DisabledWallets') {
        this.walletStatuses = ['disabledWallet'];
        this.newsFilterLabel = this.$t('exoplatform.wallet.label.disabledWalletsByAdmin');
      } else if (this.filterWallet === 'DisabledUsersWallets'){
        this.walletStatuses = ['disabledUser'];
        this.newsFilterLabel = this.$t('exoplatform.wallet.label.disabledUsersWallets');
      } else if (this.filterWallet === 'RejectedWallets') {
        this.walletStatuses = ['rejectedWallet'];
        this.newsFilterLabel = this.$t('exoplatform.wallet.label.rejectedWallets');
      } else {
        this.newsFilterLabel = this.$t('exoplatform.wallet.label.All');
        this.walletStatuses = ['all'];
      }
      this.showMenu = false;
    },
  },
  created() {
    this.newsFilterLabel = this.$t('exoplatform.wallet.label.All');
  },
  methods: {
    init() {
      if (this.loadingWallets) {
        return;
      }

      const initialFunds = window.walletSettings.initialFunds || {};
      this.etherAmount = initialFunds.etherAmount || 0;
      this.tokenAmount = initialFunds.tokenAmount || 0;

      return this.walletUtils.getWallets()
        .then((wallets) => {
          wallets.forEach((wallet) => {
            wallet.loading = true;
          });

          this.wallets = wallets.sort(this.sortByName);
          // *async* approval retrieval
          this.wallets.forEach((wallet) => {
            wallet.fiatBalance = wallet.fiatBalance || (wallet.etherBalance && this.walletUtils.etherToFiat(wallet.etherBalance));
            wallet.loading = false;
          });
          return this.$nextTick();
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
    isDisplayWallet(wallet) {
      if (this.displayDisabledWallets ) {
        return wallet && wallet.address
          && (this.displayUsers || wallet.type !== 'user')
          && (this.displaySpaces || wallet.type !== 'space')
          && (this.displayAdmin || wallet.type !== 'admin')
          && (this.displayDisabledWallets && (!wallet.enabled && !wallet.disabledUser))
          && (!this.search
            || wallet.name.toLowerCase().indexOf(this.search.toLowerCase()) >= 0
            || wallet.address.toLowerCase().indexOf(this.search.toLowerCase()) >= 0);
      } else if (this.displayDisabledUserWallets ) {
        return wallet && wallet.address
            && (this.displayUsers || wallet.type !== 'user')
            && (this.displaySpaces || wallet.type !== 'space')
            && (this.displayAdmin || wallet.type !== 'admin')
            && (this.displayDisabledUserWallets && wallet.disabledUser)
            && (!this.search
              || wallet.name.toLowerCase().indexOf(this.search.toLowerCase()) >= 0
              || wallet.address.toLowerCase().indexOf(this.search.toLowerCase()) >= 0);
      } else if (this.displayRejectedWallets ) {
        return wallet && wallet.address
            && (this.displayUsers || wallet.type !== 'user')
            && (this.displaySpaces || wallet.type !== 'space')
            && (this.displayAdmin || wallet.type !== 'admin')
            && (this.displayRejectedWallets && wallet.initializationState === 'DENIED')
            && (!this.search
              || wallet.name.toLowerCase().indexOf(this.search.toLowerCase()) >= 0
              || wallet.address.toLowerCase().indexOf(this.search.toLowerCase()) >= 0);
      } else if (this.displayAllWallets ) {
        return wallet && wallet.address
        && (!this.search
            || wallet.name.toLowerCase().indexOf(this.search.toLowerCase()) >= 0
            || wallet.address.toLowerCase().indexOf(this.search.toLowerCase()) >= 0);
      } else {
        return wallet && wallet.address
        && (this.displayUsers || wallet.type !== 'user')
        && (this.displaySpaces || wallet.type !== 'space')
        && (this.displayAdmin || wallet.type !== 'admin')
        && (this.displayDisabledWallets || (wallet.enabled && !wallet.disabledUser))
        && (this.displayDisapprovedWallets || wallet.isApproved)
        && (this.displayRejectedWallets || wallet.initializationState !== 'DENIED')
        && (this.displayDisabledUserWallets || !wallet.disabledUser)
        && (!this.search
            || wallet.name.toLowerCase().indexOf(this.search.toLowerCase()) >= 0
            || wallet.address.toLowerCase().indexOf(this.search.toLowerCase()) >= 0);
      }

    },
    refreshWallet(wallet, refreshOnBlockchain) {
      wallet.loading = true;
      return this.addressRegistry.refreshWallet(wallet, refreshOnBlockchain)
        .then(() => {
          wallet.fiatBalance = wallet.fiatBalance || (wallet.etherBalance && this.walletUtils.etherToFiat(wallet.etherBalance));
        })
        .finally(() => {
          wallet.loading = false;
        });
    },
    openAccountDetail(wallet, hash) {
      this.selectedTransactionHash = hash;
      this.selectedWalletAddress = wallet.address;
      this.selectedWallet = wallet;
      this.selectedWalletDetails = {
        title: 'ether',
        icon: 'fab fa-ethereum',
        symbol: 'ether',
        address: this.selectedWalletAddress,
        balance: wallet.etherBalance,
        balanceFiat: wallet.fiatBalance,
        details: wallet,
      };
      this.seeAccountDetails = true;
      this.$refs.accountDetail.open();
      this.$nextTick(() => {
        const thiss = this;
        $('.v-overlay')
          .off('click')
          .on('click', () => thiss.back());
      });
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
          if (result) {
            return this.addressRegistry.refreshWallet(wallet);
          } else {
            this.error = (enable && this.$t('exoplatform.wallet.error.errorEnablingWallet', {0: wallet.name})) || this.$t('exoplatform.wallet.error.errorDisablingWallet', {0: wallet.name});
          }
        })
        .catch(e => this.error = String(e));
    },
    walletPendingTransaction(hash) {
      this.$emit('pending', hash);
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
      this.$refs.sendTokenModal.open(wallet, null, this.tokenAmount);
    },
    openSendEtherModal(wallet) {
      this.walletToProcess = wallet;
      const etherAmount = this.walletUtils.toFixed(this.etherAmount) - Number(wallet && wallet.etherBalance);
      if (etherAmount && etherAmount > 0 && Number.isFinite(etherAmount)) {
        this.$refs.sendEtherModal.open(wallet, null, etherAmount);
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
      if (wallet) {
        return this.walletUtils.saveWalletInitializationStatus(wallet.address, status)
          .then(() => {
            return this.refreshWallet(wallet);
          }).catch(e => {
            this.error = String(e);
          });
      }
    },
    watchWalletTransaction(wallet, hash) {
      this.walletUtils.watchTransactionStatus(hash, () => {
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
    },
    closeMenu() {
      this.showMenu = false;
    },
    closeActionsMenu() {
      this.actionSelectedWallet = {};
    },
  },
};
</script>