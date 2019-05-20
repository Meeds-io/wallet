<template>
  <v-flex id="walletMetamaskSetup">
    <div v-if="!metamaskEnabled" class="alert alert-info">
      <i class="uiIconInfo"></i> <span v-if="walletAddress">
        Current wallet is in readonly mode.
      </span> <br v-if="walletAddress">
      <span>
        Please install or enable Metamask extension in your browser
      </span> <br>
      <button class="btn" @click.stop="installInstructionDialog = true">
        See help
      </button> <div>
        Or
      </div> <button class="btn" @click.stop="disableMetamaskUsage">
        Switch to use browser wallet
      </button>

      <v-dialog
        v-model="installInstructionDialog"
        content-class="uiPopup with-overflow"
        width="500px"
        max-width="100wv"
        @keydown.esc="installInstructionDialog = false">
        <v-card>
          <div class="popupHeader ClearFix">
            <a
              class="uiIconClose pull-right"
              aria-hidden="true"
              @click="installInstructionDialog = false"></a> <span class="PopupTitle popupTitle">
                Enable wallet application
              </span>
          </div>
          <v-card-text>
            To access your wallet you 'll need to:
            <ol type="1">
              <li>
                Install/enable <a target="about:blank" href="https://metamask.io/">
                  Metamask
                </a> in your browser
              </li>
              <li>
                Follow setup instructions on Metamask browser plugin
              </li>
              <li>
                Connect to Metamask account
              </li>
              <li v-if="networkLabel && networkLabel.length">
                Switch Metamask network to <strong>
                  {{ networkLabel }}
                </strong>
              </li>
              <li>
                Associate the automatically generated account address from Metamask to your profile (a box will be displayed automaticatty once you enable Metamask on browser)
              </li>
            </ol>
          </v-card-text>
          <v-card-actions>
            <v-spacer />
            <button class="btn" @click="installInstructionDialog = false">
              Close
            </button>
            <v-spacer />
          </v-card-actions>
        </v-card>
      </v-dialog>
    </div>
    <div v-else-if="!metamaskConnected" class="alert alert-warning">
      <i class="uiIconWarning"></i> Please connect to Metamask
    </div> <div v-else-if="error" class="alert alert-error">
      <i class="uiIconError"></i> {{ error }}
    </div> <div v-else-if="displaySpaceMetamaskEnableHelp" class="alert alert-info">
      <i class="uiIconInfo"></i> Please enable/install Metamask to be able to add a new space account
    </div>
    <div v-else-if="displayNotSameNetworkWarning" class="alert alert-warning">
      <i class="uiIconWarning"></i> Please switch Metamask to <strong>
        {{ networkLabel }}
      </strong>
    </div>
    <div v-else-if="isAdministration && isPrincipalContractAdmin" class="alert alert-info">
      <i class="uiIconInfo"></i> <span>
        You are using <code>Admin</code> address in metamask
      </span>
    </div>
    <div v-else-if="isAdministration && principalContractAdminAddress && !isPrincipalContractAdmin" class="alert alert-warning">
      <i class="uiIconWarning"></i> Attention: you are using an account different from<code>Admin</code> account
    </div>
    <div v-else-if="newAddressDetected">
      <div v-if="associatedWalletAddress" class="alert alert-warning">
        <i class="uiIconWarning"></i> <span v-if="isAdministration">
          Attention: you are using a different metamask account from your associated address {{ associatedWalletAddress }}
        </span> <span v-else>
          Please switch metamask to {{ associatedWalletAddress }} account to be able to send transactions
        </span>
      </div>
      <br>
      <div v-if="displayAddressAssociationBox" class="alert alert-info">
        <i class="uiIconInfo"></i> <span>
          A new wallet has been detected on Metamask!
        </span> <br>
        <button class="btn" @click.stop="addressAssociationDialog = true">
          See details
        </button>
        <v-dialog
          v-model="addressAssociationDialog"
          content-class="uiPopup with-overflow"
          width="500px"
          max-width="100wv"
          @keydown.esc="addressAssociationDialog = false">
          <v-card>
            <div class="popupHeader ClearFix">
              <a
                class="uiIconClose pull-right"
                aria-hidden="true"
                @click="addressAssociationDialog = false"></a> <span class="PopupTitle popupTitle">
                  Configure your wallet address
                </span>
            </div>
            <v-card-text>
              <div v-if="currentAccountAlreadyInUse">
                Currently selected account in Metamask is already in use, you can't use it in this wallet.
              </div> <div v-else-if="displaySpaceAccountAssociationHelp">
                Would you like to use the current address <wallet-address :value="detectedMetamaskAccount" /> in Space Wallet ?
              </div> <div v-else-if="displayUserAccountAssociationHelp">
                Would you like to use the current address <wallet-address :value="detectedMetamaskAccount" /> in your Wallet ?
              </div> <div v-else-if="displayUserAccountChangeHelp">
                Would you like to replace your wallet address <wallet-address :value="associatedWalletAddress" /> by the current address <wallet-address :value="detectedMetamaskAccount" /> ?
              </div>
            </v-card-text>

            <v-card-actions v-if="displayAccountHelpActions" class="text-xs-center">
              <v-spacer />
              <button class="btn btn-primary mr-2" @click="saveNewAddressInWallet()">
                Yes
              </button> <button class="btn" @click="addressAssociationDialog = false">
                No
              </button>
              <v-spacer />
            </v-card-actions>
          </v-card>
        </v-dialog>
      </div>
    </div>
  </v-flex>
</template>

<script>
import WalletAddress from './WalletAddress.vue';
import * as constants from '../js/Constants.js';

import {setDraggable, disableMetamask} from '../js/WalletUtils.js';
import {searchWalletByAddress, saveNewAddress} from '../js/AddressRegistry.js';

export default {
  components: {
    WalletAddress,
  },
  props: {
    isSpace: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    isSpaceAdministrator: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    loading: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    isAdministration: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    walletAddress: {
      type: String,
      default: function() {
        return null;
      },
    },
    refreshIndex: {
      type: Number,
      default: function() {
        return 0;
      },
    },
  },
  data() {
    return {
      addressAssociationDialog: false,
      installInstructionDialog: false,
      networkLabel: null,
      principalContractAdminAddress: null,
      sameConfiguredNetwork: true,
      associatedWalletAddress: null,
      detectedMetamaskAccount: null,
      currentAccountAlreadyInUse: false,
      metamaskEnabled: false,
      metamaskConnected: false,
    };
  },
  computed: {
    displayNotSameNetworkWarning() {
      return !this.loading && this.refreshIndex && !this.sameConfiguredNetwork && (!this.isSpace || !this.associatedWalletAddress);
    },
    isPrincipalContractAdmin() {
      return this.principalContractAdminAddress && this.detectedMetamaskAccount && this.principalContractAdminAddress.toLowerCase() === this.detectedMetamaskAccount.toLowerCase();
    },
    displaySpaceAccountAssociationHelp() {
      return !this.loading && this.refreshIndex && this.isSpace && this.sameConfiguredNetwork && !this.associatedWalletAddress && this.detectedMetamaskAccount;
    },
    displayUserAccountAssociationHelp() {
      return !this.loading && this.refreshIndex && !this.isSpace && this.sameConfiguredNetwork && !this.associatedWalletAddress && this.detectedMetamaskAccount;
    },
    displayAddressAssociationBox() {
      // Display dialog association only when it's not a space or the space doesn't have an associated address yet
      return !this.loading && this.refreshIndex && !this.isAdministration && (!this.isSpace || !this.associatedWalletAddress);
    },
    displayUserAccountChangeHelp() {
      return !this.loading && this.refreshIndex && !this.isSpace && this.sameConfiguredNetwork && this.associatedWalletAddress && this.detectedMetamaskAccount;
    },
    displayAccountHelpActions() {
      return !this.loading && this.refreshIndex && this.sameConfiguredNetwork && !this.currentAccountAlreadyInUse && (this.displayUserAccountChangeHelp || this.displayUserAccountAssociationHelp || this.displaySpaceAccountAssociationHelp);
    },
    displaySpaceMetamaskEnableHelp() {
      return !this.loading && this.refreshIndex && this.isSpace && !this.associatedWalletAddress && !this.detectedMetamaskAccount;
    },
    newAddressDetected() {
      return !this.loading && this.refreshIndex && this.sameConfiguredNetwork && this.detectedMetamaskAccount && this.associatedWalletAddress !== this.detectedMetamaskAccount && (!this.isSpace || this.isSpaceAdministrator);
    },
  },
  created() {
    this.init();
    this.$nextTick(() => {
      setDraggable();
    });
  },
  methods: {
    init() {
      if (!window.walletSettings) {
        return;
      }

      this.metamaskEnabled = window.web3 && window.web3.currentProvider;
      this.metamaskConnected = this.metamaskEnabled && window.walletSettings.metamaskConnected;

      this.associatedWalletAddress = window.walletSettings.userPreferences.walletAddress;

      this.addressAssociationDialog = false;
      this.installInstructionDialog = false;
      this.networkLabel = null;
      this.sameConfiguredNetwork = true;
      this.detectedMetamaskAccount = null;
      this.currentAccountAlreadyInUse = false;
      this.principalContractAdminAddress = window.walletSettings.principalContractAdminAddress;

      if (this.metamaskEnabled && this.metamaskConnected) {
        this.detectedMetamaskAccount = window.walletSettings.detectedMetamaskAccount;

        this.sameConfiguredNetwork = window.walletSettings.defaultNetworkId === window.walletSettings.currentNetworkId;
        this.networkLabel = constants.NETWORK_NAMES[window.walletSettings.defaultNetworkId];
        if (!this.networkLabel) {
          this.networkLabel = window.walletSettings.providerURL;
        }

        // compute detected account associated user/space
        if (this.detectedMetamaskAccount !== this.associatedWalletAddress) {
          return this.initAccount();
        }
      }
    },
    initAccount() {
      return searchWalletByAddress(this.detectedMetamaskAccount)
        .then((item, error) => {
          if (error) {
            throw error;
          }
          this.currentAccountAlreadyInUse = item && item.id && item.id.length;
          return item;
        })
        .catch((e) => {
          console.debug('searchAddress method - error', e);
        });
    },
    saveNewAddressInWallet() {
      return saveNewAddress(this.isSpace ? window.walletSpaceGroup : eXo.env.portal.userName, this.isSpace ? 'space' : 'user', this.detectedMetamaskAccount)
        .then((resp, error) => {
          if (error) {
            throw error;
          }
          if (resp && resp.ok) {
            this.$emit('refresh');
            this.init();
          } else {
            this.error = 'Error saving new Wallet address';
          }
        })
        .catch((e) => {
          console.debug('saveNewAddress method - error', e);
          this.$emit('error', `Error saving new Wallet address: ${e}`);
        });
    },
    disableMetamaskUsage() {
      disableMetamask(this.isSpace);
      this.$emit('refresh');
    },
  },
};
</script>
