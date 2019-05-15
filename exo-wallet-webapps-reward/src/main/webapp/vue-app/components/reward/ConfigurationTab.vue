<template>
  <v-card flat>
    <v-card-text>
      <div class="text-xs-left rewardWalletConfiguration">
        <span>
          Periodicity:
        </span>
        <div id="selectedPeriodType" class="selectBoxVuetifyParent v-input">
          <v-combobox
            v-model="settingsToSave.periodType"
            :disabled="!configurationEditable"
            :items="periods"
            attach="#selectedPeriodType"
            class="selectBoxVuetify"
            hide-no-data
            hide-selected
            return-masked-value
            small-chips />
        </div>
      </div>

      <div class="text-xs-left rewardWalletConfiguration">
        <span>
          Token
        </span>
        <div id="selectedContractAddress" class="selectBoxVuetifyParent v-input">
          <v-combobox
            v-model="settingsToSave.contractAddress"
            :disabled="!configurationEditable"
            :items="contracts"
            attach="#selectedContractAddress"
            item-value="address"
            item-text="name"
            class="selectBoxVuetify"
            hide-no-data
            hide-selected
            return-masked-value
            small-chips />
        </div>
      </div>
      <v-card v-if="settingsToSave && settingsToSave.pluginSettings" flat>
        <v-container
          fluid
          grid-list-lg
          class="pt-0 pb-0">
          <v-layout row wrap>
            <v-flex
              v-for="pluginSetting in settingsToSave.pluginSettings"
              :key="pluginSetting.pluginId"
              md6
              xs12>
              <v-card flat>
                <v-card-title primary-title>
                  <h4>
                    {{ pluginSetting.pluginId.toUpperCase() }}
                  </h4>
                </v-card-title>
                <v-card-text class="pt-0 pb-0">
                  <div class="text-xs-left rewardWalletConfiguration">
                    <span>
                      Minimal threshold to reward users:
                    </span>
                    <v-text-field
                      v-model.number="pluginSetting.threshold"
                      :disabled="!configurationEditable"
                      name="threshold"
                      class="input-text-center" />
                  </div>
                  <div v-if="pluginSetting.budgetType !== 'FIXED_PER_POINT'" class="text-xs-left rewardWalletConfiguration">
                    <v-checkbox
                      v-model="pluginSetting.usePools"
                      :disabled="!configurationEditable"
                      label="Use pools" />
                  </div>
                  <div class="text-xs-left mt-4">
                    <div>
                      The reward budget is set:
                    </div>
                    <v-flex class="ml-4">
                      <v-radio-group v-model="pluginSetting.budgetType">
                        <v-radio
                          :disabled="!configurationEditable"
                          value="FIXED"
                          label="By a fixed budget of" />
                        <v-flex v-if="pluginSetting.budgetType === 'FIXED'" class="rewardWalletConfiguration mb-2">
                          <v-text-field
                            v-model.number="pluginSetting.amount"
                            :disabled="!configurationEditable"
                            placeholder="Enter the fixed total budget"
                            type="number"
                            class="pt-0 pb-0"
                            name="totalBudget" />
                        </v-flex>
                        <v-radio
                          :disabled="!configurationEditable"
                          value="FIXED_PER_MEMBER"
                          label="By a fixed budget per eligible member" />
                        <v-flex v-if="pluginSetting.budgetType === 'FIXED_PER_MEMBER'" class="rewardWalletConfiguration mb-2">
                          <v-text-field
                            v-model.number="pluginSetting.amount"
                            :disabled="!configurationEditable"
                            placeholder="Enter the fixed budget per eligible member on period"
                            type="number"
                            class="pt-0 pb-0"
                            name="budgetPerMember" />
                        </v-flex>
                        <v-radio
                          :disabled="!configurationEditable"
                          value="FIXED_PER_POINT"
                          label="By a fixed budget per point" />
                        <v-flex v-if="pluginSetting.budgetType === 'FIXED_PER_POINT'" class="rewardWalletConfiguration mb-2">
                          <v-text-field
                            v-model.number="pluginSetting.amount"
                            :disabled="!configurationEditable"
                            placeholder="Enter the fixed budget per aquired point"
                            type="number"
                            class="pt-0 pb-0"
                            name="budgetPerPoint" />
                        </v-flex>
                      </v-radio-group>
                    </v-flex>
                  </div>
                </v-card-text>
              </v-card>
            </v-flex>
          </v-layout>
        </v-container>
      </v-card>
    </v-card-text>
    <v-card-actions>
      <v-spacer />
      <v-btn
        v-if="configurationEditable"
        :loading="loadingSettings"
        class="btn btn-primary ml-2"
        dark
        @click="save">
        Save
      </v-btn>
      <v-btn
        v-else
        class="btn btn-primary ml-2"
        dark
        @click="configurationEditable = true">
        Edit
      </v-btn>
      <v-spacer />
    </v-card-actions>
  </v-card>
</template>

<script>
import {saveRewardSettings} from '../../js/RewardServices.js';

export default {
  props: {
    contracts: {
      type: Array,
      default: function() {
        return [];
      },
    },
  },
  data() {
    return {
      loadingSettings: false,
      configurationEditable: false,
      settingsToSave: {},
      periods: [
        {
          text: 'Week',
          value: 'WEEK',
        },
        {
          text: 'Month',
          value: 'MONTH',
        },
        {
          text: 'Quarter',
          value: 'QUARTER',
        },
        {
          text: 'Semester',
          value: 'SEMESTER',
        },
        {
          text: 'Year',
          value: 'YEAR',
        },
      ],
    };
  },
  computed: {
    selectedContractDetails() {
      if(!this.settingsToSave || !this.settingsToSave.contractAddress) {
        return null;
      }
      const contractAddress = this.settingsToSave.contractAddress.toLowerCase();
      return this.contracts && this.contracts.find(contract => contract.address &&  contract.address.toLowerCase() === contractAddress);
    },
    selectedPeriodType() {
      if(!this.settingsToSave || !this.settingsToSave.periodType) {
        return this.periods[0];
      }
      return this.periods.find(period => period.value === this.settingsToSave.periodType);
    },
  },
  methods: {
    init() {
      if (window.walletRewardSettings) {
        this.settingsToSave = Object.assign({}, window.walletRewardSettings);
        this.$nextTick().then(() =>{
          this.settingsToSave.contractAddress = this.selectedContractDetails;
          this.settingsToSave.periodType = this.selectedPeriodType;
        });
      } else {
        this.settingsToSave = {};
      }
    },
    save() {
      const thiss = this;
      if(this.settingsToSave.contractAddress) {
        this.settingsToSave.contractAddress = this.settingsToSave.contractAddress.address;
      }
      if(this.settingsToSave.periodType) {
        this.settingsToSave.periodType = this.settingsToSave.periodType.value;
      }
      this.loadingSettings = true;
      // Wait to refresh UI for visual loading icon
      window.setTimeout(() => {
        saveRewardSettings(this.settingsToSave)
          .then((saved) => {
            if (saved) {
              this.configurationEditable = false;
              return this.$emit('saved');
            } else {
              throw new Error("Error saving settings");
            }
          })
          .catch((error) => {
            console.debug("Error while saving 'reward settings'", error);
            thiss.$emit('error', "Error while saving 'reward settings'");
          })
          .finally(() => {
            thiss.loadingSettings = false;
          });
      }, 200);
    },
  },
};
</script>
