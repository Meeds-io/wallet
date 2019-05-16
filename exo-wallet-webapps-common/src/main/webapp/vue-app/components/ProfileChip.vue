<template>
  <a
    v-if="profileId"
    :id="id"
    :title="profileId"
    :href="url"
    rel="nofollow"
    target="_blank">
    <template v-if="disapproved">
      <del class="red--text">{{ displayName }}</del> (Disapproved)
    </template>
    <template v-else-if="deletedUser">
      <del class="red--text">{{ displayName }}</del> (Deleted)
    </template>
    <template v-else-if="disabledUser">
      <del class="red--text">{{ displayName }}</del> (Disabled user)
    </template>
    <template v-else-if="displayNoAddress && !address">
      <del class="red--text">{{ displayName }}</del> (No wallet)
    </template>
    <template v-else-if="!enabled">
      <del class="red--text">{{ displayName }}</del> (Disabled wallet)
    </template>
    <template v-else-if="disabledInRewardPool">
      {{ displayName }} <span class="red--text">(Disabled pool)</span>
    </template>
    <template v-else-if="initializationState !== 'INITIALIZED'">
      {{ displayName }} <span class="orange--text">(Not initialized)</span>
    </template>
    <template v-else>
      {{ displayName }}
    </template>
  </a>
  <code v-else-if="displayName">
    <template v-if="enabled">
      {{ displayName }}
    </template>
    <span v-else>
      <del class="red--text">{{ displayName }}</del> (Disabled)
    </span>
  </code>
  <wallet-address
    v-else
    :value="address"
    display-label />
</template>

<script>
import WalletAddress from './WalletAddress.vue';

export default {
  components: {
    WalletAddress,
  },
  props: {
    profileId: {
      type: String,
      default: function() {
        return null;
      },
    },
    spaceId: {
      type: String,
      default: function() {
        return null;
      },
    },
    profileTechnicalId: {
      type: String,
      default: function() {
        return null;
      },
    },
    profileType: {
      type: String,
      default: function() {
        return null;
      },
    },
    avatar: {
      type: String,
      default: function() {
        return null;
      },
    },
    displayName: {
      type: String,
      default: function() {
        return null;
      },
    },
    address: {
      type: String,
      default: function() {
        return null;
      },
    },
    tiptipPosition: {
      type: String,
      default: function() {
        return null;
      },
    },
    enabled: {
      type: Boolean,
      default: function() {
        return true;
      },
    },
    disabledInRewardPool: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    displayNoAddress: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    disapproved: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    deletedUser: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    disabledUser: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    initializationState: {
      type: String,
      default: function() {
        return 'INITIALIZED';
      },
    },
  },
  data() {
    return {
      id: `chip${parseInt(Math.random() * 10000)
        .toString()
        .toString()}`,
      labels: {
        CancelRequest: 'Cancel Request',
        Confirm: 'Confirm',
        Connect: 'Connect',
        Ignore: 'Ignore',
        RemoveConnection: 'Remove Connection',
        StatusTitle: 'Loading...',
        join: 'Join',
        leave: 'Leave',
        members: 'Members',
      },
    };
  },
  computed: {
    url() {
      if (!this.profileType || this.profileType === 'user') {
        return `${eXo.env.portal.context}/${eXo.env.portal.portalName}/profile/${this.profileId}`;
      } else if (this.profileType === 'space') {
        return `${eXo.env.portal.context}/g/:spaces:${this.profileId}/`;
      }
      return '#';
    },
  },
  watch: {
    profileId(oldValue, newValue) {
      if (this.profileId) {
        // TODO disable tiptip because of high CPU usage using its code
        this.initTiptip();
      }
    },
  },
  created() {
    if (this.profileId && (this.profileType === 'user' || this.profileType === 'space')) {
      // TODO disable tiptip because of high CPU usage using its code
      this.initTiptip();
    }
  },
  methods: {
    initTiptip() {
      if (this.profileType === 'space') {
        this.$nextTick(() => {
          $(`#${this.id}`).spacePopup({
            userName: eXo.env.portal.userName,
            spaceID: this.spaceId,
            restURL: '/portal/rest/v1/social/spaces/{0}',
            membersRestURL: '/portal/rest/v1/social/spaces/{0}/users?returnSize=true',
            managerRestUrl: '/portal/rest/v1/social/spaces/{0}/users?role=manager&returnSize=true',
            membershipRestUrl: '/portal/rest/v1/social/spacesMemberships?space={0}&returnSize=true',
            defaultAvatarUrl: this.avatar ? this.avatar : `/portal/rest/v1/social/spaces/${this.profileId}/avatar`,
            deleteMembershipRestUrl: '/portal/rest/v1/social/spacesMemberships/{0}:{1}:{2}',
            labels: this.labels,
            content: false,
            keepAlive: true,
            defaultPosition: this.tiptipPosition || 'left_bottom',
            maxWidth: '240px',
          });
        });
      } else {
        this.$nextTick(() => {
          $(`#${this.id}`).userPopup({
            restURL: '/portal/rest/social/people/getPeopleInfo/{0}.json',
            userId: this.profileId,
            labels: this.labels,
            content: false,
            keepAlive: true,
            defaultPosition: this.tiptipPosition || 'left_bottom',
            maxWidth: '240px',
          });
        });
      }
    },
  },
};
</script>
