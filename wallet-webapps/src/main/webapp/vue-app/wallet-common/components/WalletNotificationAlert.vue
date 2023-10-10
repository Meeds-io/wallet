<!--
This file is part of the Meeds project (https://meeds.io/).
Copyright (C) 2022 Meeds Association
contact@meeds.io
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
<script>
export default {
  data: () => ({
    alert: null,
  }),
  computed: {
    transactionLinkLabel() {
      return this.$t('exoplatform.wallet.message.followTransaction', {0: this.walletUtils.getTransactionExplorerName()});
    },
    transactionHashLink(){
      return this.walletUtils.getTransactionEtherscanlink().concat(this.alert.transactionHash);
    },
    showTransactionLink() {
      return this.alert?.transactionHash;
    }
  },
  created() {
    this.$root.$on('wallet-notification-alert', alert => {
      this.alert = alert;
      this.$nextTick().then(() => {
        if (this.alert) {
          const detail = {
            alertMessage: this.alert.message,
            alertType: this.alert.type,
          };
          if (this.transactionHashLink) {
            detail.alertLink = this.transactionHashLink;
            detail.alertLinkText = this.transactionLinkLabel;
            detail.alertLinkTarget = '_blank';
            detail.alertLinkTooltip = this.$t('exoplatform.wallet.message.transactionExplorerLink');
          }
          document.dispatchEvent(new CustomEvent('alert-message-html', {detail}));
        } else {
          document.dispatchEvent(new CustomEvent('close-alert-message'));
        }
      });
    });
  },
};
</script>
