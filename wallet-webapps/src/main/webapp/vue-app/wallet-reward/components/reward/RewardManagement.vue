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
  <div class="mx-5">
    <div class="pt-6 text-title">{{ $t('wallet.administration.rewardManagement') }}</div>
    <div class="pt-8 d-flex align-center justify-center">
      <v-icon
        color="orange darken-2"
        size="50">
        fa-money-bill
      </v-icon>
    </div>
    <div class="d-flex flex-column justify-center align-center pb-8 pt-3 col-sm-8 mx-auto">
      <p class="text-header mb-8">
        {{ $t('wallet.administration.rewardManagement.noContributionsYet') }}
      </p>
    </div>
  </div>
</template>