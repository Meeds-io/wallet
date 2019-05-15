<template>
  <v-dialog
    v-model="dialog"
    attach="#walletDialogsParent"
    content-class="uiPopup"
    width="500px"
    max-width="100vw"
    @keydown.esc="dialog = false">
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix">
        <a
          class="uiIconClose pull-right"
          aria-hidden="true"
          @click="dialog = false"></a>
        <span class="PopupTitle popupTitle">
          Add Token address
        </span>
      </div>
      <v-card-text>
        <div v-if="error && !loading" class="alert alert-error v-content">
          <i class="uiIconError"></i>{{ error }}
        </div>
        <v-form
          @submit="
            $event.preventDefault();
            $event.stopPropagation();
          ">
          <v-text-field
            v-if="dialog"
            v-model="address"
            :disabled="loading"
            name="address"
            label="Address"
            placeholder="Select ERC20 Token address"
            type="text"
            autofocus />
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <button
          :disabled="loading"
          :loading="loading"
          class="btn btn-primary"
          @click="addToken">
          Save
        </button>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>

export default {
  props: {
    open: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    account: {
      type: String,
      default: function() {
        return null;
      },
    },
    netId: {
      type: Number,
      default: function() {
        return 0;
      },
    },
    isDefaultContract: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
  },
  data() {
    return {
      dialog: false,
      loading: null,
      error: null,
      address: null,
    };
  },
  watch: {
    open() {
      if (this.open) {
        this.dialog = true;
        this.$nextTick(() => {
          this.walletUtils.setDraggable();
        });
      }
    },
    dialog() {
      if (!this.dialog) {
        this.$emit('close');
      }
    },
  },
  methods: {
    addToken() {
      this.error = null;
      if (!this.address || !window.localWeb3.utils.isAddress(this.address.trim())) {
        this.error = 'Invalid address';
        return;
      }
      this.loading = true;
      try {
        return this.tokenUtils.saveContractAddress(this.account, this.address.trim().toLowerCase(), this.netId, this.isDefaultContract)
          .then((added, error) => {
            if (error) {
              throw error;
            }
            if (added) {
              this.$emit('added', this.address);
              this.dialog = false;
              this.address = null;
            } else {
              this.error = `Address is not recognized as ERC20 Token contract's address`;
            }
            this.loading = false;
          })
          .catch((err) => {
            console.debug('saveContractAddress method - error', err);
            this.loading = false;
            this.error = `${err}`;
          });
      } catch (e) {
        console.debug('saveContractAddress method - error', e);
        this.loading = false;
        this.error = `${e}`;
      }
    },
  },
};
</script>
