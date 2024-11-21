<!--
  This file is part of the Meeds project (https://meeds.io/).

  Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io

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
  <exo-drawer
    ref="drawer"
    v-model="drawer"
    :right="!$vuetify.rtl"
    eager
    @closed="close">
    <template #title>
      <span class="pb-2"> {{ $t('wallet.administration.rewardDetails.label.filterRewardList') }} </span>
    </template>
    <template v-if="drawer" #content>
      <form
        ref="rewardFilter"
        id="rewardFilter">
        <v-card-text>
          <span>{{ $t('wallet.administration.rewardDetails.label.contributor') }}</span>
          <v-flex class="user-suggester text-truncate">
            <exo-identity-suggester
              ref="granteeAttendeeAutoComplete"
              name="granteeAttendee"
              v-model="granteeAttendee"
              :search-options="searchOptions"
              :labels="granteeSuggesterLabels"
              include-users />
            <div v-if="grantees && grantees.length" class="identitySuggester no-border mt-0">
              <wallet-reward-grantee-attendee-item
                v-for="grantee in grantees"
                :key="grantee.identity.id"
                :attendee="grantee"
                @remove-attendee="removeGranteeAttendee" />
            </div>
          </v-flex>
        </v-card-text>
        <v-card-text>
          <span>{{ $t('wallet.administration.rewardDetails.label.status') }}</span>
          <v-radio-group v-model="status" class="mt-1">
            <v-radio :label="$t('wallet.administration.rewardDetails.label.all')" value="ALL" />
            <v-radio :label="$t('wallet.administration.rewardDetails.label.rewarded')" value="VALID" />
            <v-radio :label="$t('wallet.administration.rewardDetails.label.transactionError')" value="FAILED" />
            <v-radio :label="$t('wallet.administration.rewardDetails.label.toReward')" value="ESTIMATED" />
            <v-radio :label="$t('wallet.administration.rewardDetails.label.notEligible')" value="INELIGIBLE" />
          </v-radio-group>
        </v-card-text>
      </form>
    </template>
    <template #footer>
      <div class="VuetifyApp flex d-flex">
        <v-btn
          class="dark-grey-color px-0 hiddent-xs-only"
          text
          outlined
          @click="resetAndApply">
          <v-icon size="18" class="icon-default-color me-2">fa-redo</v-icon>
          {{ $t('wallet.administration.rewardDetails.label.resetFilter') }}
        </v-btn>
        <v-spacer />
        <div class="d-btn">
          <v-btn
            class="btn me-2"
            @click="cancel">
            <template>
              {{ $t('exoplatform.wallet.button.cancel') }}
            </template>
          </v-btn>
          <v-btn
            class="btn btn-primary"
            @click="confirm()">
            <template>
              {{ $t('exoplatform.wallet.button.confirm') }}
            </template>
          </v-btn>
        </div>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
export default {
  data: () => ({
    programs: [],
    programId: '0',
    drawer: false,
    loading: false,
    status: 'ALL',
    searchOptions: {
      currentUser: '',
    },
    granteeAttendee: null,
    grantees: [],
    granteesIds: [],
  }),
  computed: {
    granteeSuggesterLabels() {
      return {
        searchPlaceholder: this.$t('wallet.administration.label.filter.grantee.searchPlaceholder'),
        placeholder: this.$t('wallet.administration.label.filter.grantee.placeholder'),
        noDataLabel: this.$t('wallet.administration.label.filter.grantee.noDataLabel'),
      };
    },
  },
  watch: {
    granteeAttendee() {
      this.addSelectedGrantee(this.granteeAttendee);
    },
  },
  created() {
    this.$root.$on('reward-open-filter-drawer', this.open);
  },
  methods: {
    open() {
      this.$refs.drawer.open();
    },
    confirm(differ) {
      window.setTimeout(() => {
        this.$emit('selectionConfirmed', this.status, this.grantees);
        this.$refs.drawer.close();
      }, differ && 200 || 10);
    },
    reset() {
      this.grantees = [];
      this.granteeAttendee = null;
      this.status = 'ALL';
    },
    resetAndApply() {
      this.reset();
      this.$nextTick().then(() => this.confirm());
    },
    cancel() {
      this.$refs.drawer.close();
      this.reset();
    },
    removeGranteeAttendee(attendee) {
      const index = this.grantees.findIndex(addedAttendee => {
        return attendee.identity.remoteId === addedAttendee.identity.remoteId
            && attendee.identity.providerId === addedAttendee.identity.providerId;
      });
      if (index >= 0) {
        this.grantees.splice(index, 1);
        this.granteesIds.splice(index, 1);
      }
    },
    addSelectedGrantee(grantee) {
      if (!grantee) {
        if (this.$refs?.granteeAttendeeAutoComplete) {
          this.$nextTick(this.$refs.granteeAttendeeAutoComplete.$refs.selectAutoComplete.deleteCurrentItem);
        }
        return;
      }
      if (!this.grantees) {
        this.grantees = [];
      }
      const found = this.grantees?.find(g => {
        return g.identity.remoteId === grantee.remoteId
            && g.identity.providerId === grantee.providerId;
      });
      if (!found) {
        this.grantees.push({
          identity: grantee,
        });
      }
      this.granteeAttendee = null;
    },
  }
};
</script>