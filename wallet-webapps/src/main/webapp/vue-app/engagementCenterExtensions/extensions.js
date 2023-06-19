const userActions = ['createWallet'];
export function init() {
  extensionRegistry.registerExtension('engagementCenterActions', 'user-actions', {
    type: 'wallet',
    options: {
      rank: 50,
      icon: 'fas fa-wallet',
      match: (actionLabel) => userActions.includes(actionLabel),
      getLink: (realization) => {
        Vue.prototype.$set(realization, 'link', `${eXo.env.portal.context}/${eXo.env.portal.portalName}/wallet`);
      }
    },
  });
}