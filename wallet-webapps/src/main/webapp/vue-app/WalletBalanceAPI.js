export function getWalletAccount() {
    return fetch(`/portal/rest/wallet/api/account/detailsById?id=${eXo.env.portal.userName}&type=user`, {
        method: 'GET',
    }).then((resp) => {
      if(resp && resp.ok) {
        return resp.json();
      } else {
        throw new Error ('Error when getting wallet account');
      }
    })
}