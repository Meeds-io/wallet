$( document ).ready(function() {
  function initWalletTipTip(tentative) {
    if(eXo && eXo.social && eXo.social.tiptip) {
      eXo.social.tiptip.extraActions = eXo.social.tiptip.extraActions ? eXo.social.tiptip.extraActions : [];
      eXo.social.tiptip.extraActions.push({
        appendContentTo(divUIAction, ownerId, type) {
          if(!type || type === 'username') {
            divUIAction.append(`<a title="Send Funds" class="btn sendFundsTipTipButton" href="/portal/intranet/wallet?receiver=${ownerId}&receiver_type=user&principal=true">
                <i aria-hidden="true" class="uiIconSendFunds material-icons">send</i>
            </a>`);
          }
        }
      });
    } else if(tentative < 20) {
      setTimeout(() => initWalletTipTip(++tentative), 300);
    }
  }

  return fetch(`/portal/rest/wallet/api/global-settings`, {credentials: 'include'})
    .then(resp =>  {
      if (resp && resp.ok) {
        return resp.json();
      } else {
        return null;
      }
    })
    .then(settings => {
      if(settings.isWalletEnabled) {
        initWalletTipTip(0);
      } else {
        console.debug("Wallet disabled for current user");
      }
    })
    .catch(e => {
      console.debug("Error while initializing tiptip of Wallet", e);
    });
});
