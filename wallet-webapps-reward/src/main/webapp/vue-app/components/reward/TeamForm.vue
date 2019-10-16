<template>
  <v-card class="pt-4" flat>
    <v-card-title v-if="error && String(error).trim() != '{}'" class="text-center">
      <div class="alert alert-error v-content">
        <i class="uiIconError"></i> {{ error }}
      </div>
    </v-card-title>
    <v-container
      grid-list-md
      class="pt-2"
      flat>
      <v-layout
        wrap
        class="rewardPoolForm"
        flat>
        <v-flex 
          v-if="!viewOnly"
          xs12
          sm6>
          <v-text-field
            v-model="name"
            :label="$t('exoplatform.wallet.label.poolName')"
            :placeholder="$t('exoplatform.wallet.label.poolNamePlaceholder')"
            name="name"
            required
            autofocus />
        </v-flex>

        <v-flex
          v-if="!viewOnly"
          xs12
          sm6>
          <v-text-field
            v-model="description"
            :label="$t('exoplatform.wallet.label.poolDescription')"
            :placeholder="$t('exoplatform.wallet.label.poolDescriptionPlaceholder')"
            name="description" />
        </v-flex>

        <v-flex
          v-if="!viewOnly"
          id="rewardTeamSpaceAutoComplete"
          class="contactAutoComplete"
          xs12
          sm6>
          <v-autocomplete
            ref="rewardTeamSpaceAutoComplete"
            v-model="rewardTeamSpace"
            :items="rewardTeamSpaceOptions"
            :items-per-page="1000"
            :loading="isLoadingSpaceSuggestions"
            :search-input.sync="rewardTeamSpaceSearchTerm"
            :label="$t('exoplatform.wallet.label.poolSpace')"
            :placeholder="$t('exoplatform.wallet.label.poolSpacePlaceholder')"
            attach="#rewardTeamSpaceAutoComplete"
            class="contactAutoComplete"
            content-class="contactAutoCompleteContent bigContactAutoComplete"
            max-width="100%"
            item-text="name"
            item-value="id"
            hide-details
            hide-selected
            chips
            cache-items
            dense
            flat>
            <template slot="no-data">
              <v-list-item>
                <v-list-item-title>
                  {{ $t('exoplatform.wallet.label.poolSpaceSearchPlaceholder') }}
                </v-list-item-title>
              </v-list-item>
            </template>

            <template slot="selection" slot-scope="{item, selected}">
              <v-chip
                v-if="item.error"
                :input-value="selected"
                class="autocompleteSelectedItem">
                <del>
                  <span>
                    {{ item.name }}
                  </span>
                </del>
              </v-chip>
              <v-chip
                v-else
                :input-value="selected"
                class="autocompleteSelectedItem">
                <span>
                  {{ item.name }}
                </span>
              </v-chip>
            </template>

            <template slot="item" slot-scope="{item}">
              <v-list-item-avatar
                v-if="item.avatar"
                size="20">
                <img :src="item.avatar">
              </v-list-item-avatar>
              <v-list-item-title v-text="item.name" />
            </template>
          </v-autocomplete>
        </v-flex>

        <address-auto-complete
          v-if="!viewOnly"
          ref="managerAutocomplete"
          :input-label="$t('exoplatform.wallet.label.poolManager')"
          :input-placeholder="$t('exoplatform.wallet.label.poolManagerPlaceholder')"
          :no-data-label="$t('exoplatform.wallet.label.poolManagerSearchPlaceholder')"
          no-address
          big-field
          class="xs12 sm6"
          @item-selected="manager = $event && $event.id" />

        <v-flex v-if="!viewOnly" xs12>
          <v-radio-group v-model="rewardType" :label="$t('exoplatform.wallet.label.poolMembers')">
            <v-radio value="COMPUTED" :label="$t('exoplatform.wallet.label.rewardPoolComputedBudget')" />
            <v-radio value="FIXED" :label="$t('exoplatform.wallet.label.rewardPoolFixedBudget')" />
            <v-flex xs12 sm6>
              <v-text-field
                v-if="rewardType === 'FIXED'"
                v-model="budget"
                :placeholder="$t('exoplatform.wallet.label.rewardFixedBudgetPlaceholder')"
                type="number"
                class="pt-0 pb-0"
                name="budget" />
            </v-flex>
            <v-radio value="FIXED_PER_MEMBER" label="By a fixed budget per eligible member (not retained from global budget)" />
            <v-flex
              v-if="rewardType === 'FIXED_PER_MEMBER'"
              xs12
              sm6>
              <v-text-field
                v-model="budget"
                :placeholder="$t('exoplatform.wallet.label.rewardFixedBudgetPerMemberPlaceholder')"
                type="number"
                class="pt-0 pb-0"
                name="budget" />
            </v-flex>
          </v-radio-group>
        </v-flex>

        <v-flex xs12>
          <address-auto-complete
            v-if="!viewOnly"
            ref="memberAutocomplete"
            :ignore-items="members"
            :input-label="$t('exoplatform.wallet.label.poolMembers')"
            :input-placeholder="$t('exoplatform.wallet.label.poolMemberPlaceholder')"
            :no-data-label="$t('exoplatform.wallet.label.poolMemberSearchPlaceholder')"
            no-address
            big-field
            @item-selected="addMember($event)" />
          <v-data-table
            :items="membersObjects"
            :items-per-page="1000"
            item-key="id"
            class="elevation-1 mt-2"
            sortable
            hide-default-footer>
            <template slot="no-data">
              <tr>
                <td colspan="3" class="text-center">
                  {{ $t('exoplatform.wallet.label.noMembersInPool') }}
                </td>
              </tr>
            </template>
            <!-- Without slot-scope, the template isn't displayed -->
            <!-- eslint-disable-next-line vue/no-unused-vars -->
            <template slot="headers" slot-scope="props">
              <tr>
                <th colspan="2" class="text-center">
                  {{ $t('exoplatform.wallet.label.name') }}
                </th>
                <th class="text-right">
                  <v-btn
                    v-if="!viewOnly"
                    :title="$t('exoplatform.wallet.button.deleteAll')"
                    icon
                    @click="members = []">
                    <v-icon>
                      delete
                    </v-icon>
                  </v-btn>
                </th>
              </tr>
            </template>
            <template slot="item" slot-scope="props">
              <tr>
                <td class="text-left">
                  <v-avatar size="36px">
                    <img :src="props.item.avatar" onerror="this.src = '/eXoSkin/skin/images/system/SpaceAvtDefault.png'">
                  </v-avatar>
                </td>
                <td class="text-center">
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
                    display-no-address />
                </td>
                <td class="text-right">
                  <v-btn
                    v-if="!viewOnly"
                    :title="$t('exoplatform.wallet.button.delete')"
                    icon
                    @click="deleteMember(props.item)">
                    <v-icon>
                      delete
                    </v-icon>
                  </v-btn>
                </td>
              </tr>
            </template>
          </v-data-table>
        </v-flex>
      </v-layout>
    </v-container>
    <v-card-actions>
      <v-spacer />
      <v-btn
        v-if="!viewOnly"
        :loading="loading"
        class="btn btn-primary mr-1"
        dark
        @click="save">
        {{ $t('exoplatform.wallet.button.save') }}
      </v-btn>
      <v-btn
        class="btn"
        color="white"
        @click="$emit('close')">
        {{ $t('exoplatform.wallet.button.close') }}
      </v-btn>
      <v-spacer />
    </v-card-actions>
  </v-card>
</template>

<script>
import {saveRewardTeam} from '../../js/RewardServices.js';

export default {
  props: {
    team: {
      type: Object,
      default: function() {
        return null;
      },
    },
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
  },
  data: () => ({
    loading: false,
    error: null,
    id: null,
    name: '',
    description: '',
    rewardType: 'COMPUTED',
    budget: '',
    computedBudget: 0,
    memberSelection: null,
    manager: null,
    managerObject: null,
    members: [],
    membersObjects: [],
    rewardTeamSpace: null,
    rewardTeamSpaceId: null,
    rewardTeamSpaceOptions: [],
    rewardTeamSpaceSearchTerm: null,
    isLoadingSpaceSuggestions: false,
  }),
  computed: {
    viewOnly() {
      return this.team && this.team.noTeam;
    }
  },
  watch: {
    team() {
      if (this.team) {
        this.init();
      }
    },
    rewardTeamSpace(newValue, oldValue) {
      if (oldValue) {
        this.rewardTeamSpaceSearchTerm = null;
        // A hack to close on select
        // See https://www.reddit.com/r/vuetifyjs/comments/819h8u/how_to_close_a_multiple_autocomplete_vselect/
        this.$refs.rewardTeamSpaceAutoComplete.isFocused = false;
      }
      this.rewardTeamSpaceId = null;
      if (this.rewardTeamSpaceOptions && this.rewardTeamSpaceOptions.length) {
        const selectedObject = this.rewardTeamSpaceOptions.find((space) => space.name === this.rewardTeamSpace || space.id === this.rewardTeamSpace);
        this.rewardTeamSpaceId = selectedObject && selectedObject.technicalId;
        if (selectedObject && selectedObject.members && selectedObject.members.length) {
          this.members = selectedObject.members && selectedObject.members.slice();
        }
      }
    },
    rewardTeamSpaceSearchTerm() {
      if (this.rewardTeamSpaceSearchTerm) {
        this.isLoadingSuggestions = true;
        this.addressRegistry.searchSpaces(this.rewardTeamSpaceSearchTerm, true)
          .then((items) => {
            if (items) {
              this.rewardTeamSpaceOptions = items;
            } else {
              this.rewardTeamSpaceOptions = [];
            }
            this.isLoadingSpaceSuggestions = false;
          })
          .catch((e) => {
            console.debug('searchSpaces method - error', e);
            this.isLoadingSpaceSuggestions = false;
          });
      }
    },
    manager() {
      if (!this.manager || !this.manager.length) {
        this.managerObject = null;
        return;
      }
      const managerObject = this.membersObjects && this.membersObjects.find((wallet) => wallet.id === this.manager && wallet.type === 'user');
      if (managerObject) {
        this.managerObject = managerObject;
      } else {
        const walletReward = this.walletRewards.find((walletReward) => walletReward.wallet && walletReward.wallet.id === this.manager && walletReward.wallet.type === 'user');
        if (walletReward) {
          this.managerObject = Object.assign({identityId: walletReward.wallet.technicalId}, walletReward.wallet);
        } else {
          this.addressRegistry.searchWalletByTypeAndId(this.manager, 'user').then((userDetails) => {
            if (userDetails) {
              this.managerObject = Object.assign({identityId: userDetails.technicalId}, userDetails);
            }
          });
        }
      }
    },
    members() {
      const oldMemberObjects = this.membersObjects;
      this.membersObjects = [];
      if (this.members && this.members.length) {
        this.members.forEach((memberId) => {
          const memberObject = oldMemberObjects && oldMemberObjects.find((wallet) => wallet.id === memberId && wallet.type === 'user');
          if (memberObject) {
            this.membersObjects.push(memberObject);
          } else {
            const walletReward = this.walletRewards.find((walletReward) => walletReward.wallet && walletReward.wallet.id === memberId && walletReward.wallet.type === 'user');
            if (walletReward) {
              this.membersObjects.push(Object.assign({identityId: walletReward.wallet.technicalId}, walletReward.wallet));
            } else {
              this.addressRegistry.searchWalletByTypeAndId(memberId, 'user').then((userDetails) => {
                if (userDetails) {
                  this.membersObjects.push(Object.assign({identityId: userDetails.technicalId}, userDetails));
                }
              });
            }
          }
        });
      }
    },
  },
  methods: {
    init() {
      if (this.$refs && this.$refs.managerAutocomplete) {
        this.$refs.managerAutocomplete.clear();
      }
      if (this.$refs && this.$refs.memberAutocomplete) {
        this.$refs.memberAutocomplete.clear();
      }

      this.error = null;
      this.loading = false;
      this.id = this.team && this.team.id;
      this.name = (this.team && this.team.name) || '';
      this.description = (this.team && this.team.description) || '';
      this.rewardType = (this.team && this.team.rewardType) || 'COMPUTED';
      this.budget = (this.team && this.team.budget) || '';
      this.computedBudget = (this.team && this.team.computedBudget) || '0';
      this.manager = null;
      this.members = [];

      this.rewardTeamSpace = null;
      this.rewardTeamSpaceId = null;
      this.rewardTeamSpaceSearchTerm = null;
      this.isLoadingSpaceSuggestions = false;

      if (this.team) {
        if (this.team.spacePrettyName && this.team.spaceId) {
          this.addressRegistry.searchSpaces(this.team.spacePrettyName).then((items) => {
            if (items) {
              this.rewardTeamSpaceOptions = items;
            } else {
              this.rewardTeamSpaceOptions = [];
            }
            if (!this.rewardTeamSpaceOptions.find((item) => item.id === this.team.spaceId)) {
              this.rewardTeamSpaceOptions.push({id: this.team.spacePrettyName, technicalId: this.team.spaceId, name: this.team.spacePrettyName});
            }
            this.rewardTeamSpace = this.team.spacePrettyName;
            this.rewardTeamSpaceId = this.team.spaceId;
          });
        }

        this.manager = this.team.manager && this.team.manager.id;
        if (this.$refs && this.$refs.managerAutocomplete) {
          this.$refs.managerAutocomplete.selectItem(this.manager, this.manager && 'user');
        }

        if (this.team.members && this.team.members.length) {
          this.members = this.team.members.map((memberObject) => memberObject.id);
        } else {
          this.members = [];
        }
      }
    },
    deleteMember(poolMember) {
      if (poolMember) {
        const poolMemberIndex = this.members.indexOf(poolMember.id);
        if (poolMemberIndex >= 0) {
          this.members.splice(poolMemberIndex, 1);
        }
      }
    },
    addMember(memberObject) {
      if (!memberObject || !memberObject.id) {
        return;
      }
      this.error = '';

      // Check if member already exists in other pool
      let otherDuplicatedTeams = this.teams || [];
      otherDuplicatedTeams = otherDuplicatedTeams.filter(team => team.id && team.id !== this.team.id && team.members && team.members.filter(member => member.id === memberObject.id).length);
      if (otherDuplicatedTeams && otherDuplicatedTeams.length) {
        this.error = this.$t('exoplatform.wallet.warning.memberDuplicatedInOtherPool', {0: memberObject.name, 1: otherDuplicatedTeams[0].name});
      } else {
        if (this.members) {
          if (this.members.indexOf(memberObject.id) < 0) {
            this.members.push(memberObject.id);
          }
        } else {
          this.members = [memberObject.id];
        }
      }

      this.$refs.memberAutocomplete.selectItem(null);
    },
    save() {
      this.error = null;
      if (!this.name) {
        this.error = this.$t('exoplatform.wallet.warning.poolNameIsMandatory');
        return;
      }

      // Check team name
      if (this.teams && this.teams.length && (!this.id || this.name !== this.team.name)) {
        let nameAlreadyExists = false;
        this.teams.forEach((team) => {
          nameAlreadyExists = nameAlreadyExists || (team.name.toLowerCase() === this.name.toLowerCase() && this.id !== team.id);
        });
        if (nameAlreadyExists) {
          this.error = this.$t('exoplatform.wallet.warning.poolNameAlreadyExists');
          return;
        }
      }

      this.loading = true;
      try {
        const members = this.membersObjects && this.membersObjects.map((memberObject) => Object({id: memberObject.id, identityId: memberObject.identityId}));
        if (this.team && this.team.members && this.team.members.length) {
          members.forEach((memberObject) => {
            const oldMember = this.team.members.find((oldMemberObject) => String(oldMemberObject.identityId) === String(memberObject.identityId));
            if (oldMember) {
              memberObject.technicalId = oldMember.technicalId;
            }
          });
        }
        const team = {
          id: this.id,
          name: this.name,
          description: this.description,
          rewardType: this.rewardType,
          budget: (this.budget && Number(this.budget)) || 0,
          spacePrettyName: this.rewardTeamSpace,
          spaceId: this.rewardTeamSpaceId,
          members: members,
          manager: {
            id: this.manager,
            identityId: this.managerObject && this.managerObject.identityId,
          },
        };

        return saveRewardTeam(team)
          .then((addedTeam) => {
            if (addedTeam) {
              this.$emit('saved', addedTeam);
            } else {
              console.debug('Error saving pool, response code is NOK');
              this.error = this.$t('exoplatform.wallet.error.errorSavingPool');
            }
          })
          .catch((e) => {
            console.debug('Error saving pool', e);
            this.error = this.$t('exoplatform.wallet.error.errorSavingPool');
          })
          .finally(() => {
            this.loading = false;
          });
      } catch (e) {
        console.debug('Error saving pool', e);
        this.error = this.$t('exoplatform.wallet.error.errorSavingPool');
        this.loading = false;
      }
    },
  },
};
</script>
