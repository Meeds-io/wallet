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

import java.math.BigInteger;
import java.util.Set;

import org.exoplatform.wallet.model.ContractDetail;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.transaction.TransactionDetail;

/**
 * Communicates with blockchain to send transactions and retrieve information
 * using admin wallet
 */
public interface WalletTokenAdminService {

  /**
   * Generates admin account wallet and store it internally in eXo Server
   */
  void createAdminAccount();

  /**
   * Creates admin account wallet using provided private key and store it
   * internally in eXo Server
   * 
   * @param privateKey admin account wallet private key
   * @param issuerUsername current user creating wallet
   * @return created wallet
   * @throws IllegalAccessException if current user is not allowed to create
   *           admin wallet account
   */
  Wallet createAdminAccount(String privateKey, String issuerUsername) throws IllegalAccessException;

  /**
   * @return Admin wallet address
   */
  String getAdminWalletAddress();

  /**
   * @param rawTransaction raw transaction to send to blockchain
   * @return real generated transaction hash using Web3j
   */
  String generateHash(String rawTransaction);

  /**
   * Send rewarded token amounts (on blockchain) to a receiver wallet address
   * using 'Admin' wallet. The amount sent could be different from rewarded
   * amount. A label and a message are associated to the transaction. Those
   * properties aren't sent on blockchain, but stored in database. The
   * transaction issuer will be stored in transaction details stored internally
   * in eXo server.
   *
   * @param transactionDetail
   * @param issuerUsername
   * @return {@link TransactionDetail} with the hash of the transaction sent in
   *         blockchain
   * @throws Exception
   */
  TransactionDetail reward(TransactionDetail transactionDetail, String issuerUsername) throws Exception;// NOSONAR

  /**
   * Send ether (on blockchain) to a receiver wallet address using 'Admin'
   * wallet. The transaction issuer, label and message will be stored in
   * transaction details inside eXo server only.
   * 
   * @param transactionDetail
   * @param issuerUsername
   * @return {@link TransactionDetail} with the hash of the transaction sent in
   *         blockchain
   * @throws Exception
   */
  TransactionDetail sendEther(TransactionDetail transactionDetail, String issuerUsername) throws Exception;// NOSONAR

  /**
   * Send token (on blockchain) to a receiver wallet address using 'Admin'
   * wallet. The transaction issuer, label and message will be stored in
   * transaction details inside eXo server only.
   * 
   * @param transactionDetail
   * @param issuerUsername
   * @return {@link TransactionDetail} with the hash of the transaction sent in
   *         blockchain
   * @throws Exception
   */
  TransactionDetail sendToken(TransactionDetail transactionDetail, String issuerUsername) throws Exception; // NOSONAR

  /**
   * Get token balance of a wallet address (on blockchain)
   * 
   * @param address
   * @return {@link TransactionDetail} with the hash of the transaction sent in
   *         blockchain
   * @throws Exception
   */
  BigInteger getTokenBalanceOf(String address) throws Exception;// NOSONAR

  /**
   * Get ether balance of a wallet address (on blockchain)
   * 
   * @param address
   * @return {@link TransactionDetail} with the hash of the transaction sent in
   *         blockchain
   * @throws Exception
   */
  BigInteger getEtherBalanceOf(String address) throws Exception;// NOSONAR

  /**
   * Checks whether the wallet is initialized or not (on blockchain)
   * 
   * @param wallet
   * @return {@link TransactionDetail} with the hash of the transaction sent in
   *         blockchain
   * @throws Exception
   */
  boolean isInitializedAccount(Wallet wallet) throws Exception;// NOSONAR

  /**
   * Retrieves wallet details from blockchain
   * 
   * @param wallet object to refresh
   * @param contractDetail contract details attributes
   * @param walletModifications list of called method names to change wallet
   *          state on blockchain. This parameter will be used to know which
   *          methods to call to refresh wallet state in order to optimize the
   *          number of calls to Blockchain
   * @throws Exception
   */
  void retrieveWalletInformationFromBlockchain(Wallet wallet,
                                               ContractDetail contractDetail,
                                               Set<String> walletModifications) throws Exception; // NOSONAR

  /**
   * Boosts the transaction issued from Admin wallet by increasing the gas price
   */
  void boostAdminTransactions();
}
