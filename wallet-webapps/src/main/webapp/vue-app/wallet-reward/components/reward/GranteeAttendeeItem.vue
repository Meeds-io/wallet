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
  <v-chip
    close
    class="identitySuggesterItem me-4 mt-4"
    @click:close="$emit('remove-attendee', attendee)">
    <v-avatar left>
      <v-img :src="avatarUrl" />
    </v-avatar>
    <span class="text-truncate">
      {{ displayName }}
    </span>
  </v-chip>
</template>

<script>
export default {
  props: {
    attendee: {
      type: Object,
      default: () => ({}),
    },
  },
  computed: {
    avatarUrl() {
      const profile = this.attendee.identity && (this.attendee.identity.profile || this.attendee.identity.space);
      return profile && (profile.avatarUrl || profile.avatar || '/portal/rest/v1/social/users/default-image/avatar');
    },
    displayName() {
      const profile = this.attendee.identity && (this.attendee.identity.profile || this.attendee.identity.space);
      const fullName = profile && (profile.displayName || profile.fullname || profile.fullName);
      return this.isExternal ? `${fullName} (${this.$t('profile.External')})` : fullName;
    },
    isExternal() {
      const profile = this.attendee?.identity?.profile;
      return profile && (profile.dataEntity && profile.dataEntity.external === 'true' || profile.external);
    },
  },
};
</script>