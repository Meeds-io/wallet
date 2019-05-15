<template>
  <v-card flat>
    <v-card-text v-if="error && String(error).trim() != '{}'" class="text-xs-center">
      <div class="alert alert-error">
        <i class="uiIconError"></i> {{ error }}
      </div>
    </v-card-text>
    <v-card-text
      class="text-xs-center"
      data-app>
      <v-menu
        ref="selectedDateMenu"
        v-model="selectedDateMenu"
        transition="scale-transition"
        lazy
        offset-y
        class="dateSelector">
        <v-text-field
          slot="activator"
          v-model="periodDatesDisplay"
          label="Select the period date"
          prepend-icon="event" />
        <v-date-picker
          v-model="selectedDate"
          :first-day-of-week="1"
          :type="!periodType || periodType === 'WEEK' ? 'date' : 'month'"
          @input="selectedDateMenu = false" />
      </v-menu>
    </v-card-text>
    <v-container fluid grid-list-md>
      <v-layout
        row
        wrap
        class="text-xs-center">
        <v-flex md4 xs12>
          <h4>Eligible users: <strong>{{ eligibleUsersCount }}</strong></h4>
        </v-flex>
        <v-flex md4 xs12>
          <h4>Total budget: <strong>{{ toFixed(totalBudget) }} {{ symbol }}</strong></h4>
        </v-flex>
        <v-flex md4 xs12>
          <h4>Sent tokens: <strong>{{ toFixed(sentBudget) }} {{ symbol }}</strong></h4>
        </v-flex>
        <v-flex
          v-for="totalReward in totalRewards"
          :key="totalReward.pluginId"
          md4
          xs12>
          <h4>Total {{ totalReward.pluginId }}: <strong>{{ totalReward.total }}</strong></h4>
        </v-flex>
      </v-layout>
    </v-container>
    <v-container>
      <v-layout>
        <v-flex md4 xs12>
          <v-switch v-model="displayDisabledUsers" label="Display disabled users" />
        </v-flex>
      </v-layout>
      <v-flex>
        <v-text-field
          v-model="search"
          append-icon="search"
          label="Search in name, pools, wallet address"
          single-line
          hide-details />
      </v-flex>
    </v-container>

    <v-data-table
      :headers="identitiesHeaders"
      :items="filteredIdentitiesList"
      :loading="loading"
      :sortable="true"
      item-key="address"
      class="elevation-1 mr-3 mb-2"
      disable-initial-sort
      hide-actions>
      <template slot="items" slot-scope="props">
        <tr :active="props.selected">
          <td>
            <v-avatar size="36px">
              <img :src="props.item.wallet.avatar" onerror="this.src = '/eXoSkin/skin/images/system/SpaceAvtDefault.png'">
            </v-avatar>
          </td>
          <td class="text-xs-left">
            <profile-chip
              :address="props.item.wallet.address"
              :profile-id="props.item.wallet.id"
              :profile-technical-id="props.item.wallet.technicalId"
              :space-id="props.item.wallet.spaceId"
              :profile-type="props.item.wallet.type"
              :display-name="props.item.wallet.name"
              :enabled="props.item.wallet.enabled"
              :disabled-in-reward-pool="props.item.disabledPool"
              :disapproved="props.item.wallet.disapproved"
              :deleted-user="props.item.wallet.deletedUser"
              :disabled-user="props.item.wallet.disabledUser"
              :avatar="props.item.wallet.avatar"
              :initialization-state="props.item.wallet.initializationState"
              display-no-address />
          </td>
          <td class="text-xs-left">
            <ul v-if="props.item.rewardTeams && props.item.rewardTeams.length">
              <li v-for="team in props.item.rewardTeams" :key="team.id">
                <template v-if="team.disabled">
                  <del class="red--text">{{ team.name }}</del> (Disabled)
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
          <td class="text-xs-center">
            <a
              v-if="props.item.rewardTransaction && props.item.rewardTransaction.hash"
              :href="`${transactionEtherscanLink}${props.item.rewardTransaction.hash}`"
              target="_blank"
              title="Open in etherscan">
              Open in etherscan
            </a> <span v-else>
              -
            </span>
          </td>
          <td class="text-xs-center">
            <template v-if="!props.item.rewardTransaction || !props.item.rewardTransaction.status">
              <v-icon
                v-if="!props.item.wallet.address"
                color="warning"
                title="No address">
                warning
              </v-icon>
              <v-icon
                v-else-if="!props.item.tokensToSend"
                :title="`No enough earned points`"
                color="warning">
                warning
              </v-icon>
              <div v-else>
                -
              </div>
            </template>
            <v-progress-circular
              v-else-if="props.item.rewardTransaction.status === 'pending'"
              color="primary"
              indeterminate
              size="20" />
            <v-icon
              v-else
              :color="props.item.rewardTransaction.status === 'success' ? 'success' : 'error'"
              :title="props.item.rewardTransaction.status === 'success' ? 'Successfully proceeded' : props.item.rewardTransaction.status === 'pending' ? 'Transaction in progress' : 'Transaction error'"
              v-text="props.item.rewardTransaction.status === 'success' ? 'fa-check-circle' : 'fa-exclamation-circle'" />
          </td>
          <td class="text-xs-center">
            <span
              v-if="props.item.tokensSent"
              class="grey--text text--darken-1"
              title="Amount sent">
              {{ toFixed(props.item.tokensSent) }} {{ symbol }}
            </span>
            <span v-else-if="props.item.tokensToSend" title="Amount to send">
              {{ toFixed(props.item.tokensToSend) }} {{ symbol }}
            </span>
            <span
              v-else
              class="grey--text text--darken-1"
              title="No rewards for selected period">
              0 {{ symbol }}
            </span>
          </td>
          <td>
            <v-btn
              :disabled="!props.item.rewards || !props.item.rewards.length"
              icon
              small
              title="Display reward details"
              @click="selectedWallet = props.item">
              <v-icon :color="(props.item.rewards && props.item.rewards.length) && 'primary'" size="16">fa-info-circle</v-icon>
            </v-btn>
          </td>
        </tr>
      </template>
      <template slot="footer">
        <td :colspan="identitiesHeaders.length - 2">
          <strong>
            Total
          </strong>
        </td>
        <td colspan="2">
          <strong>
            {{ totalTokens }} {{ symbol }}
          </strong>
        </td>
      </template>
    </v-data-table>

    <v-card-actions>
      <v-spacer />
      <v-btn
        :loading="sendingRewards"
        :disabled="sendingRewardsDisabled"
        class="btn btn-primary pl-2 pr-2"
        @click="sendRewards">
        Send rewards
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
    walletRewards: {
      type: Array,
      default: function() {
        return [];
      },
    },
    walletAddress: {
      type: String,
      default: function() {
        return null;
      },
    },
    periodType: {
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
    transactionEtherscanLink: {
      type: String,
      default: function() {
        return null;
      },
    },
    eligibleUsersCount: {
      type: Number,
      default: function() {
        return 0;
      },
    },
    totalBudget: {
      type: Number,
      default: function() {
        return 0;
      },
    },
    sentBudget: {
      type: Number,
      default: function() {
        return 0;
      },
    },
    totalRewards: {
      type: Array,
      default: function() {
        return [];
      },
    },
  },
  data() {
    return {
      search: '',
      displayDisabledUsers: false,
      selectedDate: `${new Date().getFullYear()}-${new Date().getMonth() + 1}-${new Date().getDate()}`,
      selectedDateMenu: false,
      selectedStartDate: null,
      selectedEndDate: null,
      loading: false,
      sendingRewards: false,
      selectedWallet: null,
      identitiesHeaders: [
        {
          text: '',
          align: 'right',
          sortable: false,
          value: 'avatar',
          width: '36px',
        },
        {
          text: 'Name',
          align: 'left',
          sortable: true,
          value: 'name',
        },
        {
          text: 'Pools',
          align: 'center',
          sortable: false,
          value: 'rewardTeams',
        },
        {
          text: 'Transaction',
          align: 'center',
          sortable: true,
          value: 'hash',
        },
        {
          text: 'Status',
          align: 'center',
          sortable: true,
          value: 'status',
        },
        {
          text: 'Tokens',
          align: 'center',
          sortable: true,
          value: 'tokensToSend',
          width: '80px',
        },
        {
          text: '',
          align: 'center',
          sortable: false,
          value: 'actions',
          width: '80px',
        },
      ],
    };
  },
  computed: {
    sendingRewardsDisabled() {
      if (!this.walletRewards || !this.walletRewards.length) {
        return true;
      }
      let disabledButton = true;
      for (const index in this.walletRewards) {
        const walletReward = this.walletRewards[index];
        if (walletReward.enabled && walletReward.tokensToSend && (!walletReward.rewardTransaction || !walletReward.rewardTransaction.status || walletReward.rewardTransaction.status === 'error')) {
          disabledButton = false;
        }
        // If any transaction is still pending, then disable sending button
        if (walletReward.rewardTransaction && walletReward.rewardTransaction.status === 'pending') {
          return true;
        }
      }
      return disabledButton;
    },
    periodDatesDisplay() {
      if (this.selectedStartDate && this.selectedEndDate) {
        return `${this.selectedStartDate} to ${this.selectedEndDate}`;
      } else if (this.selectedStartDate) {
        return this.selectedStartDate;
      } else {
        return '';
      }
    },
    selectedDateInSeconds() {
      return this.selectedDate ? new Date(this.selectedDate).getTime() / 1000 : 0;
    },
    selectedStartDateInSeconds() {
      return this.selectedStartDate ? new Date(this.selectedStartDate).getTime() / 1000 : 0;
    },
    selectedEndDateInSeconds() {
      return this.selectedEndDate ? new Date(this.selectedEndDate).getTime() / 1000 : 0;
    },
    symbol() {
      return this.contractDetails && this.contractDetails.symbol ? this.contractDetails.symbol : '';
    },
    filteredIdentitiesList() {
      return this.walletRewards ? this.walletRewards.filter((wallet) => (this.displayDisabledUsers || wallet.enabled || wallet.tokensSent || wallet.tokensToSend) && this.filterItemFromList(wallet, this.search)) : [];
    },
    totalTokens() {
      if (this.filteredIdentitiesList) {
        let result = 0;
        this.filteredIdentitiesList.forEach((wallet) => {
          result += wallet.tokensSent ? (wallet.tokensSent ? Number(wallet.tokensSent) : 0) : (wallet.tokensToSend && wallet.tokensToSend > 0 ? Number(wallet.tokensToSend) : 0);
        });
        return this.toFixed(result);
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
      this.refreshDates()
        .then(() => this.$nextTick())
        .then(() => this.$emit('dates-changed'));
    }
  },
  methods: {
    refreshDates() {
      this.loading = true;
      return getRewardDates(new Date(this.selectedDate), this.periodType)
        .then((period) => {
          this.selectedStartDate = this.formatDate(new Date(period.startDateInSeconds * 1000));
          this.selectedEndDate = this.formatDate(new Date(period.endDateInSeconds * 1000));
        })
        .finally(() => this.loading = false);
    },
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
      if (searchText === '-' && (!walletReward.rewardTeams || !walletReward.rewardTeams.length)) {
        return true;
      }
      const teams =
        walletReward.rewardTeams && walletReward.rewardTeams.length
          ? walletReward.rewardTeams
              .map((team) => team.name)
              .join(',')
              .toLowerCase()
          : '';
      return teams.indexOf(searchText) > -1;
    },
    sendRewards() {
      this.error = null;
      this.sendingRewards = true;
      sendRewards(this.selectedDateInSeconds)
        .then(() => {
          this.$emit('refresh');
        })
        .catch(e => {
          this.error = String(e);
        })
        .finally(() => {
          this.sendingRewards = false;
        });
    },
    formatDate(date) {
      if (!date) {
        return null;
      }
      const dateString = date.toString();
      // Example: 'Feb 01 2018'
      return dateString.substring(dateString.indexOf(' ') + 1, dateString.indexOf(':') - 3);
    },
  },
};
</script>
