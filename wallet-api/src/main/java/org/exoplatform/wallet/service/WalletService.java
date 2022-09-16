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
package org.exoplatform.wallet.service;

import org.exoplatform.wallet.model.ContractDetail;
import org.exoplatform.wallet.model.settings.*;
import org.exoplatform.wallet.model.transaction.FundsRequest;

/**
 * A storage service to save/load information used by users and spaces wallets
 */
public interface WalletService {

  /**
   * Save initial funds
   * 
   * @param initialFundsSettings initial funds to save
   */

  void saveInitialFundsSettings(InitialFundsSettings initialFundsSettings);

  /**
   * @return saved initial funds in internal store
   */
  InitialFundsSettings getInitialFundsSettings();

  /**
   * Retrieves global stored settings used for all users.
   * 
   * @return {@link GlobalSettings} global settings of default watched
   *         blockchain network
   */
  GlobalSettings getSettings();

  /**
   * Retrieves user settings including global setting, network settings and
   * contract detail. if username is not null, the personal settings will be
   * included. if spaceId is not null wallet address will be retrieved
   * 
   * @param spaceId space pretty name to include its settings
   * @param currentUser username to include its preferences
   * @param isAdministration whether the settings to retrieve are for
   *          administration or not
   * @return {@link UserSettings} user settings with user and space preferences
   *         included into it
   */
  UserSettings getUserSettings(String spaceId, String currentUser, boolean isAdministration);

  /**
   * Save user preferences of Wallet
   * 
   * @param currentUser current user name to save its preferences
   * @param userPreferences user preferences to save
   */
  void saveUserPreferences(String currentUser, WalletSettings userPreferences);

  /**
   * Save funds request and send notifications
   * 
   * @param fundsRequest funds request details to save
   * @param currentUser username of user sending request
   * @throws IllegalAccessException if request sender is not allowed to send
   *           request to receiver wallet
   */
  void requestFunds(FundsRequest fundsRequest, String currentUser) throws IllegalAccessException;

  /**
   * Mark a fund request web notification as sent
   * 
   * @param notificationId web notification id
   * @param currentUser current username that is marking the notification as
   *          sent
   * @throws IllegalAccessException if current user is not the targetted user of
   *           notification
   */
  void markFundRequestAsSent(String notificationId, String currentUser) throws IllegalAccessException;

  /**
   * Get fund request status
   * 
   * @param notificationId web notification id
   * @param currentUser current username
   * @return true if fund request sent
   * @throws IllegalAccessException if current user is not the targetted user of
   *           notification
   */
  boolean isFundRequestSent(String notificationId, String currentUser) throws IllegalAccessException;

  /**
   * Sets contract detail object in global settings
   * 
   * @param contractDetail
   */
  void setConfiguredContractDetail(ContractDetail contractDetail);

  /**
   * @return true if wallet admin is enabled on Token contract else return false
   */
  boolean isEnabled();

  /**
   * Sets gas price from blockchain network
   * 
   * @param blockchainGasPrice gas price in WEI
   */
  void setGasPrice(double blockchainGasPrice);

  /**
   * @return gas price retrieved from blockchain
   */
  double getGasPrice();

}
