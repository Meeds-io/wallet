<template>
  <v-flex flat transparent>
    <v-flex v-if="duplicatedWallets && duplicatedWallets.length" class="text-xs-center">
      <div class="alert alert-warning">
        <i class="uiIconWarning"></i> Some users are members of multiple pools, the budget computing could be wrong:
        <ul>
          <li v-for="duplicatedWallet in duplicatedWallets" :key="duplicatedWallet.id">
            <code>{{ duplicatedWallet.name }}</code>
          </li>
        </ul>
      </div>
    </v-flex>
    <confirm-dialog
      ref="deleteTeamConfirm"
      :loading="loading"
      :message="deletePoolMessage"
      title="Delete pool confirmation"
      ok-label="Delete"
      cancel-label="Cancel"
      @ok="removeTeam(teamToDelete.id)" />
    <add-team-form
      v-show="selectedTeam"
      ref="teamModal"
      :team="selectedTeam"
      :wallet-rewards="walletRewards"
      @saved="refreshTeams(true)"
      @close="selectedTeam = null" />
    <div
      v-show="!selectedTeam"
      id="addTeamButton"
      class="text-xs-left ml-3">
      <v-btn
        title="Add a new pool"
        color="primary"
        class="btn btn-primary"
        icon
        large
        @click="selectedTeam = {}">
        <v-icon>
          add
        </v-icon>
      </v-btn>
    </div>
    <h4 v-show="!selectedTeam" class="text-xs-center">
      <span>Eligible pools users: <strong>{{ eligiblePoolsUsersCount }}</strong></span>
    </h4>
    <h4 v-show="!selectedTeam" class="text-xs-center">
      <span>Total pools budget: <strong>{{ toFixed(poolsBudget) }} {{ symbol }}</strong></span>
    </h4>
    <v-container
      v-show="!selectedTeam"
      fluid
      grid-list-md>
      <v-data-iterator
        :items="teams"
        content-tag="v-layout"
        no-data-text=""
        row
        wrap
        hide-actions>
        <v-flex
          slot="item"
          slot-scope="props"
          class="rewardTeamCard"
          xs12
          sm12
          md6
          lg4>
          <v-card :style="props.item.spacePrettyName && `background: url(/portal/rest/v1/social/spaces/${props.item.spacePrettyName}/banner)  0 0/100% auto no-repeat`" class="elevation-3">
            <v-card flat class="transparent">
              <v-card-title class="pb-0">
                <v-chip dark>
                  <v-avatar v-if="props.item.spacePrettyName">
                    <img :src="`/portal/rest/v1/social/spaces/${props.item.spacePrettyName}/avatar`">
                  </v-avatar>
                  <h3 v-if="props.item.disabled" class="headline">
                    <del class="red--text">{{ props.item.name }}</del>
                  </h3>
                  <h3 v-else class="headline">
                    {{ props.item.name }}
                  </h3>
                </v-chip>
              </v-card-title>
              <v-card-title class="pt-0">
                <v-chip dark>
                  <h4 v-if="props.item.description">
                    {{ props.item.description }}
                  </h4>
                  <h4 v-else>
                    <i>No description</i>
                  </h4>
                </v-chip>
              </v-card-title>
              <v-divider />
              <v-list dense class="pb-0">
                <v-list-tile>
                  <v-list-tile-content>
                    Members:
                  </v-list-tile-content>
                  <v-list-tile-content class="align-end">
                    {{ props.item.members ? props.item.members.length : 0 }}
                  </v-list-tile-content>
                </v-list-tile>
                <v-list-tile v-if="props.item.rewardType === 'FIXED'">
                  <v-list-tile-content>
                    Fixed total budget:
                  </v-list-tile-content>
                  <v-list-tile-content class="align-end">
                    {{ props.item.budget }} {{ symbol }}
                  </v-list-tile-content>
                </v-list-tile>
                <v-list-tile v-if="props.item.rewardType === 'FIXED_PER_MEMBER'">
                  <v-list-tile-content>
                    Fixed budget per member:
                  </v-list-tile-content>
                  <v-list-tile-content class="align-end">
                    {{ Number(toFixed(props.item.rewardPerMember)) }} {{ symbol }}
                  </v-list-tile-content>
                </v-list-tile>
                <v-list-tile v-if="props.item.rewardType === 'COMPUTED'">
                  <v-list-tile-content>
                    Budget:
                  </v-list-tile-content>
                  <v-list-tile-content class="align-end">
                    Computed
                  </v-list-tile-content>
                </v-list-tile>
                <v-list-tile>
                  <v-flex class="align-start pr-1">
                    <v-divider />
                  </v-flex>
                  <v-flex
                    class="align-center">
                    <strong>
                      {{ period }}
                    </strong>
                  </v-flex>
                  <v-flex class="align-end pl-1">
                    <v-divider />
                  </v-flex>
                </v-list-tile>
                <v-list-tile>
                  <v-list-tile-content>
                    Eligible members:
                  </v-list-tile-content>
                  <v-list-tile-content class="align-end">
                    {{ props.item.validMembersWallets ? props.item.validMembersWallets.length : 0 }} / {{ props.item.members ? props.item.members.length : 0 }}
                  </v-list-tile-content>
                </v-list-tile>
                <v-list-tile>
                  <v-list-tile-content>
                    Budget:
                  </v-list-tile-content>
                  <v-list-tile-content
                    v-if="!Number(props.item.computedBudget) || !props.item.validMembersWallets || !props.item.validMembersWallets.length"
                    class="align-end red--text">
                    <strong>
                      0 {{ symbol }}
                    </strong>
                  </v-list-tile-content>
                  <v-list-tile-content v-else class="align-end">
                    {{ Number(toFixed(props.item.computedBudget)) }} {{ symbol }}
                  </v-list-tile-content>
                </v-list-tile>
                <v-list-tile>
                  <v-list-tile-content>
                    Budget per member:
                  </v-list-tile-content>
                  <v-list-tile-content
                    v-if="!Number(props.item.computedBudget) || !props.item.validMembersWallets || !props.item.validMembersWallets.length"
                    class="align-end red--text">
                    <strong>
                      0 {{ symbol }}
                    </strong>
                  </v-list-tile-content>
                  <v-list-tile-content v-else class="align-end">
                    {{ toFixed(Number(props.item.computedBudget) / props.item.validMembersWallets.length) }} {{ symbol }}
                  </v-list-tile-content>
                </v-list-tile>
              </v-list>
            </v-card>
            <v-card-actions>
              <v-spacer />
              <v-btn
                v-if="props.item.id"
                flat
                color="primary"
                @click="selectedTeam = props.item">
                Edit
              </v-btn>
              <v-btn
                v-else
                flat
                color="primary"
                @click="selectedTeam = props.item">
                View
              </v-btn>
              <v-btn
                v-if="props.item.id && props.item.disabled"
                flat
                color="primary"
                @click="disableTeam(props.item, false)">
                Enable
              </v-btn>
              <v-btn
                v-else-if="props.item.id"
                flat
                color="primary"
                @click="disableTeam(props.item, true)">
                Disable
              </v-btn>
              <v-btn
                v-if="props.item.id"
                flat
                color="primary"
                @click="
                  teamToDelete = props.item;
                  $refs.deleteTeamConfirm.open();
                ">
                Delete
              </v-btn>
              <v-spacer />
            </v-card-actions>
          </v-card>
        </v-flex>
      </v-data-iterator>
    </v-container>
  </v-flex>
</template>

<script>
import AddTeamForm from './TeamForm.vue';

import {getRewardTeams, saveRewardTeam, removeRewardTeam} from '../../js/RewardServices.js';

export default {
  components: {
    AddTeamForm,
  },
  props: {
    walletRewards: {
      type: Array,
      default: function() {
        return [];
      },
    },
    duplicatedWallets: {
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
    period: {
      type: String,
      default: function() {
        return null;
      },
    },
    eligiblePoolsUsersCount: {
      type: Number,
      default: function() {
        return 0;
      },
    },
  },
  data: () => ({
    teams: [],
    teamToDelete: null,
    selectedTeam: null,
  }),
  computed: {
    poolsBudget() {
      return this.walletRewards.reduce((sum, wallet) => sum + wallet.poolTokensToSend, 0)
    },
    deletePoolMessage() {
      return this.teamToDelete && `Would you like to delete pool <strong>${this.teamToDelete.name}</strong>`;
    },
    symbol() {
      return this.contractDetails && this.contractDetails.symbol ? this.contractDetails.symbol : '';
    },
  },
  watch: {
    selectedTeam() {
      if (this.selectedTeam) {
        this.$emit('form-opened');
      } else {
        this.$emit('form-closed');
      }
    },
  },
  methods: {
    refresh() {
      this.selectedTeam = null;
      this.$emit('refresh');
    },
    refreshTeams(refreshAll) {
      return getRewardTeams()
        .then((teams) => {
          this.teams = teams;
          if(refreshAll) {
            this.refresh();
          }
        })
        .catch((e) => {
          console.debug('Error getting teams list', e);
          this.error = 'Error getting teams list, please contact your administrator';
        });
    },
    disableTeam(team, disable) {
      if(team.id === 0) {
        return;
      }
      const teamToSave = Object.assign({}, team);
      teamToSave.disabled = disable;

      this.loading = true;
      delete teamToSave.validMembersWallets;
      return saveRewardTeam(teamToSave)
        .then(() => {
          this.refreshTeams(true);
        })
        .catch((e) => {
          console.debug('Error saving pool', e);
          this.error = 'Error saving pool, please contact your administrator.';
        })
        .finally(() => {
          this.loading = false;
        });
    },
    removeTeam(id) {
      if (id < 1) {
        console.error("Can't delete team with id", id);
      }
      removeRewardTeam(id)
        .then((status) => {
          if (status) {
            this.teamToDelete = null;
            return this.refreshTeams(true);
          } else {
            this.error = 'Error removing team';
          }
        })
        .catch((e) => {
          console.debug('Error getting team with id', id, e);
          this.error = 'Error removing team';
        });
    },
  },
};
</script>
