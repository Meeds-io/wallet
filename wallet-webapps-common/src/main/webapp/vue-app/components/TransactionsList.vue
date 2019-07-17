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

      <v-expansion-panel v-if="Object.keys(sortedTransactions).length">
        <v-expansion-panel-content
          v-for="(item, index) in sortedTransactions"
          :id="`transaction-${item.hash}`"
          :key="index"
          :value="item.selected">
          <v-list
            slot="header"
            :class="item.selected && 'blue lighten-5'"
            two-line
            ripple
            class="pt-0 pb-0">
            <v-list-tile
              :key="item.hash"
              class="transactionDetailItem autoHeight"
              avatar
              ripple>
              <v-progress-circular
                v-if="item.pending"
                indeterminate
                color="primary"
                class="mr-4" />
              <v-list-tile-avatar v-else-if="item.error" :title="item.error">
                <v-icon color="red">
                  warning
                </v-icon>
              </v-list-tile-avatar>
              <v-list-tile-avatar v-else-if="item.adminIcon">
                <v-icon color="grey">
                  fa-cog
                </v-icon>
              </v-list-tile-avatar>
              <v-list-tile-avatar v-else-if="item.isReceiver">
                <v-icon color="green">
                  fa-arrow-down
                </v-icon>
              </v-list-tile-avatar>
              <v-list-tile-avatar v-else>
                <v-icon color="red">
                  fa-arrow-up
                </v-icon>
              </v-list-tile-avatar>

              <v-list-tile-content class="transactionDetailContent">
                <v-list-tile-title v-if="item.isContractCreation">
                  <span>
                    {{ $t('exoplatform.wallet.label.createdContract') }}
                  </span>
                  <wallet-address
                    :value="item.contractAddress"
                    :name="item.contractName"
                    display-label />
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.type === 'ether'">
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
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.contractMethodName === 'transferFrom'">
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
                </v-list-tile-title>

                <v-list-tile-title v-else-if="(item.contractMethodName === 'transfer' || item.contractMethodName === 'approve')">
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
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.contractMethodName === 'addAdmin'">
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
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.contractMethodName === 'removeAdmin'">
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
                </v-list-tile-title>
                <v-list-tile-title v-else-if="item.contractMethodName === 'transferOwnership'">
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
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.contractMethodName === 'approveAccount'">
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
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.contractMethodName === 'disapproveAccount'">
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
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.contractMethodName === 'pause'">
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
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.contractMethodName === 'unPause'">
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
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.contractMethodName === 'setSellPrice'">
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
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.contractMethodName === 'upgradeData'">
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
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.contractMethodName === 'upgradeImplementation'">
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
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.contractMethodName === 'transformToVested'">
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
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.contractMethodName === 'initializeAccount'">
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
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.contractMethodName === 'reward'">
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
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.contractMethodName === 'depositFunds'">
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
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.value && Number(item.value) && item.amountFiat">
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
                </v-list-tile-title>

                <v-list-tile-title v-else>
                  <span>
                    {{ $t('exoplatform.wallet.label.contractTransaction') }}
                  </span>
                  <wallet-address
                    :value="item.contractAddress"
                    :name="item.contractName"
                    display-label />
                </v-list-tile-title>

                <v-list-tile-sub-title>
                  <v-icon
                    v-if="!item.pending && !item.status"
                    color="orange"
                    title="Transaction failed">
                    warning
                  </v-icon>
                  <v-list-tile-action-text v-if="item.dateFormatted">
                    {{ item.dateFormatted }}
                  </v-list-tile-action-text>
                </v-list-tile-sub-title>
              </v-list-tile-content>

              <v-list-tile-content v-if="item.type === 'ether' && item.value && Number(item.value)" class="transactionDetailActions">
                <v-list-tile-title :class="item.adminIcon ? '' : item.isReceiver ? 'green--text' : 'red--text'">
                  <span>
                    {{ toFixed(item.value) }} ether
                  </span>
                </v-list-tile-title>
                <v-list-tile-sub-title v-if="item.amountFiat">
                  <v-list-tile-action-text>
                    {{ toFixed(item.amountFiat) }} {{ fiatSymbol }}
                  </v-list-tile-action-text>
                </v-list-tile-sub-title>
              </v-list-tile-content>

              <v-list-tile-content v-else class="transactionDetailActions">
                <v-list-tile-title
                  v-if="item.contractAmount"
                  :class="item.adminIcon ? '' : item.isReceiver ? 'green--text' : 'red--text'">
                  <span>
                    {{ toFixed(item.contractAmount) }} {{ item.contractSymbol }}
                  </span>
                </v-list-tile-title>
                <v-list-tile-title
                  v-else-if="item.value && Number(item.value)"
                  :class="item.isReceiver ? 'green--text' : 'red--text'">
                  <span>
                    {{ toFixed(item.value) }} ether
                  </span>
                </v-list-tile-title>
                <v-list-tile-sub-title
                  v-if="item.contractMethodName === 'reward'">
                  <v-list-tile-action-text>
                    {{ toFixed(item.value) }} {{ item.contractSymbol }}
                  </v-list-tile-action-text>
                </v-list-tile-sub-title>
                <v-list-tile-sub-title
                  v-else-if="item.amountFiat">
                  <v-list-tile-action-text>
                    {{ toFixed(item.amountFiat) }} {{ fiatSymbol }}
                  </v-list-tile-action-text>
                </v-list-tile-sub-title>
                <v-list-tile-sub-title v-else />
              </v-list-tile-content>
            </v-list-tile>
          </v-list>

          <v-list class="pl-5 ml-2 pr-4" dense>
            <v-list-tile v-if="!item.pending">
              <v-list-tile-content>
                {{ $t('exoplatform.wallet.label.status') }}
              </v-list-tile-content>
              <v-list-tile-content class="align-end">
                <v-icon :color="item.status ? 'success' : 'error'" v-text="item.status ? 'fa-check-circle' : 'fa-exclamation-circle'" />
              </v-list-tile-content>
            </v-list-tile>
            <v-list-tile v-if="administration && item.issuer">
              <v-list-tile-content>
                {{ $t('exoplatform.wallet.label.issuer') }}
              </v-list-tile-content>
              <v-list-tile-content class="align-end">
                <profile-chip
                  :address="item.issuer.address"
                  :profile-id="item.issuer.id"
                  :profile-technical-id="item.issuer.technicalId"
                  :space-id="item.issuer.spaceId"
                  :profile-type="item.issuer.type"
                  :display-name="item.issuer.name"
                  :avatar="item.issuer.avatar" />
              </v-list-tile-content>
            </v-list-tile>

            <v-list-tile v-if="item.label" class="dynamic-height">
              <v-list-tile-content>
                {{ $t('exoplatform.wallet.label.transactionLabel') }}
              </v-list-tile-content>
              <v-list-tile-content class="align-end paragraph" v-text="item.label" />
            </v-list-tile>

            <v-list-tile v-if="item.message" class="dynamic-height">
              <v-list-tile-content>
                {{ $t('exoplatform.wallet.label.transactionMessage') }}
              </v-list-tile-content>
              <v-list-tile-content class="align-end paragraph" v-text="item.message" />
            </v-list-tile>

            <v-list-tile v-if="Number(item.contractAmount)">
              <v-list-tile-content>
                {{ item.contractAmountLabel }}
              </v-list-tile-content>
              <v-list-tile-content class="align-end">
                {{ toFixed(item.contractAmount) }} {{ item.contractSymbol }}
              </v-list-tile-content>
            </v-list-tile>
            <template v-if="item.contractMethodName === 'reward'">
              <v-list-tile v-if="Number(item.value)">
                <v-list-tile-content>
                  {{ $t('exoplatform.wallet.label.transferredAmount') }}
                </v-list-tile-content>
                <v-list-tile-content class="align-end">
                  {{ toFixed(item.value) }} {{ item.contractSymbol }}
                </v-list-tile-content>
              </v-list-tile>
            </template>
            <template v-else>
              <v-list-tile v-if="Number(item.amountFiat)">
                <v-list-tile-content>
                  {{ $t('exoplatform.wallet.label.fiatAmount') }}
                </v-list-tile-content>
                <v-list-tile-content class="align-end">
                  {{ toFixed(item.amountFiat) }} {{ fiatSymbol }}
                </v-list-tile-content>
              </v-list-tile>
              <v-list-tile v-if="Number(item.value)">
                <v-list-tile-content>
                  {{ $t('exoplatform.wallet.label.etherAmount') }}
                </v-list-tile-content>
                <v-list-tile-content class="align-end">
                  {{ toFixed(item.value) }} ether
                </v-list-tile-content>
              </v-list-tile>
            </template>

            <v-list-tile v-if="item.fromAddress">
              <v-list-tile-content>
                {{ $t('exoplatform.wallet.label.fromAddress') }}
              </v-list-tile-content>
              <v-list-tile-content class="align-end">
                <a
                  v-if="addressEtherscanLink"
                  :href="`${addressEtherscanLink}${item.fromAddress}`"
                  :title="$t('exoplatform.wallet.label.openOnEtherscan')"
                  target="_blank">
                  {{ item.fromAddress }}
                </a>
              </v-list-tile-content>
            </v-list-tile>

            <v-list-tile v-if="item.toAddress">
              <v-list-tile-content>
                {{ $t('exoplatform.wallet.label.toAddress') }}
              </v-list-tile-content>
              <v-list-tile-content class="align-end">
                <a
                  v-if="addressEtherscanLink"
                  :href="`${addressEtherscanLink}${item.toAddress}`"
                  :title="$t('exoplatform.wallet.label.openOnEtherscan')"
                  target="_blank">
                  {{ item.toAddress }}
                </a>
              </v-list-tile-content>
            </v-list-tile>

            <v-list-tile v-if="item.contractName">
              <v-list-tile-content>
                {{ $t('exoplatform.wallet.label.contractName') }}
              </v-list-tile-content>
              <v-list-tile-content class="align-end">
                {{ item.contractName }}
              </v-list-tile-content>
            </v-list-tile>

            <v-list-tile v-if="item.contractAddress">
              <v-list-tile-content>
                {{ $t('exoplatform.wallet.label.contractAddress') }}
              </v-list-tile-content>
              <v-list-tile-content class="align-end">
                <a
                  v-if="tokenEtherscanLink"
                  :href="`${tokenEtherscanLink}${item.contractAddress}`"
                  :title="$t('exoplatform.wallet.label.openOnEtherscan')"
                  target="_blank">
                  {{ item.contractAddress }}
                </a>
              </v-list-tile-content>
            </v-list-tile>

            <v-list-tile v-if="item.fee">
              <v-list-tile-content>
                {{ $t('exoplatform.wallet.label.transactionFee') }}
              </v-list-tile-content>
              <v-list-tile-content v-if="item.feeToken" class="align-end">
                {{ toFixed(item.feeToken) }} {{ item.contractSymbol }}
              </v-list-tile-content>
              <v-list-tile-content v-else class="align-end">
                <div class="no-wrap">
                  {{ toFixed(item.feeFiat) }} {{ fiatSymbol }}
                  <v-icon
                    v-if="item.feeNoSufficientFunds"
                    color="orange"
                    title="You financed transaction fee with ether instead of Token.">
                    warning
                  </v-icon>
                </div>
              </v-list-tile-content>
            </v-list-tile>

            <v-list-tile>
              <v-list-tile-content>
                {{ $t('exoplatform.wallet.label.transactionHash') }}
              </v-list-tile-content>
              <v-list-tile-content class="align-end">
                <a
                  v-if="transactionEtherscanLink"
                  :href="`${transactionEtherscanLink}${item.hash}`"
                  :title="$t('exoplatform.wallet.label.openOnEtherscan')"
                  target="_blank">
                  {{ item.hash }}
                </a>
              </v-list-tile-content>
            </v-list-tile>
          </v-list>
        </v-expansion-panel-content>
        <div v-if="!limitReached">
          <v-btn
            :loading="loading"
            color="primary"
            flat
            @click="transactionsLimit += transactionsPerPage">
            {{ $t('exoplatform.wallet.button.loadMore') }}
          </v-btn>
        </div>
      </v-expansion-panel>
      <v-flex v-else-if="!loading" class="text-xs-center">
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
          console.debug('loadTransactions - method error', e);

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
      return loadTransactions(this.account, this.contractDetails, this.transactions, false, limit, filterObject, this.administration, () => {
        thiss.$emit('refresh-balance');
        thiss.forceUpdateList();
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
