<template>
  <v-flex :id="id" class="contactAutoComplete">
    <v-autocomplete
      ref="selectAutoComplete"
      v-model="selectedValue"
      :items="items"
      :loading="isLoadingSuggestions"
      :search-input.sync="searchTerm"
      :label="inputLabel"
      :disabled="disabled"
      :attach="`#${id}`"
      :placeholder="inputPlaceholder"
      :content-class="`contactAutoCompleteContent ${bigField && 'bigContactAutoComplete'}`"
      :filter="filterIgnoredItems"
      :required="required"
      class="contactAutoComplete"
      max-width="100%"
      item-text="name"
      item-value="id_type"
      hide-details
      hide-selected
      chips
      cache-items
      dense
      flat>
      <template slot="no-data">
        <v-list-tile>
          <v-list-tile-title v-if="noDataLabel">
            {{ noDataLabel }}
          </v-list-tile-title>
          <v-list-tile-title v-else>
            Search for a <strong>
              Wallet
            </strong> of space or user
          </v-list-tile-title>
        </v-list-tile>
      </template>

      <template slot="selection" slot-scope="{item, selected}">
        <v-chip
          v-if="item.avatar"
          :selected="selected"
          :title="addressLoad === 'error' ? `the recipient is disabled or has't a valid wallet yet` : ''"
          class="autocompleteSelectedItem"
          @input="selectItem(item)">
          <v-progress-circular
            v-if="addressLoad === 'loading'"
            indeterminate
            color="white"
            class="mr-2" />
          <v-icon
            v-else-if="item.enabled === false || item.deletedUser || item.disabledUser"
            :title="(item.disabledUser && 'Disabled user') || (item.deletedUser && 'Deleted space') || (item.enabled === false && 'Wallet is disabled')"
            class="mr-2"
            color="orange"
            size="15">
            warning
          </v-icon>
          <v-icon
            v-else-if="addressLoad === 'error'"
            title="Invalid address"
            class="mr-2"
            color="red"
            size="15">
            warning
          </v-icon>
          <span>
            {{ item.name }}
          </span>
        </v-chip>
        <v-label
          v-else
          :selected="selected"
          class="black--text"
          solo
          @input="selectItem(item)">
          {{ item.name }}
        </v-label>
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
</template>

<script>
import {searchWallets, searchWalletByAddress, searchWalletByTypeAndId} from '../js/AddressRegistry.js';

export default {
  props: {
    inputLabel: {
      type: String,
      default: function() {
        return null;
      },
    },
    inputPlaceholder: {
      type: String,
      default: function() {
        return null;
      },
    },
    noDataLabel: {
      type: String,
      default: function() {
        return null;
      },
    },
    autofocus: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    noAddress: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    disabled: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    bigField: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    required: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    ignoreItems: {
      type: Array,
      default: function() {
        return [];
      },
    },
  },
  data() {
    return {
      items: [],
      id: `AutoComplete${parseInt(Math.random() * 10000)
        .toString()
        .toString()}`,
      selectedValue: null,
      searchTerm: null,
      address: null,
      isLoadingSuggestions: false,
      addressLoad: '',
      currentUserItem: null,
      error: null,
    };
  },
  watch: {
    autofocus() {
      if(this.autofocus) {
        this.$refs.selectAutoComplete.focus();
      }
    },
    searchTerm(value) {
      if (!value) {
        return;
      }
      if (!this.noAddress && window.localWeb3.utils.isAddress(value)) {
        this.items.push({
          address: value,
          name: value,
          id: value,
        });
      } else if (value && value.length) {
        this.isLoadingSuggestions = true;
        return searchWallets(value).then((data) => {
          this.items = data;
          if (!this.items) {
            if (this.currentUserItem) {
              this.items = [this.currentUserItem];
            } else {
              this.items = [];
            }
          } else if (this.currentUserItem) {
            this.items.push(this.currentUserItem);
          }
        })
        .finally(() => {
          this.isLoadingSuggestions = false;
        });
      } else if (this.currentUserItem) {
        this.items = [this.currentUserItem];
      } else {
        this.items = [];
      }
    },
    selectedValue() {
      // this.$refs.selectAutoComplete.isFocused = false;
      this.addressLoad = 'loading';
      if (this.selectedValue) {
        const isAddress = this.selectedValue.indexOf('_') < 0;
        const type = isAddress ? null : this.selectedValue.substring(0, this.selectedValue.indexOf('_'));
        const id = isAddress ? this.selectedValue : this.selectedValue.substring(this.selectedValue.indexOf('_') + 1);
        if (this.noAddress || isAddress) {
          return searchWalletByAddress(this.selectedValue)
            .then(details => {
              if(details && details.type) {
                this.addressLoad = 'success';
                this.$emit('item-selected', {
                  id: details.id,
                  type: details.type,
                  address: details.address,
                  enabled: details.enabled,
                  id_type: `${details.type}_${details.id}`,
                });
              } else {
                this.addressLoad = 'success';
                this.$emit('item-selected', {
                  id: id,
                  type: null,
                  address: id,
                });
              }
            });
        } else {
          return searchWalletByTypeAndId(id, type)
            .then((data) => {
              const address = data && data.address && data.address.length && data.address.indexOf('0x') === 0 && data.address;
              if (address && address.length) {
                this.addressLoad = 'success';
                this.$emit('item-selected', {
                  id: id,
                  type: type,
                  address: address,
                });
              } else {
                this.addressLoad = 'error';
                this.$emit('item-selected', {
                  id: id,
                  type: type,
                  address: null,
                });
              }
            })
            .catch((error) => {
              console.debug('searchAddress method - error', error);
              this.addressLoad = 'error';
            });
        }
      }
    },
  },
  created() {
    searchWalletByTypeAndId(eXo.env.portal.userName, 'user').then((item) => {
      if (item) {
        item.id_type = `${item.type}_${item.id}`;
        this.currentUserItem = item;
      }
    });
  },
  methods: {
    focus() {
      this.$nextTick(() => this.$refs.selectAutoComplete.focus());
    },
    clear() {
      if (this.currentUserItem) {
        this.items = [this.currentUserItem];
      } else {
        this.items = [];
      }
      this.selectedValue = null;
      this.searchTerm = null;
      this.address = null;
      this.isLoadingSuggestions = false;
      this.addressLoad = '';
      this.error = null;
    },
    canAddItem(item) {
      return !item || !item.id || this.ignoreItems.indexOf(item.id) < 0;
    },
    filterIgnoredItems(item, queryText, itemText) {
      if (queryText && itemText.toLowerCase().indexOf(queryText.toLowerCase()) < 0) {
        return false;
      }
      if (this.ignoreItems && this.ignoreItems.length) {
        return this.canAddItem(item);
      }
      return true;
    },
    selectItem(id, type) {
      if (!id) {
        this.$refs.selectAutoComplete.selectItem(null);
      } else if (type) {
        return searchWalletByTypeAndId(id, type).then((item) => {
          item.id_type = item.type && item.id ? `${item.type}_${item.id}` : null;
          this.items.push(item);
          if (this.$refs.selectAutoComplete) {
            this.$refs.selectAutoComplete.selectItem(item);
          }
        });
      } else {
        const item = {id_type: id, name: id};
        this.items.push(item);
        if (this.$refs.selectAutoComplete) {
          this.$refs.selectAutoComplete.selectItem(item);
        }
      }
    },
  },
};
</script>
