<template>
  <v-card flat>
    <v-card-text v-if="error && String(error).trim() != '{}'" class="text-center">
      <div class="alert alert-error">
        <i class="uiIconError"></i> {{ error }}
      </div>
    </v-card-text>
    <v-card-text
      class="text-center"
      data-app>
      <v-menu
        ref="selectedDateMenu"
        v-model="selectedDateMenu"
        transition="scale-transition"
        offset-y
        class="dateSelector">
        <template v-slot:activator="{ on }">
          <v-text-field
            v-model="periodDatesDisplay"
            :label="$t('exoplatform.wallet.label.selectPeriodDate')"
            class="dateSelectorInput"
            prepend-icon="event"
            v-on="on" />
        </template>
        <v-date-picker
          v-model="selectedDate"
          :first-day-of-week="1"
          :type="!periodType || periodType === 'WEEK' ? 'date' : 'month'"
          :locale="lang"
          @input="selectedDateMenu = false" />
      </v-menu>
    </v-card-text>
    <v-container fluid grid-list-md>
      <v-layout
        row
        wrap
        class="text-center">
        <v-flex md4 xs12>
          <h4>{{ $t('exoplatform.wallet.label.eligibleUsers') }}: <strong>{{ validRewardCount }}</strong></h4>
        </v-flex>
        <v-flex md4 xs12>
          <h4>{{ $t('exoplatform.wallet.label.totalBudget') }}: <strong>{{ totalBudget }} {{ symbol }}</strong></h4>
        </v-flex>
        <v-flex md4 xs12>
          <h4>{{ $t('exoplatform.wallet.label.sent') }} {{ contractDetails && contractDetails.name }}: <strong>{{ walletUtils.toFixed(sentBudget) }} {{ symbol }}</strong></h4>
        </v-flex>
        <v-flex md4 xs12>
          <h4>
            {{ $t('exoplatform.wallet.title.adminWalletFunds') }}:
            <strong>{{ adminBalance }} {{ symbol }}</strong>
            <v-icon
              v-if="adminBalanceTooLow"
              color="orange"
              :title="$t('exoplatform.wallet.label.adminBalanceTooLow')">
              warning
            </v-icon>
          </h4>
        </v-flex>
        <v-flex
          v-for="totalReward in totalRewards"
          :key="totalReward.pluginId"
          md4
          xs12>
          <h4>{{ $t('exoplatform.wallet.label.total') }} {{ totalReward.pluginId }}: <strong>{{ totalReward.total }}</strong></h4>
        </v-flex>
      </v-layout>
    </v-container>
    <v-container>
      <v-layout>
        <v-flex md4 xs12>
          <v-switch v-model="displayDisabledUsers" :label="$t('exoplatform.wallet.label.displayDisabledUsers')" />
        </v-flex>
      </v-layout>
      <v-flex>
        <v-text-field
          v-model="search"
          :label="$t('exoplatform.wallet.label.searchRewardWalletsPlaceholder')"
          append-icon="search"
          single-line
          hide-details />
      </v-flex>
    </v-container>

    <v-data-table
      :headers="identitiesHeaders"
      :items="filteredIdentitiesList"
      :items-per-page="1000"
      :loading="loading"
      item-key="identityId"
      class="elevation-1 mr-3 mb-2"
      hide-default-footer
      sortable>
      <template slot="item" slot-scope="props">
        <tr :active="props.selected">
          <td>
            <v-avatar size="36px">
              <img :src="props.item.wallet.avatar" onerror="this.src = '/eXoSkin/skin/images/system/SpaceAvtDefault.png'">
            </v-avatar>
          </td>
          <td class="text-left">
            <profile-chip
              :address="props.item.wallet.address"
              :profile-id="props.item.wallet.id"
              :profile-technical-id="props.item.wallet.technicalId"
              :space-id="props.item.wallet.spaceId"
              :profile-type="props.item.wallet.type"
              :display-name="props.item.wallet.name"
              :enabled="props.item.wallet.enabled"
              :disabled-in-reward-pool="props.item.disabledPool"
              :disapproved="!props.item.wallet.isApproved"
              :deleted-user="props.item.wallet.deletedUser"
              :disabled-user="props.item.wallet.disabledUser"
              :avatar="props.item.wallet.avatar"
              :initialization-state="props.item.wallet.initializationState"
              display-no-address />
          </td>
          <td class="text-left">
            <ul v-if="props.item.teams">
              <li v-for="team in props.item.teams" :key="team.id">
                <template v-if="team.disabled">
                  <del class="red--text">{{ team.name }}</del> ({{ $t('exoplatform.wallet.label.disabledPool') }})
                </template>
                <template v-else>
                  {{ team.name }}
                </template>
              </li>
            </ul>
            <div v-else>
              -
            </div>
          </td>
          <td class="text-center">
            <a
              v-if="props.item.transaction && props.item.transaction.hash"
              :href="`${transactionEtherscanLink}${props.item.transaction.hash}`"
              :title="$t('exoplatform.wallet.label.openOnEtherscan')"
              target="_blank">
              {{ $t('exoplatform.wallet.label.openOnEtherscan') }}
            </a> <span v-else>
              -
            </span>
          </td>
          <td class="text-center">
            <template v-if="!props.item.status">
              <v-icon
                v-if="!props.item.wallet.address"
                :title="$t('exoplatform.wallet.label.noAddress')"
                color="warning">
                warning
              </v-icon>
              <v-icon
                v-else-if="!props.item.tokensToSend"
                :title="$t('exoplatform.wallet.label.noEnoughEarnedPoints')"
                color="warning">
                warning
              </v-icon>
              <div v-else>
                -
              </div>
            </template>
            <v-progress-circular
              v-else-if="props.item.status === 'pending'"
              color="primary"
              indeterminate
              size="20" />
            <v-icon
              v-else
              :color="props.item.status === 'success' ? 'success' : 'error'"
              :title="props.item.status === 'success' ? 'Successfully proceeded' : props.item.status === 'pending' ? 'Transaction in progress' : 'Transaction error'"
              v-text="props.item.status === 'success' ? 'fa-check-circle' : 'fa-exclamation-circle'" />
          </td>
          <td class="text-center">
            <span
              v-if="props.item.tokensSent"
              :title="$t('exoplatform.wallet.label.amountSent')"
              class="grey--text text--darken-1">
              {{ walletUtils.toFixed(props.item.tokensSent) }} {{ symbol }}
            </span>
            <span v-else-if="props.item.tokensToSend" :title="$t('exoplatform.wallet.label.amountToSend')">
              {{ walletUtils.toFixed(props.item.tokensToSend) }} {{ symbol }}
            </span>
            <span
              v-else
              :title="$t('exoplatform.wallet.label.noRewardsForPeriod')"
              class="grey--text text--darken-1">
              0 {{ symbol }}
            </span>
          </td>
          <td>
            <v-btn
              :disabled="!props.item.rewards || !props.item.rewards.length"
              :title="$t('exoplatform.wallet.label.displayRewardDetails')"
              icon
              small
              @click="selectedWallet = props.item">
              <v-icon :color="(props.item.rewards && props.item.rewards.length) && 'primary'" size="16">fa-info-circle</v-icon>
            </v-btn>
          </td>
        </tr>
      </template>
      <template slot="body.append">
        <tr>
          <td :colspan="identitiesHeaders.length - 2">
            <strong>
              {{ $t('exoplatform.wallet.label.total') }}
            </strong>
          </td>
          <td colspan="2">
            <strong class="ml-7">
              {{ totalTokens }} {{ symbol }}
            </strong>
          </td>
        </tr>
      </template>
    </v-data-table>

    <v-card-actions>
      <v-spacer />
      <v-btn
        :loading="sendingRewards"
        :disabled="sendingRewardsDisabled"
        class="btn btn-primary pl-2 pr-2"
        @click="sendRewards">
        {{ $t('exoplatform.wallet.button.sendRewards') }}
      </v-btn>
      <v-spacer />
    </v-card-actions>

    <reward-detail-modal
      ref="rewardDetails"
      :wallet="selectedWallet"
      :period="periodDatesDisplay"
      :symbol="symbol"
      @closed="selectedWallet = null" />
  </v-card>
</template>

<script>
import RewardDetailModal from './modal/RewardDetailModal.vue';

import {getRewardDates, sendRewards} from '../../js/RewardServices.js';

export default {
  components: {
    RewardDetailModal,
  },
  props: {
    rewardReport: {
      type: Object,
      default: function() {
        return null;
      },
    },
    adminWallet: {
      type: Object,
      default: function() {
        return null;
      },
    },
    periodDatesDisplay: {
      type: String,
      default: function() {
        return null;
      },
    },
    totalRewards: {
      type: Array,
      default: function() {
        return [];
      },
    },
    contractDetails: {
      type: Object,
      default: function() {
        return null;
      },
    },
    transactionEtherscanLink: {
      type: String,
      default: function() {
        return null;
      },
    },
  },
  data() {
    return {
      search: '',
      currentTimeInSeconds: Date.now() / 1000,
      displayDisabledUsers: false,
      selectedDate: `${new Date().getFullYear()}-${new Date().getMonth() + 1}-${new Date().getDate()}`,
      selectedDateMenu: false,
      loading: false,
      sendingRewards: false,
      selectedWallet: null,
      lang: 'en',
    };
  },
  computed: {
    identitiesHeaders() {
      return [
        {
          text: '',
          align: 'right',
          sortable: false,
          value: 'avatar',
          width: '36px',
        },
        {
          text: this.$t('exoplatform.wallet.label.name'),
          align: 'left',
          sortable: true,
          value: 'wallet.name',
        },
        {
          text: this.$t('exoplatform.wallet.label.pools'),
          align: 'center',
          sortable: true,
          value: 'poolName',
        },
        {
          text: this.$t('exoplatform.wallet.label.transaction'),
          align: 'center',
          sortable: true,
          value: 'transaction.timestamp',
        },
        {
          text: this.$t('exoplatform.wallet.label.status'),
          align: 'center',
          sortable: true,
          value: 'status',
        },
        {
          text: (this.contractDetails && this.contractDetails.name),
          align: 'center',
          sortable: true,
          value: 'tokensToSend',
          width: '80px',
        },
        {
          text: '',
          align: 'center',
          sortable: false,
          value: 'rewards',
          width: '80px',
        },
      ];
    },
    adminBalance() {
      return (this.adminWallet && this.walletUtils.toFixed(this.adminWallet.tokenBalance)) || 0;
    },
    adminBalanceTooLow() {
      return this.rewardReport && this.adminWallet && this.rewardReport.remainingTokensToSend && this.adminWallet.tokenBalance < this.rewardReport.remainingTokensToSend;
    },
    walletRewards() {
      return (this.rewardReport && this.rewardReport.rewards) || [];
    },
    period() {
      return this.rewardReport && this.rewardReport.period;
    },
    periodType() {
      return this.period && this.period.rewardPeriodType;
    },
    sentBudget() {
      return (this.rewardReport && this.walletUtils.toFixed(this.rewardReport.tokensSent)) || 0;
    },
    totalBudget() {
      return (this.rewardReport && this.walletUtils.toFixed(this.rewardReport.tokensToSend)) || 0;
    },
    validRewardCount() {
      return (this.rewardReport && this.rewardReport.validRewardCount) || 0;
    },
    sendingRewardsDisabled() {
      if (this.isNotPastPeriod) {
        return true;
      }
      if (!this.walletRewards || !this.walletRewards.length) {
        return true;
      }
      let disabledButton = true;
      for (const index in this.walletRewards) {
        const walletReward = this.walletRewards[index];
        // If any transaction is still pending, then disable sending button
        if (walletReward.status === 'pending') {
          return true;
        }

        if (walletReward.enabled && walletReward.tokensToSend && (!walletReward.status || walletReward.status === 'error')) {
          disabledButton = false;
        }
      }
      return disabledButton;
    },
    selectedDateInSeconds() {
      return this.selectedDate ? new Date(this.selectedDate).getTime() / 1000 : 0;
    },
    symbol() {
      return this.contractDetails && this.contractDetails.symbol ? this.contractDetails.symbol : '';
    },
    filteredIdentitiesList() {
      return (this.walletRewards && this.walletRewards.filter((wallet) => (this.displayDisabledUsers || wallet.enabled || wallet.tokensSent || wallet.tokensToSend) && this.filterItemFromList(wallet, this.search))) || [];
    },
    isNotPastPeriod() {
      return !this.period || this.period.endDateInSeconds > this.currentTimeInSeconds;
    },
    totalTokens() {
      if (this.filteredIdentitiesList) {
        let result = 0;
        this.filteredIdentitiesList.forEach((wallet) => {
          result += wallet.tokensSent ? (wallet.tokensSent ? Number(wallet.tokensSent) : 0) : (wallet.tokensToSend && wallet.tokensToSend > 0 ? Number(wallet.tokensToSend) : 0);
        });
        return this.walletUtils.toFixed(result);
      } else {
        return 0;
      }
    },
  },
  watch: {
    selectedWallet() {
      if(this.selectedWallet) {
        this.$refs.rewardDetails.open();
      }
    },
    selectedDate() {
      this.loading = true;
      this.lang = eXo.env.portal.language;
      return getRewardDates(new Date(this.selectedDate), this.periodType)
        .then((period) => {
          this.$emit('dates-changed', period);
        })
        .finally(() => this.loading = false);
    }
  },
  methods: {
    filterItemFromList(walletReward, searchText) {
      if (!searchText || !searchText.length) {
        return true;
      }
      searchText = searchText.trim().toLowerCase();
      const name = walletReward && walletReward.wallet && walletReward.wallet.name && walletReward.wallet.name.toLowerCase();
      if (name.indexOf(searchText) > -1) {
        return true;
      }
      const address = walletReward && walletReward.wallet && walletReward.wallet.address && walletReward.wallet.address.toLowerCase();
      if (address.indexOf(searchText) > -1) {
        return true;
      }
      const poolName = walletReward && walletReward.poolName && walletReward.poolName.toLowerCase();
      if (searchText === '-' || (poolName.indexOf(searchText) > -1)) {
        return true;
      }
      return false;
    },
    sendRewards() {
      this.error = null;
      this.sendingRewards = true;
      sendRewards(this.selectedDateInSeconds)
        .catch(e => {
          this.error = String(e);
        })
        .finally(() => {
          this.$emit('refresh');
          this.sendingRewards = false;
        });
    },
  },
};
</script>
