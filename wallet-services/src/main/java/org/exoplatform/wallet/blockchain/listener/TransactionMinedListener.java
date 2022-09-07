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
package org.exoplatform.wallet.blockchain.listener;

import static org.exoplatform.wallet.utils.WalletUtils.ETHER_FUNC_SEND_FUNDS;
import static org.exoplatform.wallet.utils.WalletUtils.TRANSACTION_MODIFIED_EVENT;

import java.util.*;

import org.exoplatform.wallet.blockchain.service.EthereumClientConnector;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.wallet.contract.MeedsToken;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.listener.*;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.*;

@Asynchronous
public class TransactionMinedListener extends Listener<Object, Map<String, Object>> {

  private WalletAccountService     accountService;

  private WalletContractService    contractService;

  private WalletTransactionService walletTransactionService;

  private EthereumClientConnector web3jConnector;

  private ListenerService          listenerService;

  public TransactionMinedListener(EthereumClientConnector web3jConnector) {
    this.web3jConnector = web3jConnector;
  }

  @Override
  public void onEvent(Event<Object, Map<String, Object>> event) throws Exception {
    Map<String, Object> transactionDetailObject = event.getData();
    String hash = (String) transactionDetailObject.get("hash");
    TransactionDetail transactionDetail = getTransactionService().getTransactionByHash(hash);
    if (transactionDetail == null) {
      return;
    }

    Set<String> contractMethodsInvoked = new HashSet<>();
    Map<String, Set<String>> walletsModifications = new HashMap<>();
    String contractMethodName = transactionDetail.getContractMethodName();
    if (StringUtils.isNotBlank(contractMethodName)) {
      contractMethodsInvoked.add(contractMethodName);
    }

    if (transactionDetail.isSucceeded()) {
      addWalletsModification(transactionDetail, walletsModifications);
    }

    // Refresh saved contract detail in internal database from blockchain
    if (!contractMethodsInvoked.isEmpty()) {
      getContractService().refreshContractDetail(contractMethodsInvoked);
    }

    // Refresh modified wallets in blockchain
    if (!walletsModifications.isEmpty()) {
      getAccountService().refreshWalletsFromBlockchain(walletsModifications);
    }

    if (transactionDetail.isSucceeded() || web3jConnector.getTransaction(transactionDetail.getHash()) != null) {
      walletTransactionService.cancelTransactionsWithSameNonce(transactionDetail);
    }

    getListenerService().broadcast(TRANSACTION_MODIFIED_EVENT, null, transactionDetail);
  }

  private void addWalletsModification(TransactionDetail transactionDetail, Map<String, Set<String>> walletsModifications) {
    if (transactionDetail == null) {
      return;
    }

    addWalletModificationState(transactionDetail.getFromWallet(), ETHER_FUNC_SEND_FUNDS, walletsModifications);

    String contractMethodName = transactionDetail.getContractMethodName();
    if (StringUtils.isBlank(contractMethodName)) {
      addWalletModificationState(transactionDetail.getToWallet(), ETHER_FUNC_SEND_FUNDS, walletsModifications);
    } else if (StringUtils.equals(contractMethodName, MeedsToken.FUNC_TRANSFER)
        || StringUtils.equals(contractMethodName, MeedsToken.FUNC_TRANSFERFROM)) {
      addWalletModificationState(transactionDetail.getFromWallet(), contractMethodName, walletsModifications);
      addWalletModificationState(transactionDetail.getToWallet(), contractMethodName, walletsModifications);
      addWalletModificationState(transactionDetail.getByWallet(), contractMethodName, walletsModifications);
    } else if (StringUtils.equals(contractMethodName, MeedsToken.FUNC_APPROVE)) {
      addWalletModificationState(transactionDetail.getFromWallet(), contractMethodName, walletsModifications);
    } else if (StringUtils.equals(contractMethodName, MeedsToken.FUNC_TRANSFEROWNERSHIP)) {
      addWalletModificationState(transactionDetail.getFromWallet(), contractMethodName, walletsModifications);
      addWalletModificationState(transactionDetail.getToWallet(), contractMethodName, walletsModifications);
    }
  }

  private void addWalletModificationState(Wallet wallet,
                                          String contractMethodName,
                                          Map<String, Set<String>> walletsModifications) {
    if (wallet == null) {
      return;
    }
    String address = wallet.getAddress();
    walletsModifications.computeIfAbsent(address, k -> new HashSet<>()).add(contractMethodName);
  }

  private WalletTransactionService getTransactionService() {
    if (walletTransactionService == null) {
      walletTransactionService = CommonsUtils.getService(WalletTransactionService.class);
    }
    return walletTransactionService;
  }

  private WalletAccountService getAccountService() {
    if (accountService == null) {
      accountService = CommonsUtils.getService(WalletAccountService.class);
    }
    return accountService;
  }

  private WalletContractService getContractService() {
    if (contractService == null) {
      contractService = CommonsUtils.getService(WalletContractService.class);
    }
    return contractService;
  }

  private ListenerService getListenerService() {
    if (listenerService == null) {
      listenerService = CommonsUtils.getService(ListenerService.class);
    }
    return listenerService;
  }

}
