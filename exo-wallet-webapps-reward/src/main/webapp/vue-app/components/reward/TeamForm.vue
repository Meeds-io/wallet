<template>
  <v-card class="elevation-12 pt-4">
    <v-card-title v-if="error && String(error).trim() != '{}'" class="text-xs-center">
      <div class="alert alert-error v-content">
        <i class="uiIconError"></i> {{ error }}
      </div>
    </v-card-title>
    <v-container grid-list-md class="pt-2">
      <v-layout wrap class="rewardPoolForm">
        <v-flex 
          v-if="!viewOnly"
          xs12
          sm6>
          <v-text-field
            v-model="name"
            label="Pool name *"
            placeholder="Enter pool name"
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
            label="Pool description"
            placeholder="Enter pool description"
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
            :loading="isLoadingSpaceSuggestions"
            :search-input.sync="rewardTeamSpaceSearchTerm"
            attach="#rewardTeamSpaceAutoComplete"
            label="Pool space (optional, add its members in pool and display avatar)"
            class="contactAutoComplete"
            placeholder="Start typing to Search a space"
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
              <v-list-tile>
                <v-list-tile-title>
                  Search for a <strong>
                    Space
                  </strong>
                </v-list-tile-title>
              </v-list-tile>
            </template>

            <template slot="selection" slot-scope="{item, selected}">
              <v-chip
                v-if="item.error"
                :selected="selected"
                class="autocompleteSelectedItem">
                <del>
                  <span>
                    {{ item.name }}
                  </span>
                </del>
              </v-chip>
              <v-chip
                v-else
                :selected="selected"
                class="autocompleteSelectedItem">
                <span>
                  {{ item.name }}
                </span>
              </v-chip>
            </template>

            <template slot="item" slot-scope="{item}">
              <v-list-tile-avatar
                v-if="item.avatar"
                tile
                size="20">
                <img :src="item.avatar">
              </v-list-tile-avatar>
              <v-list-tile-title v-text="item.name" />
            </template>
          </v-autocomplete>
        </v-flex>

        <address-auto-complete
          v-if="!viewOnly"
          ref="managerAutocomplete"
          input-label="Pool manager"
          input-placeholder="Select a pool manager"
          no-data-label="Search for a user"
          no-address
          big-field
          class="xs12 sm6"
          @item-selected="manager = $event && $event.id" />

        <v-flex v-if="!viewOnly" xs12>
          <v-radio-group v-model="rewardType" label="Reward pool members">
            <v-radio value="COMPUTED" label="By computing pool reward from total budget" />
            <v-radio value="FIXED" label="By a total fixed budget (not retained from global budget)" />
            <v-flex xs12 sm6>
              <v-text-field
                v-if="rewardType === 'FIXED'"
                v-model="budget"
                placeholder="Enter the pool fixed budget"
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
                v-model="budgetPerMember"
                placeholder="Enter the fixed budget per pool member"
                type="number"
                class="pt-0 pb-0"
                name="budgetPerMember" />
            </v-flex>
          </v-radio-group>
        </v-flex>

        <v-flex xs12>
          <address-auto-complete
            v-if="!viewOnly"
            ref="memberAutocomplete"
            :ignore-items="members"
            input-label="Pool members"
            input-placeholder="Add new member"
            no-data-label="Search for a user"
            no-address
            big-field
            @item-selected="addMember($event)" />
          <v-data-table
            :items="membersObjects"
            item-key="id"
            class="elevation-1 mt-2"
            sortable>
            <template slot="no-data">
              <tr>
                <td colspan="3" class="text-xs-center">
                  No pool members
                </td>
              </tr>
            </template>
            <!-- Without slot-scope, the template isn't displayed -->
            <!-- eslint-disable-next-line vue/no-unused-vars -->
            <template slot="headers" slot-scope="props">
              <tr>
                <th colspan="2" class="text-xs-center">
                  Name
                </th>
                <th class="text-xs-right">
                  <v-btn
                    v-if="!viewOnly"
                    icon
                    title="Delete all"
                    @click="members = []">
                    <v-icon>
                      delete
                    </v-icon>
                  </v-btn>
                </th>
              </tr>
            </template>
            <template slot="items" slot-scope="props">
              <tr>
                <td class="text-xs-left">
                  <v-avatar size="36px">
                    <img :src="props.item.avatar" onerror="this.src = '/eXoSkin/skin/images/system/SpaceAvtDefault.png'">
                  </v-avatar>
                </td>
                <td class="text-xs-center">
                  <profile-chip
                    :address="props.item.address"
                    :profile-id="props.item.id"
                    :profile-technical-id="props.item.technicalId"
                    :space-id="props.item.spaceId"
                    :profile-type="props.item.type"
                    :display-name="props.item.name"
                    :enabled="props.item.enabled"
                    :disapproved="props.item.disapproved"
                    :deleted-user="props.item.deletedUser"
                    :disabled-user="props.item.disabledUser"
                    :avatar="props.item.avatar"
                    display-no-address />
                </td>
                <td class="text-xs-right">
                  <v-btn
                    v-if="!viewOnly"
                    icon
                    title="Delete"
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
        Save
      </v-btn>
      <v-btn
        class="btn"
        color="white"
        @click="$emit('close')">
        Close
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
    budgetPerMember: '',
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
      this.budgetPerMember = (this.team && this.team.rewardPerMember) || '';
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
      if (this.members) {
        if (this.members.indexOf(memberObject.id) < 0) {
          this.members.push(memberObject.id);
        }
      } else {
        this.members = [memberObject.id];
      }
      this.$refs.memberAutocomplete.selectItem(null);
    },
    save() {
      this.error = null;
      if (!this.name) {
        this.error = 'Pool name is mandatory';
        return;
      }

      if (this.teams && this.teams.length) {
        let nameAlreadyExists = false;
        this.teams.forEach((team) => {
          nameAlreadyExists = nameAlreadyExists || team.name.toLowerCase() === this.name.toLowerCase();
        });
        if (nameAlreadyExists) {
          this.error = 'Pool name already exists';
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
          budget: this.budget && this.rewardType === 'FIXED' ? Number(this.budget) : 0,
          rewardPerMember: this.budgetPerMember && this.rewardType === 'FIXED_PER_MEMBER' ? Number(this.budgetPerMember) : 0,
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
              this.error = 'Error saving pool, please contact your administrator.';
            }
          })
          .catch((e) => {
            console.debug('Error saving pool', e);
            this.error = 'Error saving pool, please contact your administrator.';
          })
          .finally(() => {
            this.loading = false;
          });
      } catch (e) {
        console.debug('Error saving pool', e);
        this.error = 'Error saving pool, please contact your administrator.';
        this.loading = false;
      }
    },
  },
};
</script>
