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
  <v-flex flat transparent>
    <confirm-dialog
      ref="deleteTeamConfirm"
      :loading="loading"
      :message="deletePoolMessage"
      :title="$t('exoplatform.wallet.label.deletePoolConfirmation')"
      :ok-label="$t('exoplatform.wallet.button.delete')"
      cancel-label="Cancel"
      @ok="removeTeam(teamToDelete.id)" />
    <wallet-reward-add-team-form
      v-show="selectedTeam"
      ref="teamModal"
      :team="selectedTeam"
      :teams="teams"
      :wallet-rewards="walletRewards"
      @saved="teamSaved"
      @close="selectedTeam = null" />
    <div
      v-show="!selectedTeam"
      id="addTeamButton"
      class="text-start ms-3">
      <v-btn
        :title="$t('exoplatform.wallet.button.addNewPool')"
        color="primary"
        class="btn btn-primary pa-0"
        icon
        large
        dark
        @click="selectedTeam = {}">
        <v-icon dark color="white">
          add
        </v-icon>
      </v-btn>
    </div>
    <h4 v-show="!selectedTeam" class="text-center">
      <span>{{ $t('exoplatform.wallet.label.eligiblePoolsUsers') }}: <strong>{{ eligiblePoolsUsersCount }}</strong></span>
    </h4>
    <h4 v-show="!selectedTeam" class="text-center">
      <span>{{ $t('exoplatform.wallet.label.totalPoolsBudget') }}: <strong>{{ walletUtils.toFixed(poolsBudget) }} {{ symbol }}</strong></span>
    </h4>
    <v-container
      v-show="!selectedTeam"
      fluid
      grid-list-md>
      <v-data-iterator
        :items="teams"
        :items-per-page="1000"
        no-data-text=""
        hide-default-footer>
        <template v-slot:default="props">
          <v-row>
            <v-col
              v-for="item in props.items"
              :key="item.name"
              cols="12"
              sm="12"
              md="6"
              lg="4">
              <v-card :style="item.spacePrettyName && `background: url(/portal/rest/v1/social/spaces/${item.spacePrettyName}/banner)  0 0/100% auto no-repeat`" class="elevation-3">
                <v-card flat class="transparent">
                  <v-card-title class="pb-0">
                    <v-chip color="transparent">
                      <v-avatar v-if="item.spacePrettyName">
                        <img :src="`/portal/rest/v1/social/spaces/${item.spacePrettyName}/avatar`">
                      </v-avatar>
                      <h3 v-if="item.disabled" class="headline">
                        <del class="red--text">{{ item.name }}</del>
                      </h3>
                      <h3 v-else class="headline">
                        {{ item.name }}
                      </h3>
                    </v-chip>
                  </v-card-title>
                  <v-card-title class="pt-0">
                    <v-chip color="transparent">
                      <h4 v-if="item.description">
                        {{ item.description }}
                      </h4>
                      <h4 v-else>
                        <i>{{ $t('exoplatform.wallet.label.noDescription') }}</i>
                      </h4>
                    </v-chip>
                  </v-card-title>
                  <v-divider />
                  <v-list dense class="pb-0">
                    <v-list-item>
                      <v-list-item-content>
                        {{ $t('exoplatform.wallet.label.poolMembers') }}:
                      </v-list-item-content>
                      <v-list-item-content class="align-end">
                        {{ item.members ? item.members.length : 0 }}
                      </v-list-item-content>
                    </v-list-item>
                    <v-list-item v-if="item.rewardType === 'FIXED'">
                      <v-list-item-content>
                        {{ $t('exoplatform.wallet.label.fixedTotalbudget') }}:
                      </v-list-item-content>
                      <v-list-item-content class="align-end">
                        {{ item.budget }} {{ symbol }}
                      </v-list-item-content>
                    </v-list-item>
                    <v-list-item v-if="item.rewardType === 'FIXED_PER_MEMBER'">
                      <v-list-item-content>
                        {{ $t('exoplatform.wallet.label.fixedTotalbudget') }}:
                      </v-list-item-content>
                      <v-list-item-content class="align-end">
                        {{ Number(walletUtils.toFixed(item.budget)) }} {{ symbol }}
                      </v-list-item-content>
                    </v-list-item>
                    <v-list-item v-if="item.rewardType === 'COMPUTED'">
                      <v-list-item-content>
                        {{ $t('exoplatform.wallet.label.budget') }}:
                      </v-list-item-content>
                      <v-list-item-content class="align-end">
                        {{ $t('exoplatform.wallet.label.computed') }}
                      </v-list-item-content>
                    </v-list-item>
                    <v-list-item>
                      <v-flex class="align-start pe-1">
                        <v-divider />
                      </v-flex>
                      <v-flex
                        class="align-center">
                        <strong>
                          {{ periodDatesDisplay }}
                        </strong>
                      </v-flex>
                      <v-flex class="align-end ps-1">
                        <v-divider />
                      </v-flex>
                    </v-list-item>
                    <v-list-item>
                      <v-list-item-content>
                        {{ $t('exoplatform.wallet.label.eligiblePoolMembers') }}:
                      </v-list-item-content>
                      <v-list-item-content class="align-end">
                        {{ item.validMembersWallets ? item.validMembersWallets.length : 0 }} / {{ item.members ? item.members.length : 0 }}
                      </v-list-item-content>
                    </v-list-item>
                    <v-list-item>
                      <v-list-item-content>
                        {{ $t('exoplatform.wallet.label.budget') }}:
                      </v-list-item-content>
                      <v-list-item-content
                        v-if="!Number(item.computedBudget) || !item.validMembersWallets || !item.validMembersWallets.length"
                        class="align-end red--text">
                        <strong>
                          0 {{ symbol }}
                        </strong>
                      </v-list-item-content>
                      <v-list-item-content v-else class="align-end">
                        {{ Number(walletUtils.toFixed(item.computedBudget)) }} {{ symbol }}
                      </v-list-item-content>
                    </v-list-item>
                    <v-list-item>
                      <v-list-item-content>
                        {{ $t('exoplatform.wallet.label.budgetPerMember') }}:
                      </v-list-item-content>
                      <v-list-item-content
                        v-if="!Number(item.computedBudget) || !item.validMembersWallets || !item.validMembersWallets.length"
                        class="align-end red--text">
                        <strong>
                          0 {{ symbol }}
                        </strong>
                      </v-list-item-content>
                      <v-list-item-content v-else class="align-end">
                        {{ walletUtils.toFixed(Number(item.computedBudget) / item.validMembersWallets.length) }} {{ symbol }}
                      </v-list-item-content>
                    </v-list-item>
                  </v-list>
                </v-card>
                <v-card-actions>
                  <v-spacer />
                  <v-btn
                    v-if="item.id"
                    text
                    color="primary"
                    @click="selectedTeam = item">
                    {{ $t('exoplatform.wallet.button.edit') }}
                  </v-btn>
                  <v-btn
                    v-else
                    text
                    color="primary"
                    @click="selectedTeam = item">
                    {{ $t('exoplatform.wallet.button.view') }}
                  </v-btn>
                  <v-btn
                    v-if="item.id && item.disabled"
                    text
                    color="primary"
                    @click="disableTeam(item, false)">
                    {{ $t('exoplatform.wallet.button.enable') }}
                  </v-btn>
                  <v-btn
                    v-else-if="item.id"
                    text
                    color="primary"
                    @click="disableTeam(item, true)">
                    {{ $t('exoplatform.wallet.button.disable') }}
                  </v-btn>
                  <v-btn
                    v-if="item.id"
                    text
                    color="primary"
                    @click="
                      teamToDelete = item;
                      $refs.deleteTeamConfirm.open();
                    ">
                    {{ $t('exoplatform.wallet.button.delete') }}
                  </v-btn>
                  <v-spacer />
                </v-card-actions>
              </v-card>
            </v-col>
          </v-row>
        </template>
      </v-data-iterator>
    </v-container>
  </v-flex>
</template>

<script>

import {saveRewardTeam, removeRewardTeam} from '../../js/RewardServices.js';

export default {
  props: {
    teams: {
      type: Array,
      default: function() {
        return [];
      },
    },
    walletRewards: {
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
    periodDatesDisplay: {
      type: String,
      default: function() {
        return null;
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
      return this.walletRewards.reduce((sum, wallet) => sum + wallet.poolTokensToSend, 0);
    },
    deletePoolMessage() {
      return this.teamToDelete && `Would you like to delete pool <strong>${this.teamToDelete.name}</strong>`;
    },
    symbol() {
      return this.contractDetails && this.contractDetails.symbol ? this.contractDetails.symbol : '';
    },
    eligiblePoolsUsersCount() {
      return this.walletRewards.filter(wallet =>  wallet.poolTokensToSend > 0).length;
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
    teamSaved() {
      this.$emit('refresh-teams');
      this.selectedTeam = null;
    },
    disableTeam(team, disable) {
      if (team.id === 0) {
        return;
      }
      const teamToSave = Object.assign({}, team);
      teamToSave.disabled = disable;

      this.loading = true;
      delete teamToSave.validMembersWallets;
      return saveRewardTeam(teamToSave)
        .then(() => {
          this.$emit('refresh-teams');
        })
        .catch((e) => {
          console.error  ('Error saving pool', e);
          this.error = this.$t('exoplatform.wallet.error.errorSavingPool');
        })
        .finally(() => {
          this.loading = false;
        });
    },
    removeTeam(id) {
      if (id < 1) {
        console.error('Can\'t delete team with id', id);
      }
      removeRewardTeam(id)
        .then((status) => {
          if (status) {
            this.teamToDelete = null;
            this.$emit('refresh-teams');
          } else {
            this.error = this.$t('exoplatform.wallet.error.errorRemovingPool');
          }
        })
        .catch((e) => {
          console.error  ('Error getting team with id', id, e);
          this.error = this.$t('exoplatform.wallet.error.errorRemovingPool');
        });
    },
  },
};
</script>
