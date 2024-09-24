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

export function getRewardSettings() {
  return fetch('/wallet/rest/settings/reward', {
    method: 'GET',
    credentials: 'include',
  })
    .then((resp) => resp && resp.ok && resp.json());
}

export function saveRewardSettings(settings) {
  return fetch('/wallet/rest/settings/reward', {
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

export function deleteRewardSettings() {
  return fetch('/wallet/rest/settings/reward', {
    method: 'DELETE',
    credentials: 'include',
  }).then(resp => {
    if (resp && resp.ok) {
      return resp.json;
    } else {
      throw new Error('Error when deleting rewarding settings');
    }
  });
}

export function getRewardDates(date) {
  return fetch(`/wallet/rest/settings/reward/getDates?date=${date}`, {
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
  }).then((resp) => resp && resp.ok && resp.json());
}

export function sendRewards(period) {
  return fetch('/wallet/rest/reward/send', {
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json'
    },
    method: 'POST',
    body: JSON.stringify({
      id: period?.id,
      rewardPeriodType: period?.rewardPeriodType,
      timeZone: period?.timeZone,
      startDateInSeconds: period?.startDateInSeconds,
      endDateInSeconds: period?.endDateInSeconds
    })
  }).then((resp) => {
    if (!resp || !resp.ok) {
      try {
        if (resp.status === 500) {
          return resp.json().then(errorResponse => {
            if (errorResponse && errorResponse.error && errorResponse.error.length) {
              throw new Error(errorResponse.error[0]);
            } else {
              throw new Error('Error sending rewards');
            }
          });
        }
      } catch (e) {
        // Ignore exception, not parsable to JSON
      }
      throw new Error('Error sending rewards');
    }
  });
}

export function computeRewards(page, size) {
  return fetch(`/wallet/rest/reward/compute?page=${page}&size=${size}`, {
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

export function computeRewardsByPeriod(period) {
  return fetch('/wallet/rest/reward/period/compute', {
    method: 'POST',
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      id: period?.id,
      rewardPeriodType: period?.rewardPeriodType,
      timeZone: period?.timeZone,
      startDateInSeconds: period?.startDateInSeconds,
      endDateInSeconds: period?.endDateInSeconds
    })
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

export function computeRewardsByUser(date) {
  return fetch(`/wallet/rest/reward/compute/user?date=${date}`, {
    method: 'GET',
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    } else {
      throw new Error ('Error computing rewards');
    }
  });
}

export function getRewardsByUser(limit) {
  return fetch(`/wallet/rest/reward/list?limit=${limit || 10}`, {
    method: 'GET',
    credentials: 'include',
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error while getting user rewards', resp);
    } else {
      return resp.json();
    }
  });
}

export function getRewardReportPeriods(paramsObj) {
  const formData = new FormData();
  if (paramsObj) {
    Object.keys(paramsObj).forEach(key => {
      const value = paramsObj[key];
      if (window.Array && Array.isArray && Array.isArray(value)) {
        value.forEach(val => formData.append(key, val));
      } else {
        formData.append(key, value);
      }
    });
  }
  const params = new URLSearchParams(formData).toString();
  return fetch(`/wallet/rest/reward/periods?${params}`, {
    method: 'GET',
    credentials: 'include',
  }).then((resp) => {
    if (resp?.ok) {
      return resp.json();
    } else {
      throw new Error('Error when getting report periods');
    }
  });
}
