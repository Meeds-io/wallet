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

import java.io.IOException;

import org.exoplatform.wallet.model.ContractTransactionEvent;
import org.exoplatform.wallet.model.transaction.TransactionDetail;

public interface BlockchainTransactionService {

  /**
   * Sends raw transactions to blockchain
   */
  void sendPendingTransactionsToBlockchain();

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
   * Refreshes gas price from blockchain and cache it
   * 
   * @return last gas price retrieved from blockchain
   * @throws IOException when an error occurs while requesting gas price from
   *           blockchain
   */
  long refreshBlockchainGasPrice() throws IOException;

  /**
   * Checks whether the Contract Transaction Topics contains a managed wallet
   * 
   * @param contractEvent Contract Event Log
   * @return true if a wallet is found in topics list
   */
  boolean hasManagedWalletInTransaction(ContractTransactionEvent contractEvent);

  /**
   * Renew subscription to listen to new transactions from Blockchain
   */
  public void startWatchingBlockchain();

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
