<template>
  <v-card
    class="text-xs-center pr-3 pl-3 pt-2"
    flat>
    <v-form ref="form">
      <v-flex id="accessPermissionAutoComplete" class="contactAutoComplete mt-4">
        <v-autocomplete
          ref="accessPermissionAutoComplete"
          v-model="accessPermission"
          :items="accessPermissionOptions"
          :loading="isLoadingSuggestions"
          :search-input.sync="accessPermissionSearchTerm"
          attach="#accessPermissionAutoComplete"
          label="Wallet access permission (Spaces only)"
          class="contactAutoComplete"
          placeholder="Start typing to Search a space"
          content-class="contactAutoCompleteContent"
          max-width="100%"
          item-text="name"
          item-value="id"
          hide-details
          hide-selected
          chips
          cache-items
          dense
          flat
          autofocus>
          <template slot="no-data">
            <v-list-tile>
              <v-list-tile-title>
                Search for a <strong>
                  Space
                </strong>
              </v-list-tile-title>
            </v-list-tile>
          </template>
  
          <template slot="selection" slot-scope="{item, selected}">
            <v-chip
              v-if="item.error"
              :selected="selected"
              class="autocompleteSelectedItem">
              <del>
                <span>
                  {{ item.name }}
                </span>
              </del>
            </v-chip>
            <v-chip
              v-else
              :selected="selected"
              class="autocompleteSelectedItem">
              <span>
                {{ item.name }}
              </span>
            </v-chip>
          </template>
  
          <template slot="item" slot-scope="{item}">
            <v-list-tile-avatar
              v-if="item.avatar"
              tile
              size="20">
              <img :src="item.avatar">
            </v-list-tile-avatar>
            <v-list-tile-title v-text="item.name" />
          </template>
        </v-autocomplete>
      </v-flex>

      <v-flex id="selectedPrincipalAccountParent" class="selectBoxVuetifyParent">
        <v-combobox
          v-model="selectedPrincipalAccount"
          :items="accountsList"
          attach="#selectedPrincipalAccountParent"
          class="mt-4"
          item-disabled="itemDisabled"
          label="Select principal account displayed in wallet overview"
          placeholder="Select principal account displayed in wallet overview"
          chips />
      </v-flex>

      <v-flex id="selectedOverviewAccountsParent" class="selectBoxVuetifyParent">
        <v-combobox
          v-model="selectedOverviewAccounts"
          :items="accountsList"
          attach="#selectedOverviewAccountsParent"
          label="List of currencies to use (by order)"
          placeholder="List of contracts, ether and fiat to use in wallet application (by order)"
          multiple
          deletable-chips
          chips />
      </v-flex>
    </v-form>
    <v-card-actions>
      <v-spacer />
      <button class="btn btn-primary mb-3" @click="save">
        Save
      </button>
      <v-spacer />
    </v-card-actions>
  </v-card>
</template>

<script>
export default {
  props: {
    walletAddress: {
      type: String,
      default: function() {
        return null;
      },
    },
    loading: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
  },
  data() {
    return {
      accessPermission: '',
      accessPermissionOptions: [],
      accessPermissionSearchTerm: null,
      isLoadingSuggestions: false,
      contracts: [],
      selectedOverviewAccounts: [],
      selectedPrincipalAccount: null,
      etherAccount: {text: 'Ether', value: 'ether', disabled: false},
      fiatAccount: {text: 'Fiat', value: 'fiat', disabled: false},
    };
  },
  computed: {
    accountsList() {
      const accountsList = [];
      accountsList.push(Object.assign({}, this.etherAccount), Object.assign({}, this.fiatAccount));
      if (this.contracts) {
        this.contracts.forEach((contract) => {
          if (contract) {
            accountsList.push({text: contract.name, value: contract.address, disabled: false});
          }
        });
      }
      return accountsList;
    },
  },
  watch: {
    accountsList() {
      this.$emit('principal-contract-loaded', this.selectedPrincipalAccount);
    },
    selectedPrincipalAccount() {
      if (this.selectedPrincipalAccount) {
        this.selectedOverviewAccounts.forEach((account) => {
          this.$set(account, 'disabled', false);
        });

        this.accountsList.forEach((account, index) => {
          if (this.selectedPrincipalAccount.value === account.value) {
            this.$set(account, 'disabled', true);
            const accountIndex = this.selectedOverviewAccounts.findIndex((foundSelectedAccount) => foundSelectedAccount.value === account.value);
            if (accountIndex >= 0) {
              this.selectedOverviewAccounts.splice(accountIndex, 1);
            }
            this.selectedOverviewAccounts.unshift(account);
          } else {
            this.$set(account, 'disabled', false);
          }
        });
        this.$forceUpdate();
      }
      this.$emit('principal-account-loaded', this.selectedPrincipalAccount);
    },
    accessPermission(newValue, oldValue) {
      if (oldValue) {
        this.accessPermissionSearchTerm = null;
        // A hack to close on select
        // See https://www.reddit.com/r/vuetifyjs/comments/819h8u/how_to_close_a_multiple_autocomplete_vselect/
        this.$refs.accessPermissionAutoComplete.isFocused = false;
      }
    },
    accessPermissionSearchTerm() {
      this.isLoadingSuggestions = true;
      this.addressRegistry.searchSpaces(this.accessPermissionSearchTerm)
        .then((items) => {
          if (items) {
            this.accessPermissionOptions = items;
          } else {
            this.accessPermissionOptions = [];
          }
          this.isLoadingSuggestions = false;
        })
        .catch((e) => {
          console.debug('searchSpaces method - error', e);
          this.isLoadingSuggestions = false;
        });
    },
  },
  methods: {
    init() {
      this.accessPermission = window.walletSettings.accessPermission;
      if (this.accessPermission) {
        this.addressRegistry.searchSpaces(this.accessPermission).then((items) => {
          if (items) {
            this.accessPermissionOptions = items;
          } else {
            this.accessPermissionOptions = [];
          }
          if (!this.accessPermissionOptions.find((item) => item.id === this.accessPermission)) {
            this.accessPermissionOptions.push({id: this.accessPermission, name: this.accessPermission, error: true});
          }
        });
      }
      this.selectedOverviewAccounts = [];
      this.contracts = [];
      const promises = [];
      if (window.walletSettings.defaultContractsToDisplay) {
        window.walletSettings.defaultContractsToDisplay.forEach((selectedValue) => {
          if (selectedValue && selectedValue.indexOf('0x') === 0) {
            promises.push(this.tokenUtils.getSavedContractDetails(selectedValue, window.walletSettings.defaultNetworkId));
          }
        });
      }
      return Promise.all(promises)
        .then((contracts) => {
          this.contracts = contracts || [];

          if (window.walletSettings.defaultOverviewAccounts) {
            window.walletSettings.defaultOverviewAccounts.forEach((selectedValue) => {
              const selectedObject = this.getOverviewAccountObject(selectedValue);
              if (selectedObject) {
                this.selectedOverviewAccounts.push(selectedObject);
              }
            });
          }

          if(window.walletSettings.defaultPrincipalAccount) {
            this.selectedPrincipalAccount = this.getOverviewAccountObject(window.walletSettings.defaultPrincipalAccount);
          }
        });
    },
    getOverviewAccountObject(selectedValue) {
      if (selectedValue === 'fiat') {
        return Object.assign({}, this.fiatAccount);
      } else if (selectedValue === 'ether') {
        return Object.assign({}, this.etherAccount);
      } else if (this.contracts && this.contracts.length) {
        const selectedContractAddress = this.contracts.findIndex((contract) => contract && contract.address === selectedValue);
        if (selectedContractAddress >= 0) {
          const contract = this.contracts[selectedContractAddress];
          if (!contract.error) {
            return {text: contract.name, value: contract.address, disabled: false};
          }
        }
      }
    },
    save() {
      if(!this.$refs.form.validate()) {
        return;
      }

      const globalSettings = {
        accessPermission: this.accessPermission,
        defaultPrincipalAccount: this.selectedPrincipalAccount && this.selectedPrincipalAccount.value,
        defaultOverviewAccounts: this.selectedOverviewAccounts && this.selectedOverviewAccounts.map((item) => item.value),
      };
      this.$emit('save', globalSettings);
    },
  },
};
</script>
