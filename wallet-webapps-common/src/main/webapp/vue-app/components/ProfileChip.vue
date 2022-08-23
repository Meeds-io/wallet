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
  <a
    v-if="profileId"
    :id="id"
    :title="address"
    :href="url"
    rel="nofollow"
    target="_blank">
    <template v-if="noStatus">
      {{ displayName }}
    </template>
    <template v-else-if="deletedUser">
      <del class="red--text">{{ displayName }}</del> ({{ $t('exoplatform.wallet.label.deletedIdentity') }})
    </template>
    <template v-else-if="disabledUser">
      <del class="red--text">{{ displayName }}</del> ({{ $t('exoplatform.wallet.label.disabledUser') }})
    </template>
    <template v-else-if="displayNoAddress && !address">
      <del class="red--text">{{ displayName }}</del> ({{ $t('exoplatform.wallet.label.noWallet') }})
    </template>
    <template v-else-if="!enabled">
      <del class="red--text">{{ displayName }}</del> ({{ $t('exoplatform.wallet.label.disabledWallet') }})
    </template>
    <template v-else-if="disabledInRewardPool">
      {{ displayName }} <span class="red--text">({{ $t('exoplatform.wallet.label.disabledPool') }})</span>
    </template>
    <template v-else-if="initializationState !== 'INITIALIZED' && initializationState !== 'MODIFIED' && initializationState !== 'NEW'">
      {{ displayName }} <span class="orange--text">({{ $t('exoplatform.wallet.label.notInitialized') }})</span>
    </template>
    <template v-else>
      {{ displayName }}
    </template>
  </a>
  <code v-else-if="displayName">
    <template v-if="enabled">
      {{ displayName }}
    </template>
    <span v-else>
      <del class="red--text">{{ displayName }}</del> ({{ $t('exoplatform.wallet.label.disabledWallet') }})
    </span>
  </code>
  <wallet-reward-wallet-address
    v-else
    :value="address"
    display-label />
</template>

<script>
export default {
  props: {
    profileId: {
      type: String,
      default: function() {
        return null;
      },
    },
    profileType: {
      type: String,
      default: function() {
        return null;
      },
    },
    displayName: {
      type: String,
      default: function() {
        return null;
      },
    },
    address: {
      type: String,
      default: function() {
        return null;
      },
    },
    enabled: {
      type: Boolean,
      default: function() {
        return true;
      },
    },
    disabledInRewardPool: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    displayNoAddress: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    deletedUser: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    disabledUser: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    noStatus: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    initializationState: {
      type: String,
      default: function() {
        return 'INITIALIZED';
      },
    },
  },
  data() {
    return {
      id: `chip${parseInt(Math.random() * 10000)
        .toString()
        .toString()}`,
    };
  },
  computed: {
    url() {
      if (!this.profileType || this.profileType === 'user') {
        return `${eXo.env.portal.context}/${eXo.env.portal.portalName}/profile/${this.profileId}`;
      } else if (this.profileType === 'space') {
        return `${eXo.env.portal.context}/g/:spaces:${this.profileId}/`;
      }
      return '#';
    },
  },
};
</script>
