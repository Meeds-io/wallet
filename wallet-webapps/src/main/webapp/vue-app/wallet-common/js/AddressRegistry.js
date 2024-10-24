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
/*
 * Return an Array of users and spaces that matches the filter (used in suggestion) :
 * {
 *  name: Full name,
 *  id: id,
 *  avatar: Avatar URL/URI
 * }
 */
export function searchWallets(filter) {
  let items = null;
  return searchUsers(filter)
    .then((users) => (items = users && users.length ? users : []))
    .then(() => searchSpaces(filter))
    .then((spaces) => (items = items.concat(spaces)))
    .catch((e) => {
      console.error('searchWallets method - error', e);
    });
}

/*
 * Return the address of a user or space
 */
export function saveNewAddress(id, type, address) {
  address = address.toLowerCase();
  return fetch('/portal/rest/wallet/api/account/saveAddress', {
    method: 'POST',
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      type: type,
      id: id,
      address: address,
      enabled: true,
    }),
  });
}

export function refreshWallet(wallet, refreshOnBlockchain) {
  if (!wallet && !wallet.address && !(wallet.id && wallet.type)) {
    console.error('can\'t refresh wallet with empty identifiers', wallet);
    return Promise.resolve(null);
  }
  let promise = null;
  if (refreshOnBlockchain) {
    promise = fetch(`/portal/rest/wallet/api/account/refreshWalletFromBlockchain?address=${wallet.address}`, {
      credentials: 'include',
    }).then((resp) => {
      return resp && resp.ok;
    });
  } else {
    promise = Promise.resolve(null);
  }
  return promise.then(() => {
    if (wallet.address) {
      return searchWalletByAddress(wallet.address, true).then((freshWallet) => freshWallet && Object.assign(wallet, freshWallet));
    } else {
      return searchWalletByTypeAndId(wallet.id, wallet.type).then((freshWallet) => freshWallet && Object.assign(wallet, freshWallet));
    }
  });
}

/*
 * Return the address of a user or space
 */
export function searchAddress(id, type) {
  return searchWalletByTypeAndId(id, type).then((data) => {
    if (data && data.address && data.address.length && data.address.indexOf('0x') === 0) {
      return data.address;
    } else {
      return null;
    }
  });
}

/*
 * Return the user or space object { "name": display name of space of user,
 * "id": Id of space of user, "address": Ethereum account address, "avatar":
 * avatar URL/URI, "type": 'user' or 'space', "creator": space creator username
 * for space type }
 */
export function searchWalletByTypeAndId(id, type) {
  if (window.walletSettings && window.walletSettings.wallet && window.walletSettings.wallet.id === id && window.walletSettings.wallet.type === type) {
    return Promise.resolve(window.walletSettings.wallet);
  }

  return fetch(`/portal/rest/wallet/api/account/detailsById?id=${id}&type=${type}`, {credentials: 'include'}).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    } else {
      return null;
    }
  });
}

/*
 * Searches Full name (Space or user) by usin the provided address Return {
 * "name": display name of space of user, "id": Id of space of user, "address":
 * Ethereum account address, "avatar": avatar URL/URI, "type": 'user' or 'space' }
 */
export function searchWalletByAddress(address, noCache) {
  if (!address) {
    return Promise.resolve(null);
  }

  address = address.toLowerCase();

  if (!noCache && window.walletSettings && window.walletSettings.userPreferences && window.walletSettings.wallet && window.walletSettings.wallet.address && window.walletSettings.wallet.address.toLowerCase() === address) {
    return Promise.resolve(window.walletSettings.wallet);
  }

  return fetch(`/portal/rest/wallet/api/account/detailsByAddress?address=${address}`, {credentials: 'include'})
    .then((resp) => {
      if (resp.ok) {
        return resp.json();
      } else {
        return null;
      }
    })
    .then((item) => {
      if (item && item.name && item.name.length) {
        if (!item.id_type && item.type && item.id) {
          item.id_type = `${item.type}_${item.id}`;
        }
        return item;
      }
    })
    .catch((e) => {
      console.error('searchFullName method - error', e);
    });
}

/*
 * Search users from eXo Platform, used for suggester
 */
export function searchUsers(filter, includeCurrentUserInResults) {
  const params = $.param({
    nameToSearch: filter,
    typeOfRelation: 'mention_activity_stream',
    currentUser: includeCurrentUserInResults ? '' : eXo.env.portal.userName
  });
  return fetch(`/portal/rest/social/people/suggest.json?${params}`, {credentials: 'include'})
    .then((resp) => {
      if (resp.ok) {
        return resp.json();
      } else {
        return null;
      }
    })
    .then((items) => {
      if (items) {
        if (items.options) {
          items = items.options;
        }
        items.forEach((item) => {
          if (item.id && item.id.indexOf('@') === 0) {
            item.id = item.id.substring(1);
            item.id_type = `user_${item.id}`;
          }
        });
      } else {
        items = [];
      }
      return items;
    });
}

/*
 * Search spaces from eXo Platform, used for suggester
 */
export function searchSpaces(filter, withMembers) {
  const params = $.param({fields: ['id', 'prettyName', 'displayName', 'avatarUrl'], keyword: filter});
  return fetch(`/portal/rest/space/user/searchSpace?${params}`, {credentials: 'include'})
    .then((resp) => {
      if (resp.ok) {
        return resp.json();
      } else {
        return null;
      }
    })
    .then((items) => {
      const result = [];
      items.forEach((item) => {
        result.push({
          avatar: item.avatarUrl ? item.avatarUrl : `/portal/rest/v1/social/spaces/${item.prettyName}/avatar`,
          name: item.displayName,
          id: item.prettyName,
          id_type: `space_${item.prettyName}`,
          technicalId: item.id,
          members: withMembers ? item.members : null,
        });
      });
      return result;
    });
}

export function saveAddressLabel(labelDetails) {
  return fetch('/portal/rest/wallet/api/account/saveOrDeleteAddressLabel', {
    method: 'POST',
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(labelDetails),
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    } else {
      console.error('Error saving address label', resp);
      throw new Error('Error saving address label');
    }
  });
}

/*
 * Determins the specific space from where the users could be suggested
 */


export function switchProvider(provider, address, rawMessage, signedMessage) {
  const formData = new FormData();
  formData.append('address', address);
  formData.append('provider', provider);
  formData.append('rawMessage', rawMessage);
  formData.append('signedMessage', signedMessage);

  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/wallet/api/account/provider`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: new URLSearchParams(formData).toString(),
  }).then(resp => {
    if (!resp || !resp.ok) {
      if (resp.status === 409) {
        throw new Error('wallet.addressConflict');
      } else {
        throw new Error('Error saving new provider label');
      }
    }
  });
}

export function switchInternalProvider() {
  const formData = new FormData();
  formData.append('provider', 'INTERNAL_WALLET');

  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/wallet/api/account/provider`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: new URLSearchParams(formData).toString(),
  }).then(resp => {
    if (!resp || !resp.ok) {
      throw new Error('Error saving new provider label');
    }
  });
}
