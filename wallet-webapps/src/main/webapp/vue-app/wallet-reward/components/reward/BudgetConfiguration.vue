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
  <v-card
    class="border-radius border-color my-5 me-5"
    width="60%"
    flat>
    <div class="d-flex flex-column flex-grow-1">
      <div class="text-header text-wrap px-4 pb-0 pt-3 mt-auto mb-2">
        {{ $t('wallet.administration.budgetConfiguration') }}
      </div>
      <v-spacer />
      <div class="d-flex flex-column align-center justify-center full-width full-height pt-5">
        <v-icon color="orange darken-2" size="50">fas fa-coins</v-icon>
        <span class="mt-5">{{ $t('wallet.administration.budgetConfiguration.setUp') }}</span>
      </div>
      <v-btn
        class="btn btn-primary align-self-center my-4 px-2">
        <span class="mx-2">
          {{ $t('wallet.administration.budgetConfiguration.setBudget') }}
        </span>
      </v-btn>
    </div>
  </v-card>
</template>