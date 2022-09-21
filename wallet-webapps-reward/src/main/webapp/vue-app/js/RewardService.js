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
import TIMEZONE_IDS from '../json/timezones.json';
export const TIMEZONES = [];

export function getTimeZones() {
  if (TIMEZONES.length) {
    return TIMEZONES;
  }
  const dateObj = new Date(0);
  TIMEZONE_IDS.forEach((timeZone) => {
    const dateFormat = new Intl.DateTimeFormat(eXo.env.portal.language, {
      timeZoneName: 'long',
      second: 'numeric',
      timeZone: timeZone,
    });
    const timeZoneName = dateFormat.format(dateObj);
    TIMEZONES.push({
      value: timeZone,
      text: `${timeZoneName.charAt(2).toUpperCase() + timeZoneName.substring(3, timeZoneName.length)  } (${timeZone})`,
    });
  });
  return TIMEZONES;
}

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

export function getRewardDates(date) {
  return fetch(`/portal/rest/wallet/api/reward/settings/getDates?date=${date}`, {
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
  }).then((resp) => resp && resp.ok && resp.json());
}

export function sendRewards(date) {
  return fetch(`/portal/rest/wallet/api/reward/send?date=${date}`, {
    method: 'GET',
    credentials: 'include',
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

export function computeRewards(date) {
  return fetch(`/portal/rest/wallet/api/reward/compute?date=${date}`, {
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
