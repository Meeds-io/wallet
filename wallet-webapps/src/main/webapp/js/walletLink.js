/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
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
