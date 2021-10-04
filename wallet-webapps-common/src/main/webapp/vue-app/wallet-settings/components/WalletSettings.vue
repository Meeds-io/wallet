<template>
  <v-app>
    <v-card class="ma-4 border-radius" flat>
      <v-list two-line>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title class="title text-color">
              {{ $t('exoplatform.wallet.label.settings') }}
            </v-list-item-title>
          </v-list-item-content>
          <v-list-item-action>
            <v-btn
                small
                icon>
              <v-icon size="24" class="text-sub-title">
                {{ $vuetify.rtl && 'fa-caret-left' || 'fa-caret-right' }}
              </v-icon>
            </v-btn>
          </v-list-item-action>
        </v-list-item>
      </v-list>
    </v-card>
  </v-app>
</template>

<script>
export default {
  name: 'WalletSettings'
};
</script>
