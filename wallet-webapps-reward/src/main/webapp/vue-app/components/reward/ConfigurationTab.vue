<template>
  <v-card flat>
    <v-card-text>
      <div class="text-xs-left rewardWalletConfiguration">
        <span>
          {{ $t('exoplatform.wallet.label.periodicity') }}:
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
                      {{ $t('exoplatform.wallet.label.rewardThreshold') }}:
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
                      :label="$t('exoplatform.wallet.label.usePools')" />
                  </div>
                  <div class="text-xs-left mt-4">
                    <div>
                      {{ $t('exoplatform.wallet.label.rewardBudgetBy') }}:
                    </div>
                    <v-flex class="ml-4">
                      <v-radio-group v-model="pluginSetting.budgetType">
                        <v-radio
                          :disabled="!configurationEditable"
                          :label="$t('exoplatform.wallet.label.rewardFixedBudget')"
                          value="FIXED" />
                        <v-flex v-if="pluginSetting.budgetType === 'FIXED'" class="rewardWalletConfiguration mb-2">
                          <v-text-field
                            v-model.number="pluginSetting.amount"
                            :disabled="!configurationEditable"
                            :placeholder="$t('exoplatform.wallet.label.rewardFixedBudgetPlaceholder')"
                            type="number"
                            class="pt-0 pb-0"
                            name="totalBudget" />
                        </v-flex>
                        <v-radio
                          :disabled="!configurationEditable"
                          :label="$t('exoplatform.wallet.label.rewardFixedBudgetPlaceholder')"
                          value="FIXED_PER_MEMBER" />
                        <v-flex v-if="pluginSetting.budgetType === 'FIXED_PER_MEMBER'" class="rewardWalletConfiguration mb-2">
                          <v-text-field
                            v-model.number="pluginSetting.amount"
                            :disabled="!configurationEditable"
                            :placeholder="$t('exoplatform.wallet.label.rewardFixedBudgetPerMemberPlaceholder')"
                            type="number"
                            class="pt-0 pb-0"
                            name="budgetPerMember" />
                        </v-flex>
                        <v-radio
                          :disabled="!configurationEditable"
                          :label="$t('exoplatform.wallet.label.rewardFixedBudgetPerPoint')"
                          value="FIXED_PER_POINT" />
                        <v-flex v-if="pluginSetting.budgetType === 'FIXED_PER_POINT'" class="rewardWalletConfiguration mb-2">
                          <v-text-field
                            v-model.number="pluginSetting.amount"
                            :disabled="!configurationEditable"
                            :placeholder="$t('exoplatform.wallet.label.rewardFixedBudgetPerPointPlaceholder')"
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
      <template v-if="configurationEditable">
        <v-btn
          :loading="loadingSettings"
          class="btn btn-primary px-2"
          dark
          @click="save">
          {{ $t('exoplatform.wallet.button.save') }}
        </v-btn>
        <v-btn
          class="btn mx-2 px-3"
          @click="init">
          {{ $t('exoplatform.wallet.button.cancel') }}
        </v-btn>
      </template>
      <v-btn
        v-else
        class="btn btn-primary ml-2"
        dark
        @click="configurationEditable = true">
        {{ $t('exoplatform.wallet.button.edit') }}
      </v-btn>
      <v-spacer />
    </v-card-actions>
  </v-card>
</template>

<script>
import {saveRewardSettings} from '../../js/RewardServices.js';

export default {
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
    selectedPeriodType() {
      if(!this.settingsToSave || !this.settingsToSave.periodType) {
        return this.periods[0];
      }
      return this.periods.find(period => period.value === this.settingsToSave.periodType);
    },
  },
  methods: {
    init() {
      this.configurationEditable = false;

      if (window.walletRewardSettings) {
        this.settingsToSave = JSON.parse(JSON.stringify(window.walletRewardSettings));
        this.$nextTick().then(() =>{
          this.settingsToSave.periodType = this.selectedPeriodType;
        });
      } else {
        this.settingsToSave = {};
      }
    },
    save() {
      const thiss = this;
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
            thiss.$emit('error', this.$t('exoplatform.wallet.error.errorSavingRewardSettings'));
          })
          .finally(() => {
            thiss.loadingSettings = false;
          });
      }, 200);
    },
  },
};
</script>
