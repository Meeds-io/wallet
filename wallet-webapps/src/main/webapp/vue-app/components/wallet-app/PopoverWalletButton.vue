<template>    
  <v-tooltip bottom>
    <template v-slot:activator="{ on, attrs }">
      <div
        v-bind="attrs"
        v-on="on">
        <v-btn
          :ripple="false"
          icon
          color="primary"
          @click="sendWallet($event)">
          <v-icon size="18" class="font-weight-bold error-color pa-2 mt-n1">Ɱ</v-icon>
        </v-btn>
      </div>
    </template>
    <span>
      {{ $t('exoplatform.wallet.title.sendMeeds') }} 
    </span>
  </v-tooltip>
</template>
<script>
export default {
  props: {
    identity: {
      type: Object,
      default: null,
    }
  },
  computed: {
    identityId() {
      return this.identity && this.identity.id;
    }
  },
  methods: {
    sendWallet(event) {
      event.preventDefault();
      event.stopPropagation();
      const type = this.identity && this.identity.username ? 'user' : 'space';
      const name = this.identity && this.identity.username ? this.identity.username : this.identity.prettyName;
      window.location.href = `${eXo.env.portal.context}/${eXo.env.portal.portalName}/wallet?receiver=${name}&receiver_type=${type}&principal=true`;
    }
  }
};
</script>
