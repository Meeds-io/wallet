<template>
  <v-flex class="transactionsList">
    <v-card class="card--flex-toolbar" flat>
      <div v-if="error && !loading" class="alert alert-error">
        <i class="uiIconError"></i>{{ error }}
      </div>

      <div v-if="loading" class="grey--text">
        {{ $t('exoplatform.wallet.message.loadingRecentTransactions') }}...
      </div>
      <v-progress-linear
        v-if="loading"
        indeterminate
        color="primary"
        class="mb-0 mt-0" />

      <v-divider />
      <v-expansion-panels
        v-if="Object.keys(sortedTransactions).length"
        accordion
        focusable>
        <v-expansion-panel
          v-for="(item, index) in sortedTransactions"
          :id="`transaction-${item.hash}`"
          :key="index"
          :value="item.selected">
          <v-expansion-panel-header
            :expand-icon="false"
            hide-actions
            class="border-box-sizing px-2 py-0">
            <v-list
              :class="item.selected && 'blue lighten-5'"
              two-line
              ripple
              class="pt-0 pb-0">
              <v-list-item
                :key="item.hash"
                class="transactionDetailItem autoHeight"
                ripple>
                <v-progress-circular
                  v-if="item.pending"
                  indeterminate
                  color="primary"
                  class="mr-4" />
                <v-list-item-avatar v-else-if="item.error" :title="item.error">
                  <v-icon color="red">
                    warning
                  </v-icon>
                </v-list-item-avatar>
                <v-list-item-avatar v-else-if="item.adminIcon">
                  <v-icon color="grey">
                    fa-cog
                  </v-icon>
                </v-list-item-avatar>
                <v-list-item-avatar v-else-if="item.isReceiver">
                  <v-icon color="green">
                    fa-arrow-down
                  </v-icon>
                </v-list-item-avatar>
                <v-list-item-avatar v-else>
                  <v-icon color="red">
                    fa-arrow-up
                  </v-icon>
                </v-list-item-avatar>
  
                <v-list-item-content class="transactionDetailContent">
                  <v-list-item-title v-if="item.isContractCreation">
                    <span>
                      {{ $t('exoplatform.wallet.label.createdContract') }}
                    </span>
                    <wallet-address
                      :value="item.contractAddress"
                      :name="item.contractName"
                      display-label />
                  </v-list-item-title>
  
                  <v-list-item-title v-else-if="item.type === 'ether'">
                    <profile-chip
                      v-if="displayFullTransaction"
                      :address="item.toAddress"
                      :profile-id="item.toUsername"
                      :profile-technical-id="item.toTechnicalId"
                      :space-id="item.toSpaceId"
                      :profile-type="item.toType"
                      :display-name="item.toDisplayName"
                      :avatar="item.toAvatar" />
                    <profile-chip
                      v-else-if="displayFullTransaction"
                      :address="item.fromAddress"
                      :profile-id="item.fromUsername"
                      :profile-technical-id="item.fromTechnicalId"
                      :space-id="item.fromSpaceId"
                      :profile-type="item.fromType"
                      :display-name="item.fromDisplayName"
                      :avatar="item.fromAvatar" />
  
                    <span v-if="item.isReceiver">
                      {{ $t('exoplatform.wallet.label.receivedFrom') }}
                    </span>
                    <span v-else>
                      {{ $t('exoplatform.wallet.label.sentTo') }}
                    </span>
  
                    <profile-chip
                      v-if="item.isReceiver"
                      :address="item.fromAddress"
                      :profile-id="item.fromUsername"
                      :profile-technical-id="item.fromTechnicalId"
                      :space-id="item.fromSpaceId"
                      :profile-type="item.fromType"
                      :display-name="item.fromDisplayName"
                      :avatar="item.fromAvatar" />
  
                    <profile-chip
                      v-else
                      :address="item.toAddress"
                      :profile-id="item.toUsername"
                      :profile-technical-id="item.toTechnicalId"
                      :space-id="item.toSpaceId"
                      :profile-type="item.toType"
                      :display-name="item.toDisplayName"
                      :avatar="item.toAvatar" />
                  </v-list-item-title>
  
                  <v-list-item-title v-else-if="item.contractMethodName === 'transferFrom'">
                    <span>
                      {{ $t('exoplatform.wallet.label.sentTo') }}
                    </span>
                    <profile-chip
                      :address="item.toAddress"
                      :profile-id="item.toUsername"
                      :profile-technical-id="item.toTechnicalId"
                      :space-id="item.toSpaceId"
                      :profile-type="item.toType"
                      :display-name="item.toDisplayName"
                      :avatar="item.toAvatar" />
                    <span>
                      {{ $t('exoplatform.wallet.label.by') }}
                    </span>
                    <profile-chip
                      :address="item.byAddress"
                      :profile-id="item.byUsername"
                      :profile-technical-id="item.byTechnicalId"
                      :space-id="item.bySpaceId"
                      :profile-type="item.byType"
                      :display-name="item.byDisplayName"
                      :avatar="item.byAvatar" />
                    <span>
                      {{ $t('exoplatform.wallet.label.onBehalfOf') }}
                    </span>
                    <profile-chip
                      :address="item.fromAddress"
                      :profile-id="item.fromUsername"
                      :profile-technical-id="item.fromTechnicalId"
                      :space-id="item.fromSpaceId"
                      :profile-type="item.fromType"
                      :display-name="item.fromDisplayName"
                      :avatar="item.fromAvatar" />
                  </v-list-item-title>
  
                  <v-list-item-title v-else-if="(item.contractMethodName === 'transfer' || item.contractMethodName === 'approve')">
                    <profile-chip
                      v-if="displayFullTransaction && item.isReceiver"
                      :address="item.toAddress"
                      :profile-id="item.toUsername"
                      :profile-technical-id="item.toTechnicalId"
                      :space-id="item.toSpaceId"
                      :profile-type="item.toType"
                      :display-name="item.toDisplayName"
                      :avatar="item.toAvatar" />
                    <profile-chip
                      v-else-if="displayFullTransaction"
                      :address="item.fromAddress"
                      :profile-id="item.fromUsername"
                      :profile-technical-id="item.fromTechnicalId"
                      :space-id="item.fromSpaceId"
                      :profile-type="item.fromType"
                      :display-name="item.fromDisplayName"
                      :avatar="item.fromAvatar" />
  
                    <span v-if="item.contractMethodName === 'transfer' && item.isReceiver">
                      {{ $t('exoplatform.wallet.label.receivedFrom') }}
                    </span>
                    <span v-else-if="item.contractMethodName === 'transfer' && !item.isReceiver">
                      {{ $t('exoplatform.wallet.label.sentTo') }}
                    </span>
                    <span v-else-if="item.contractMethodName === 'approve' && item.isReceiver">
                      {{ $t('exoplatform.wallet.label.delegatedFrom') }}
                    </span>
                    <span v-else>
                      {{ $t('exoplatform.wallet.label.delegatedTo') }}
                    </span>
  
                    <profile-chip
                      v-if="item.isReceiver"
                      :address="item.fromAddress"
                      :profile-id="item.fromUsername"
                      :profile-technical-id="item.fromTechnicalId"
                      :space-id="item.fromSpaceId"
                      :profile-type="item.fromType"
                      :display-name="item.fromDisplayName"
                      :avatar="item.fromAvatar" />
                    <profile-chip
                      v-else
                      :address="item.toAddress"
                      :profile-id="item.toUsername"
                      :profile-technical-id="item.toTechnicalId"
                      :space-id="item.toSpaceId"
                      :profile-type="item.toType"
                      :display-name="item.toDisplayName"
                      :avatar="item.toAvatar" />
                  </v-list-item-title>
  
                  <v-list-item-title v-else-if="item.contractMethodName === 'addAdmin'">
                    <profile-chip
                      v-if="displayFullTransaction"
                      :address="item.fromAddress"
                      :profile-id="item.fromUsername"
                      :profile-technical-id="item.fromTechnicalId"
                      :space-id="item.fromSpaceId"
                      :profile-type="item.fromType"
                      :display-name="item.fromDisplayName"
                      :avatar="item.fromAvatar" />
                    <span>
                      {{ $t('exoplatform.wallet.label.added') }}
                    </span>
                    <profile-chip
                      :address="item.toAddress"
                      :profile-id="item.toUsername"
                      :profile-technical-id="item.toTechnicalId"
                      :space-id="item.toSpaceId"
                      :profile-type="item.toType"
                      :display-name="item.toDisplayName"
                      :avatar="item.toAvatar" />
                    <span>
                      {{ $t('exoplatform.wallet.label.asAdministratorOnContract') }}
                    </span>
                    <wallet-address
                      :value="item.contractAddress"
                      :name="item.contractName"
                      display-label />
                  </v-list-item-title>
  
                  <v-list-item-title v-else-if="item.contractMethodName === 'removeAdmin'">
                    <profile-chip
                      v-if="displayFullTransaction"
                      :address="item.fromAddress"
                      :profile-id="item.fromUsername"
                      :profile-technical-id="item.fromTechnicalId"
                      :space-id="item.fromSpaceId"
                      :profile-type="item.fromType"
                      :display-name="item.fromDisplayName"
                      :avatar="item.fromAvatar" />
                    <span>
                      {{ $t('exoplatform.wallet.label.removed') }}
                    </span>
                    <profile-chip
                      :address="item.toAddress"
                      :profile-id="item.toUsername"
                      :profile-technical-id="item.toTechnicalId"
                      :space-id="item.toSpaceId"
                      :profile-type="item.toType"
                      :display-name="item.toDisplayName"
                      :avatar="item.toAvatar" />
                    <span>
                      {{ $t('exoplatform.wallet.label.asAdministratorFromContract') }}
                    </span>
                    <wallet-address
                      :value="item.contractAddress"
                      :name="item.contractName"
                      display-label />
                  </v-list-item-title>
                  <v-list-item-title v-else-if="item.contractMethodName === 'transferOwnership'">
                    <profile-chip
                      v-if="displayFullTransaction"
                      :address="item.fromAddress"
                      :profile-id="item.fromUsername"
                      :profile-technical-id="item.fromTechnicalId"
                      :space-id="item.fromSpaceId"
                      :profile-type="item.fromType"
                      :display-name="item.fromDisplayName"
                      :avatar="item.fromAvatar" />
                    <span>
                      {{ $t('exoplatform.wallet.label.transferredOwnershipTo') }}
                    </span>
                    <profile-chip
                      :address="item.toAddress"
                      :profile-id="item.toUsername"
                      :profile-technical-id="item.toTechnicalId"
                      :space-id="item.toSpaceId"
                      :profile-type="item.toType"
                      :display-name="item.toDisplayName"
                      :avatar="item.toAvatar" />
                    <span>
                      {{ $t('exoplatform.wallet.label.onContract') }}
                    </span>
                    <wallet-address
                      :value="item.contractAddress"
                      :name="item.contractName"
                      display-label />
                  </v-list-item-title>
  
                  <v-list-item-title v-else-if="item.contractMethodName === 'approveAccount'">
                    <profile-chip
                      v-if="displayFullTransaction"
                      :address="item.fromAddress"
                      :profile-id="item.fromUsername"
                      :profile-technical-id="item.fromTechnicalId"
                      :space-id="item.fromSpaceId"
                      :profile-type="item.fromType"
                      :display-name="item.fromDisplayName"
                      :avatar="item.fromAvatar" />
                    <span>
                      {{ $t('exoplatform.wallet.label.approved') }}
                    </span>
                    <profile-chip
                      :address="item.toAddress"
                      :profile-id="item.toUsername"
                      :profile-technical-id="item.toTechnicalId"
                      :space-id="item.toSpaceId"
                      :profile-type="item.toType"
                      :display-name="item.toDisplayName"
                      :avatar="item.toAvatar" />
                    <span>
                      {{ $t('exoplatform.wallet.label.accountOnContract') }}
                    </span>
                    <wallet-address
                      :value="item.contractAddress"
                      :name="item.contractName"
                      display-label />
                  </v-list-item-title>
  
                  <v-list-item-title v-else-if="item.contractMethodName === 'disapproveAccount'">
                    <profile-chip
                      v-if="displayFullTransaction"
                      :address="item.fromAddress"
                      :profile-id="item.fromUsername"
                      :profile-technical-id="item.fromTechnicalId"
                      :space-id="item.fromSpaceId"
                      :profile-type="item.fromType"
                      :display-name="item.fromDisplayName"
                      :avatar="item.fromAvatar" />
                    <span>
                      {{ $t('exoplatform.wallet.label.disapproved') }}
                    </span>
                    <profile-chip
                      :address="item.toAddress"
                      :profile-id="item.toUsername"
                      :profile-technical-id="item.toTechnicalId"
                      :space-id="item.toSpaceId"
                      :profile-type="item.toType"
                      :display-name="item.toDisplayName"
                      :avatar="item.toAvatar" />
                    <span>
                      {{ $t('exoplatform.wallet.label.accountOnContract') }}
                    </span>
                    <wallet-address
                      :value="item.contractAddress"
                      :name="item.contractName"
                      display-label />
                  </v-list-item-title>
  
                  <v-list-item-title v-else-if="item.contractMethodName === 'pause'">
                    <profile-chip
                      v-if="displayFullTransaction"
                      :address="item.fromAddress"
                      :profile-id="item.fromUsername"
                      :profile-technical-id="item.fromTechnicalId"
                      :space-id="item.fromSpaceId"
                      :profile-type="item.fromType"
                      :display-name="item.fromDisplayName"
                      :avatar="item.fromAvatar" />
                    <span>
                      {{ $t('exoplatform.wallet.label.pausedContract') }}
                    </span>
                    <wallet-address
                      :value="item.contractAddress"
                      :name="item.contractName"
                      display-label />
                  </v-list-item-title>
  
                  <v-list-item-title v-else-if="item.contractMethodName === 'unPause'">
                    <profile-chip
                      v-if="displayFullTransaction"
                      :address="item.fromAddress"
                      :profile-id="item.fromUsername"
                      :profile-technical-id="item.fromTechnicalId"
                      :space-id="item.fromSpaceId"
                      :profile-type="item.fromType"
                      :display-name="item.fromDisplayName"
                      :avatar="item.fromAvatar" />
                    <span>
                      {{ $t('exoplatform.wallet.label.unPausedContract') }}
                    </span>
                    <wallet-address
                      :value="item.contractAddress"
                      :name="item.contractName"
                      display-label />
                  </v-list-item-title>
  
                  <v-list-item-title v-else-if="item.contractMethodName === 'setSellPrice'">
                    <profile-chip
                      v-if="displayFullTransaction"
                      :address="item.fromAddress"
                      :profile-id="item.fromUsername"
                      :profile-technical-id="item.fromTechnicalId"
                      :space-id="item.fromSpaceId"
                      :profile-type="item.fromType"
                      :display-name="item.fromDisplayName"
                      :avatar="item.fromAvatar" />
                    <span>
                      {{ $t('exoplatform.wallet.label.modifiedSellPrice') }}
                    </span>
                  </v-list-item-title>
  
                  <v-list-item-title v-else-if="item.contractMethodName === 'upgradeData'">
                    <profile-chip
                      v-if="displayFullTransaction"
                      :address="item.fromAddress"
                      :profile-id="item.fromUsername"
                      :profile-technical-id="item.fromTechnicalId"
                      :space-id="item.fromSpaceId"
                      :profile-type="item.fromType"
                      :display-name="item.fromDisplayName"
                      :avatar="item.fromAvatar" />
                    <span>
                      {{ $t('exoplatform.wallet.label.upgradedDataContract') }}
                    </span>
                  </v-list-item-title>
  
                  <v-list-item-title v-else-if="item.contractMethodName === 'upgradeImplementation'">
                    <profile-chip
                      v-if="displayFullTransaction"
                      :address="item.fromAddress"
                      :profile-id="item.fromUsername"
                      :profile-technical-id="item.fromTechnicalId"
                      :space-id="item.fromSpaceId"
                      :profile-type="item.fromType"
                      :display-name="item.fromDisplayName"
                      :avatar="item.fromAvatar" />
                    <span>
                      {{ $t('exoplatform.wallet.label.upgradedImplementationContract') }}
                    </span>
                  </v-list-item-title>
  
                  <v-list-item-title v-else-if="item.contractMethodName === 'transformToVested'">
                    <profile-chip
                      v-if="displayFullTransaction"
                      :address="item.fromAddress"
                      :profile-id="item.fromUsername"
                      :profile-technical-id="item.fromTechnicalId"
                      :space-id="item.fromSpaceId"
                      :profile-type="item.fromType"
                      :display-name="item.fromDisplayName"
                      :avatar="item.fromAvatar" />
                    <span>
                      {{ $t('exoplatform.wallet.label.vestedTokensTo') }}
                    </span>
                    <profile-chip
                      :address="item.toAddress"
                      :profile-id="item.toUsername"
                      :profile-technical-id="item.toTechnicalId"
                      :space-id="item.toSpaceId"
                      :profile-type="item.toType"
                      :display-name="item.toDisplayName"
                      :avatar="item.toAvatar" />
                  </v-list-item-title>
  
                  <v-list-item-title v-else-if="item.contractMethodName === 'initializeAccount'">
                    <profile-chip
                      v-if="displayFullTransaction"
                      :address="item.fromAddress"
                      :profile-id="item.fromUsername"
                      :profile-technical-id="item.fromTechnicalId"
                      :space-id="item.fromSpaceId"
                      :profile-type="item.fromType"
                      :display-name="item.fromDisplayName"
                      :avatar="item.fromAvatar" />
                    <span>
                      {{ $t('exoplatform.wallet.label.initializedWalletOf') }}
                    </span>
                    <profile-chip
                      :address="item.toAddress"
                      :profile-id="item.toUsername"
                      :profile-technical-id="item.toTechnicalId"
                      :space-id="item.toSpaceId"
                      :profile-type="item.toType"
                      :display-name="item.toDisplayName"
                      :avatar="item.toAvatar" />
                  </v-list-item-title>
  
                  <v-list-item-title v-else-if="item.contractMethodName === 'reward'">
                    <profile-chip
                      v-if="displayFullTransaction"
                      :address="item.fromAddress"
                      :profile-id="item.fromUsername"
                      :profile-technical-id="item.fromTechnicalId"
                      :space-id="item.fromSpaceId"
                      :profile-type="item.fromType"
                      :display-name="item.fromDisplayName"
                      :avatar="item.fromAvatar" />
                    <span>
                      {{ $t('exoplatform.wallet.label.rewarded') }}
                    </span>
                    <profile-chip
                      :address="item.toAddress"
                      :profile-id="item.toUsername"
                      :profile-technical-id="item.toTechnicalId"
                      :space-id="item.toSpaceId"
                      :profile-type="item.toType"
                      :display-name="item.toDisplayName"
                      :avatar="item.toAvatar" />
                  </v-list-item-title>
  
                  <v-list-item-title v-else-if="item.contractMethodName === 'depositFunds'">
                    <profile-chip
                      v-if="displayFullTransaction"
                      :address="item.fromAddress"
                      :profile-id="item.fromUsername"
                      :profile-technical-id="item.fromTechnicalId"
                      :space-id="item.fromSpaceId"
                      :profile-type="item.fromType"
                      :display-name="item.fromDisplayName"
                      :avatar="item.fromAvatar" />
                    <span>
                      {{ $t('exoplatform.wallet.label.sentEtherToContract') }}
                    </span>
                  </v-list-item-title>
  
                  <v-list-item-title v-else-if="item.value && Number(item.value) && item.amountFiat">
                    <profile-chip
                      v-if="displayFullTransaction"
                      :address="item.fromAddress"
                      :profile-id="item.fromUsername"
                      :profile-technical-id="item.fromTechnicalId"
                      :space-id="item.fromSpaceId"
                      :profile-type="item.fromType"
                      :display-name="item.fromDisplayName"
                      :avatar="item.fromAvatar" />
                    <span>
                      {{ $t('exoplatform.wallet.label.sentEtherToContract') }}
                    </span>
                  </v-list-item-title>
  
                  <v-list-item-title v-else>
                    <span>
                      {{ $t('exoplatform.wallet.label.contractTransaction') }}
                    </span>
                    <wallet-address
                      :value="item.contractAddress"
                      :name="item.contractName"
                      display-label />
                  </v-list-item-title>
  
                  <v-list-item-subtitle>
                    <v-icon
                      v-if="!item.pending && !item.succeeded"
                      color="orange"
                      title="Transaction failed">
                      warning
                    </v-icon>
                    <v-list-item-action-text v-if="item.dateFormatted">
                      {{ item.dateFormatted }}
                    </v-list-item-action-text>
                  </v-list-item-subtitle>
                </v-list-item-content>
  
                <v-list-item-content v-if="item.type === 'ether' && item.value && Number(item.value)" class="transactionDetailActions">
                  <v-list-item-title :class="item.adminIcon ? '' : item.isReceiver ? 'green--text' : 'red--text'">
                    <span>
                      {{ toFixed(item.value) }} ether
                    </span>
                  </v-list-item-title>
                  <v-list-item-subtitle v-if="item.amountFiat">
                    <v-list-item-action-text>
                      {{ toFixed(item.amountFiat) }} {{ fiatSymbol }}
                    </v-list-item-action-text>
                  </v-list-item-subtitle>
                </v-list-item-content>
  
                <v-list-item-content v-else class="transactionDetailActions">
                  <v-list-item-title
                    v-if="item.contractAmount"
                    :class="item.adminIcon ? '' : item.isReceiver ? 'green--text' : 'red--text'">
                    <span>
                      {{ toFixed(item.contractAmount) }} {{ item.contractSymbol }}
                    </span>
                  </v-list-item-title>
                  <v-list-item-title
                    v-else-if="item.value && Number(item.value)"
                    :class="item.isReceiver ? 'green--text' : 'red--text'">
                    <span>
                      {{ toFixed(item.value) }} ether
                    </span>
                  </v-list-item-title>
                  <v-list-item-subtitle
                    v-if="item.contractMethodName === 'reward'">
                    <v-list-item-action-text>
                      {{ toFixed(item.value) }} {{ item.contractSymbol }}
                    </v-list-item-action-text>
                  </v-list-item-subtitle>
                  <v-list-item-subtitle
                    v-else-if="item.amountFiat">
                    <v-list-item-action-text>
                      {{ toFixed(item.amountFiat) }} {{ fiatSymbol }}
                    </v-list-item-action-text>
                  </v-list-item-subtitle>
                </v-list-item-content>
              </v-list-item>
            </v-list>
          </v-expansion-panel-header>
          <v-expansion-panel-content>
            <v-list class="px-0 ml-2" dense>
              <v-list-item v-if="!item.pending">
                <v-list-item-content>
                  {{ $t('exoplatform.wallet.label.status') }}
                </v-list-item-content>
                <v-list-item-content class="align-end text-right">
                  <div class="no-wrap">
                    <v-icon :color="item.succeeded ? 'success' : 'error'" v-text="item.succeeded ? 'fa-check-circle' : 'fa-exclamation-circle'" />
                  </div>
                </v-list-item-content>
              </v-list-item>
              <v-list-item v-if="administration && item.issuer">
                <v-list-item-content>
                  {{ $t('exoplatform.wallet.label.issuer') }}
                </v-list-item-content>
                <v-list-item-content class="align-end text-right">
                  <profile-chip
                    :address="item.issuer.address"
                    :profile-id="item.issuer.id"
                    :profile-technical-id="item.issuer.technicalId"
                    :space-id="item.issuer.spaceId"
                    :profile-type="item.issuer.type"
                    :display-name="item.issuer.name"
                    :avatar="item.issuer.avatar" />
                </v-list-item-content>
              </v-list-item>
  
              <v-list-item v-if="item.label" class="dynamic-height">
                <v-list-item-content>
                  {{ $t('exoplatform.wallet.label.transactionLabel') }}
                </v-list-item-content>
                <v-list-item-content class="align-end text-right paragraph">
                  <div class="no-wrap">
                    {{ item.label }}
                  </div>
                </v-list-item-content>
              </v-list-item>
  
              <v-list-item v-if="item.message" class="dynamic-height">
                <v-list-item-content>
                  {{ $t('exoplatform.wallet.label.transactionMessage') }}
                </v-list-item-content>
                <v-list-item-content class="align-end text-right paragraph">
                  <div class="no-wrap">
                    {{ item.message }}
                  </div>
                </v-list-item-content>
              </v-list-item>
  
              <v-list-item v-if="Number(item.contractAmount)">
                <v-list-item-content>
                  {{ item.contractAmountLabel }}
                </v-list-item-content>
                <v-list-item-content class="align-end text-right">
                  <div class="no-wrap">
                    {{ toFixed(item.contractAmount) }} {{ item.contractSymbol }}
                  </div>
                </v-list-item-content>
              </v-list-item>
              <template v-if="item.contractMethodName === 'reward'">
                <v-list-item v-if="Number(item.value)">
                  <v-list-item-content>
                    {{ $t('exoplatform.wallet.label.transferredAmount') }}
                  </v-list-item-content>
                  <v-list-item-content class="align-end text-right">
                    <div class="no-wrap">
                      {{ toFixed(item.value) }} {{ item.contractSymbol }}
                    </div>
                  </v-list-item-content>
                </v-list-item>
              </template>
              <template v-else>
                <v-list-item v-if="Number(item.amountFiat)">
                  <v-list-item-content>
                    {{ $t('exoplatform.wallet.label.fiatAmount') }}
                  </v-list-item-content>
                  <v-list-item-content class="align-end text-right">
                    <div class="no-wrap">
                      {{ toFixed(item.amountFiat) }} {{ fiatSymbol }}
                    </div>
                  </v-list-item-content>
                </v-list-item>
                <v-list-item v-if="Number(item.value)">
                  <v-list-item-content>
                    {{ $t('exoplatform.wallet.label.etherAmount') }}
                  </v-list-item-content>
                  <v-list-item-content class="align-end text-right">
                    <div class="no-wrap">
                      {{ toFixed(item.value) }} ether
                    </div>
                  </v-list-item-content>
                </v-list-item>
              </template>
  
              <v-list-item v-if="item.fromAddress">
                <v-list-item-content>
                  {{ $t('exoplatform.wallet.label.fromAddress') }}
                </v-list-item-content>
                <v-list-item-content class="align-end text-right">
                  <a
                    v-if="addressEtherscanLink"
                    :href="`${addressEtherscanLink}${item.fromAddress}`"
                    :title="$t('exoplatform.wallet.label.openOnEtherscan')"
                    target="_blank">
                    {{ item.fromAddress }}
                  </a>
                </v-list-item-content>
              </v-list-item>
  
              <v-list-item v-if="item.toAddress">
                <v-list-item-content>
                  {{ $t('exoplatform.wallet.label.toAddress') }}
                </v-list-item-content>
                <v-list-item-content class="align-end text-right">
                  <a
                    v-if="addressEtherscanLink"
                    :href="`${addressEtherscanLink}${item.toAddress}`"
                    :title="$t('exoplatform.wallet.label.openOnEtherscan')"
                    target="_blank">
                    {{ item.toAddress }}
                  </a>
                </v-list-item-content>
              </v-list-item>
  
              <v-list-item v-if="item.contractName">
                <v-list-item-content>
                  {{ $t('exoplatform.wallet.label.contractName') }}
                </v-list-item-content>
                <v-list-item-content class="align-end text-right">
                  <div class="no-wrap">
                    {{ item.contractName }}
                  </div>
                </v-list-item-content>
              </v-list-item>
  
              <v-list-item v-if="item.contractAddress">
                <v-list-item-content>
                  {{ $t('exoplatform.wallet.label.contractAddress') }}
                </v-list-item-content>
                <v-list-item-content class="align-end text-right">
                  <a
                    v-if="tokenEtherscanLink"
                    :href="`${tokenEtherscanLink}${item.contractAddress}`"
                    :title="$t('exoplatform.wallet.label.openOnEtherscan')"
                    target="_blank">
                    {{ item.contractAddress }}
                  </a>
                </v-list-item-content>
              </v-list-item>
  
              <v-list-item v-if="item.fee">
                <v-list-item-content>
                  {{ $t('exoplatform.wallet.label.transactionFee') }}
                </v-list-item-content>
                <v-list-item-content v-if="item.tokenFee" class="align-end text-right">
                  <div class="no-wrap">
                    {{ toFixed(item.tokenFee) }} {{ item.contractSymbol }}
                  </div>
                </v-list-item-content>
                <v-list-item-content v-else class="align-end text-right">
                  <div class="no-wrap">
                    {{ toFixed(item.feeFiat) }} {{ fiatSymbol }}
                    <v-icon
                      v-if="item.noContractFunds"
                      color="orange"
                      title="You financed transaction fee with ether instead of Token.">
                      warning
                    </v-icon>
                  </div>
                </v-list-item-content>
              </v-list-item>
  
              <v-list-item>
                <v-list-item-content>
                  {{ $t('exoplatform.wallet.label.transactionHash') }}
                </v-list-item-content>
                <v-list-item-content class="align-end text-right">
                  <a
                    v-if="transactionEtherscanLink"
                    :href="`${transactionEtherscanLink}${item.hash}`"
                    :title="$t('exoplatform.wallet.label.openOnEtherscan')"
                    target="_blank">
                    {{ item.hash }}
                  </a>
                </v-list-item-content>
              </v-list-item>
            </v-list>
          </v-expansion-panel-content>
        </v-expansion-panel>
        <div v-if="!limitReached">
          <v-btn
            :loading="loading"
            color="primary"
            text
            @click="transactionsLimit += transactionsPerPage">
            {{ $t('exoplatform.wallet.button.loadMore') }}
          </v-btn>
        </div>
      </v-expansion-panels>
      <v-flex v-else-if="!loading" class="text-center">
        <span>
          {{ $t('exoplatform.wallet.label.noRecentTransactions') }}
        </span>
      </v-flex>
    </v-card>
  </v-flex>
</template>

<script>
import WalletAddress from './WalletAddress.vue';
import ProfileChip from './ProfileChip.vue';

import {getTransactionEtherscanlink, getAddressEtherscanlink, getTokenEtherscanlink, toFixed} from '../js/WalletUtils.js';
import {loadTransactions} from '../js/TransactionUtils.js';

export default {
  components: {
    ProfileChip,
    WalletAddress,
  },
  props: {
    fiatSymbol: {
      type: String,
      default: function() {
        return null;
      },
    },
    account: {
      type: String,
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
    error: {
      type: String,
      default: function() {
        return null;
      },
    },
    displayFullTransaction: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    selectedTransactionHash: {
      type: String,
      default: function() {
        return null;
      },
    },
    selectedContractMethodName: {
      type: String,
      default: function() {
        return null;
      },
    },
    administration: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
  },
  data() {
    return {
      // A trick to force update computed list
      // since the attribute this.transactions is modified outside the component
      refreshIndex: 1,
      loading: false,
      transactionsLimit: 10,
      transactionsPerPage: 10,
      limitReached: false,
      transactions: {},
    };
  },
  computed: {
    sortedTransactions() {
      // A trick to force update computed list
      // since the attribute this.transactions is modified outside the component
      if (!this.refreshIndex) {
        return {};
      }
      const transactions = this.transactions;
      const sortedTransactions = {};
      Object.values(transactions)
        .filter((transaction) => transaction && !transaction.ignore)
        .sort((transaction1, transaction2) => (transaction2.date || 0) - (transaction1.date || 0))
        .forEach((transaction) => {
          sortedTransactions[transaction.hash] = transaction;
        });
      return sortedTransactions;
    },
  },
  watch: {
    transactionsLimit() {
      if (this.transactionsLimit !== this.transactionsPerPage && this.account && !this.loading) {
        this.init(true).catch((error) => {
          console.debug('account field change event - error', error);
          this.loading = false;
          this.$emit('error', `Account loading error: ${error}`);
        });
      }
    },
    contractDetails() {
      if (this.contractDetails && this.account) {
        this.limitReached = false;
        this.transactions = {};
        this.transactionsLimit = this.transactionsPerPage;
        this.init().catch((error) => {
          console.debug('account field change event - error', error);
          this.loading = false;
          this.$emit('error', `${this.$t('exoplatform.wallet.error.accountLoadingError')}: ${error}`);
        });
      }
    },
  },
  created() {
    this.transactionEtherscanLink = getTransactionEtherscanlink();
    this.addressEtherscanLink = getAddressEtherscanlink();
    this.tokenEtherscanLink = getTokenEtherscanlink();

    if (this.account) {
      this.init().catch((error) => {
        console.debug('init method - error', error);
        this.loading = false;
        this.$emit('error', `${this.$t('exoplatform.wallet.error.accountInitializationError')}: ${error}`);
      });
    }
  },
  methods: {
    init(ignoreSelected) {
      this.loading = true;
      this.error = null;

      // Get transactions to latest block with maxBlocks to load
      return this.loadRecentTransaction(this.transactionsLimit)
        .then(() => {
          if (!ignoreSelected && this.selectedTransactionHash) {
            const selectedTransaction = this.transactions[this.selectedTransactionHash] || this.transactions[this.selectedTransactionHash.toLowerCase()];
            if (selectedTransaction) {
              this.$set(selectedTransaction, 'selected', true);
              this.$nextTick(() => {
                setTimeout(() => {
                  const selectedTransactionElement = document.getElementById(`transaction-${selectedTransaction.hash}`);
                  if (selectedTransactionElement) {
                    selectedTransactionElement.scrollIntoView();
                  }
                }, 200);
              });
            }
          }
          this.loading = false;
          this.forceUpdateList();
        })
        .catch((e) => {
          console.debug('loadRecentTransaction - method error', e);

          this.loading = false;
          this.$emit('error', `${e}`);
        })
        .finally(() => {
          this.$emit('loaded', this.sortedTransactions, this.transactions ? Object.keys(this.transactions).length : 0);
        });
    },
    loadRecentTransaction(limit) {
      const thiss = this;
      const filterObject = {
        hash: this.selectedTransactionHash,
        contractMethodName: this.selectedContractMethodName,
      };
      return loadTransactions(this.account, this.contractDetails, this.transactions, false, limit, filterObject, this.administration, (transactionDetails) => {
        if (transactionDetails && thiss.transactions) {
          const TransactionToUpdate = Object.values(thiss.transactions).find(transaction => transaction && transaction.hash === transactionDetails.hash);
          if (TransactionToUpdate) {
            Object.assign(TransactionToUpdate, transactionDetails);
            thiss.forceUpdateList();
          }
        }
      })
        .then(() => {
          const totalTransactionsCount = Object.keys(this.transactions).length;
          this.limitReached = (totalTransactionsCount <= this.transactionsLimit && (totalTransactionsCount % this.transactionsPerPage) > 0) || (totalTransactionsCount < this.transactionsLimit);
        })
        .catch((e) => {
          console.debug('loadTransactions - method error', e);
          this.$emit('error', `${e}`);
        });
    },
    forceUpdateList() {
      try {
        // A trick to force update computed list
        // since the attribute this.transactions is modified outside the component
        this.refreshIndex++;
        this.$forceUpdate();
      } catch (e) {
        console.debug('forceUpdateList - method error', e);
      }
    },
    toFixed(args) {
      return toFixed(args);
    },
  },
};
</script>
