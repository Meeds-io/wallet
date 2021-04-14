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
  <v-card class="walletSummaryBalance elevation-1">
    <v-card-title class="headline pb-1">
      {{ $t('exoplatform.wallet.label.currentBalance') }}
    </v-card-title>
    <v-card-title class="tokenBalance pt-0 display-1">
      {{ balance }} {{ contractDetails.symbol }}
    </v-card-title>
  </v-card>
</template>

<script>
export default {
  props: {
    wallet: {
      type: Object,
      default: function() {
        return null;
      },
    },
    contractDetails: {
      type: Object,
      default: function() {
        return {};
      },
    },
  },
  computed: {
    balance() {
      return (this.wallet && this.wallet.tokenBalance && this.walletUtils.toFixed(this.wallet.tokenBalance)) || 0;
    },
    symbol() {
      return (this.contractDetails && this.contractDetails.symbol) || '';
    },
  },
};
</script>
