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
import static org.exoplatform.wallet.utils.WalletUtils.TRANSACTION_MINED_AND_UPDATED_EVENT;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.wallet.contract.MeedsToken;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.WalletAccountService;
import org.exoplatform.wallet.service.WalletTransactionService;

/**
 * A listener that is triggered once a transaction is mined. It will update the
 * balances of user {@link Wallet}
 */
@Asynchronous
public class TransactionMinedListener extends Listener<Object, Map<String, Object>> {

  private WalletAccountService     accountService;

  private WalletTransactionService walletTransactionService;

  private ListenerService          listenerService;

  public TransactionMinedListener(WalletAccountService accountService,
                                  WalletTransactionService walletTransactionService,
                                  ListenerService listenerService) {
    this.accountService = accountService;
    this.walletTransactionService = walletTransactionService;
    this.listenerService = listenerService;
  }

  @Override
  public void onEvent(Event<Object, Map<String, Object>> event) throws Exception {
    Map<String, Object> transactionDetailObject = event.getData();
    String hash = (String) transactionDetailObject.get("hash");
    TransactionDetail transactionDetail = walletTransactionService.getTransactionByHash(hash);
    if (transactionDetail == null) {
      return;
    }

    Map<String, Set<String>> walletsModifications = new HashMap<>();
    addWalletsModification(transactionDetail, walletsModifications);
    if (!walletsModifications.isEmpty()) {
      // Refresh modified wallets in blockchain
      accountService.refreshWalletsFromBlockchain(walletsModifications);
    }

    listenerService.broadcast(TRANSACTION_MINED_AND_UPDATED_EVENT, null, transactionDetail);
  }

  private void addWalletsModification(TransactionDetail transactionDetail, Map<String, Set<String>> walletsModifications) {
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
    }
  }

  private void addWalletModificationState(Wallet wallet,
                                          String contractMethodName,
                                          Map<String, Set<String>> walletsModifications) {
    if (wallet == null) {
      return;
    }
    walletsModifications.computeIfAbsent(wallet.getAddress(), k -> new HashSet<>()).add(contractMethodName);
  }

}
