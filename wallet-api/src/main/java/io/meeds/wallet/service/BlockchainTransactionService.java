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
package io.meeds.wallet.service;

import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Future;

import io.meeds.wallet.model.ContractTransactionEvent;
import io.meeds.wallet.model.TransactionDetail;

public interface BlockchainTransactionService {

  /**
   * Sends raw transactions to blockchain
   * 
   * @return {@link List} of {@link TransactionDetail} asynchronously after sent
   *         to blockchain
   */
  List<TransactionDetail> sendPendingTransactionsToBlockchain();

  /**
   * This will refresh transaction from blockchain. If the transaction detail
   * exists in database, it will update its status, else it will create a new
   * one with data found on Blockchain mined Transaction
   * 
   * @param transactionHash hash of transaction to update
   * @return refreshed {@link TransactionDetail} from blockchain
   */
  TransactionDetail refreshTransactionFromBlockchain(String transactionHash);

  /**
   * Get gas price from blockchain
   * 
   * @return last gas price retrieved from blockchain
   * @throws IOException when an error occurs while requesting gas price from
   *           blockchain
   */
  double getGasPrice() throws IOException;

  /**
   * Add {@link TransactionDetail} to refresh processing {@link Queue}
   * @param transactionDetail {@link TransactionDetail} to refresh from blockchain
   */
  void addTransactionToRefreshFromBlockchain(TransactionDetail transactionDetail);

  /**
   * Checks whether the Contract Transaction Topics contains a managed wallet
   * 
   * @param contractEvent Contract Event Log
   * @return true if a wallet is found in topics list
   */
  boolean hasManagedWalletInTransaction(ContractTransactionEvent contractEvent);

  /**
   * Renew subscription to listen to new transactions from Blockchain
   * @return {@link Future} of async watching operation
   */
  @SuppressWarnings("rawtypes")
  public Future startWatchingBlockchain();

  /**
   * Stop subscription to listen on Blockchain
   */
  void stopWatchingBlockchain();

  /**
   * @return last watched block number
   */
  long getLastWatchedBlockNumber();

  /**
   * Saves last watched block number
   * 
   * @param lastWatchedBlockNumber
   */
  void saveLastWatchedBlockNumber(long lastWatchedBlockNumber);

}
