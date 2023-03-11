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
export const FIAT_CURRENCIES = {
  aud: {
    value: 'aud',
    text: 'Australia Dollar (AUD)',
    symbol: '$ (AUD)',
  },
  brl: {
    value: 'brl',
    text: 'Brazil Real (R$)',
    symbol: 'R$',
  },
  cad: {
    value: 'cad',
    text: 'Canadian dollar (CAD)',
    symbol: '$ (CAD)',
  },
  chf: {
    value: 'chf',
    text: 'Switzerland Franc (CHF)',
    symbol: 'CHF',
  },
  clp: {
    value: 'clp',
    text: 'Chile Peso (CLP)',
    symbol: '$ (CLP)',
  },
  cny: {
    value: 'cny',
    text: 'China Yuan Renminbi (CNY)',
    symbol: '¥ (CNY)',
  },
  czk: {
    value: 'czk',
    text: 'Czech Republic Koruna (Kč)',
    symbol: 'Kč',
  },
  dkk: {
    value: 'dkk',
    text: 'Denmark Krone (DKK)',
    symbol: 'kr (DKK)',
  },
  eur: {
    value: 'eur',
    text: 'Euro Member Countries (€)',
    symbol: '€',
  },
  gbp: {
    value: 'gbp',
    text: 'United Kingdom Pound (£)',
    symbol: '£',
  },
  hkd: {
    value: 'hkd',
    text: 'Hong Kong Dollar (HKD)',
    symbol: '$ (HKD)',
  },
  huf: {
    value: 'huf',
    text: 'Hungary Forint (Ft)',
    symbol: 'Ft',
  },
  idr: {
    value: 'idr',
    text: 'Indonesia Rupiah (Rp)',
    symbol: 'Rp',
  },
  inr: {
    value: 'inr',
    text: 'India Rupee (INR)',
    symbol: 'INR',
  },
  jpy: {
    value: 'jpy',
    text: 'Japan Yen (¥)',
    symbol: '¥',
  },
  krw: {
    value: 'krw',
    text: 'Korea (South) Won (₩)',
    symbol: '₩',
  },
  mxn: {
    value: 'mxn',
    text: 'Mexico Peso (MXN)',
    symbol: '$ (MXN)',
  },
  myr: {
    value: 'myr',
    text: 'Malaysia Ringgit (RM)',
    symbol: 'RM',
  },
  nok: {
    value: 'nok',
    text: 'Norway Krone (NOK)',
    symbol: 'kr (NOK)',
  },
  nzd: {
    value: 'nzd',
    text: 'New Zealand Dollar (NZD)',
    symbol: '$ (NZD)',
  },
  php: {
    value: 'php',
    text: 'Philippines Piso (₱)',
    symbol: '₱',
  },
  pkr: {
    value: 'pkr',
    text: 'Pakistan Rupee (₨)',
    symbol: '₨',
  },
  pln: {
    value: 'pln',
    text: 'Poland Zloty (zł)',
    symbol: 'zł',
  },
  usd: {
    value: 'usd',
    text: 'United States Dollar ($)',
    symbol: '$',
  },
  rub: {
    value: 'rub',
    text: 'Russia Ruble (₽)',
    symbol: '₽',
  },
  sek: {
    value: 'sek',
    text: 'Sweden Krona (SEK)',
    symbol: 'kr (SEK)',
  },
  sgd: {
    value: 'sgd',
    text: 'Singapore Dollar (SGD)',
    symbol: '$ (SGD)',
  },
  thb: {
    value: 'thb',
    text: 'Thailand Baht (THB)',
    symbol: '฿ (THB)',
  },
  try: {
    value: 'try',
    text: 'Turkey Lira (TRY)',
    symbol: 'TRY',
  },
  twd: {
    value: 'twd',
    text: 'Taiwan New Dollar (NT$)',
    symbol: 'NT$',
  },
  zar: {
    value: 'zar',
    text: 'South Africa Rand (ZAR)',
    symbol: 'R (ZAR)',
  },
};

export const NETWORK_NAMES = {
  0: '',
  1: 'Ethereum Main',
  2: 'Ethereum Classic main',
  3: 'Ropsten',
  4: 'Rinkeby',
  42: 'Kovan',
};

export const OK = 'OK';
export const ERROR_WALLET_NOT_CONFIGURED = 'ERROR_WALLET_NOT_CONFIGURED';
export const ERROR_WALLET_SETTINGS_NOT_LOADED = 'ERROR_WALLET_SETTINGS_NOT_LOADED';
export const ERROR_WALLET_DISCONNECTED = 'ERROR_WALLET_DISCONNECTED';
