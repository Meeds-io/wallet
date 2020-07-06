$( document ).ready(function() {
  function initWalletTipTip(attempt) {
    if(eXo && eXo.social && eXo.social.tiptip) {
      eXo.social.tiptip.extraActions = eXo.social.tiptip.extraActions ? eXo.social.tiptip.extraActions : [];
      eXo.social.tiptip.extraActions.push({
        appendContentTo(divUIAction, ownerId, type) {
          if(!type || type === 'username') {
            divUIAction.append(`<a title="Send Funds" class="sendFundsTipTipButton" href="/portal/intranet/wallet?receiver=${ownerId}&receiver_type=user&principal=true">
                <i aria-hidden="true" class="uiIconSendFunds material-icons">send</i>
            </a>`);
          }
        }
      });
    } else if(attempt < 20) {
      setTimeout(() => initWalletTipTip(++attempt), 300);
    }
  }

  return fetch(`/portal/rest/wallet/api/settings`, {credentials: 'include'})
    .then(resp =>  {
      if (resp && resp.ok) {
        return resp.json();
      } else {
        return null;
      }
    })
    .then(settings => {
      if(settings.walletEnabled) {
        initWalletTipTip(0);
      } else {
        console.debug("Wallet disabled for current user");
      }
    })
    .catch(e => {
      console.debug("Error while initializing tiptip of Wallet", e);
    });
});
