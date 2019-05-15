export function getRewardSettings() {
  return fetch('/portal/rest/wallet/api/reward/settings', {
    method: 'GET',
    credentials: 'include',
  })
    .then((resp) => resp && resp.ok && resp.json())
    .then((settings) => (window.walletRewardSettings = settings));
}

export function saveRewardSettings(settings) {
  return fetch('/portal/rest/wallet/api/reward/settings/save', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(settings),
  }).then((resp) => {
    return resp && resp.ok;
  });
}

export function getRewardTeams() {
  return fetch('/portal/rest/wallet/api/reward/team/list', {
    method: 'GET',
    credentials: 'include',
  }).then((resp) => resp && resp.ok && resp.json());
}

export function saveRewardTeam(team) {
  return fetch('/portal/rest/wallet/api/reward/team/save', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(team),
  }).then((resp) => resp && resp.ok && resp.json());
}

export function removeRewardTeam(id) {
  return fetch(`/portal/rest/wallet/api/reward/team/remove?id=${id}`, {
    method: 'GET',
    credentials: 'include',
  }).then((resp) => resp && resp.ok);
}

export function getRewardTransactions(networkId, periodType, startDateInSeconds) {
  return fetch(`/portal/rest/wallet/api/reward/transaction/list?networkId=${networkId}&periodType=${periodType}&startDateInSeconds=${startDateInSeconds}`, {
    method: 'GET',
    credentials: 'include',
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    } else {
      throw new Error('Error getting reward transactions');
    }
  });
}

export function getRewardDates(date, periodType) {
  // convert from milliseconds to seconds
  date = parseInt(date.getTime() / 1000);
  return fetch(`/portal/rest/wallet/api/reward/settings/getDates?dateInSeconds=${date}&periodType=${periodType}`, {
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
  }).then((resp) => resp && resp.ok && resp.json());
}

export function sendRewards(periodDateInSeconds) {
  return fetch(`/portal/rest/wallet/api/reward/send?periodDateInSeconds=${periodDateInSeconds}`, {
    method: 'GET',
    credentials: 'include',
  }).then((resp) => {
    if (!resp || !resp.ok) {
      try {
        if(resp.status === 500) {
          return resp.json().then(errorResponse => {
            if (errorResponse && errorResponse.error && errorResponse.error.length) {
              throw new Error(errorResponse.error[0]);
            } else {
              throw new Error('Error sending rewards');
            }
          });
        }
      } catch(e) {
        // Ignore exception, not parsable to JSON
      }
      throw new Error('Error sending rewards');
    }
  });
}

export function computeRewards(periodDateInSeconds) {
  return fetch(`/portal/rest/wallet/api/reward/compute?periodDateInSeconds=${periodDateInSeconds}`, {
    method: 'GET',
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
  }).then((resp) => {
    if (resp) {
      try {
        return resp.json().catch(() => {
          throw new Error('Error computing rewards');
        });
      } catch (e) {
        throw new Error('Error computing rewards');
      }
    } else {
      throw new Error('Error computing rewards');
    }
  });
}
