package org.exoplatform.addon.wallet.blockchain.listener;

import static org.exoplatform.addon.wallet.utils.WalletUtils.ETHER_FUNC_SEND_FUNDS;
import static org.exoplatform.addon.wallet.utils.WalletUtils.TRANSACTION_MODIFIED_EVENT;

import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import org.exoplatform.addon.wallet.contract.ERTTokenV2;
import org.exoplatform.addon.wallet.model.Wallet;
import org.exoplatform.addon.wallet.model.transaction.TransactionDetail;
import org.exoplatform.addon.wallet.service.*;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.listener.*;

@Asynchronous
public class TransactionMinedListener extends Listener<Object, JSONObject> {

  private WalletAccountService     accountService;

  private WalletContractService    contractService;

  private WalletTransactionService transactionService;

  private ListenerService          listenerService;

  @Override
  public void onEvent(Event<Object, JSONObject> event) throws Exception {
    JSONObject transactionDetailObject = event.getData();
    String hash = transactionDetailObject.getString("hash");
    TransactionDetail transactionDetails = getTransactionService().getTransactionByHash(hash);
    if (transactionDetails == null) {
      return;
    }

    Set<String> contractMethodsInvoked = new HashSet<>();
    Map<String, Set<String>> walletsModifications = new HashMap<>();
    String contractMethodName = transactionDetails.getContractMethodName();
    if (StringUtils.isNotBlank(contractMethodName)) {
      contractMethodsInvoked.add(contractMethodName);
    }

    if (transactionDetails.isSucceeded()) {
      addWalletsModification(transactionDetails, walletsModifications);
    }

    // Refresh saved contract detail in internal database from blockchain
    if (!contractMethodsInvoked.isEmpty()) {
      getContractService().refreshContractDetail(contractMethodsInvoked);
    }

    // Refresh modified wallets in blockchain
    if (!walletsModifications.isEmpty()) {
      getAccountService().refreshWalletsFromBlockchain(walletsModifications);
    }

    getListenerService().broadcast(TRANSACTION_MODIFIED_EVENT, null, transactionDetails);
  }

  private void addWalletsModification(TransactionDetail transactionDetail, Map<String, Set<String>> walletsModifications) {
    if (transactionDetail == null) {
      return;
    }

    addWalletModificationState(transactionDetail.getFromWallet(), ETHER_FUNC_SEND_FUNDS, walletsModifications);

    String contractMethodName = transactionDetail.getContractMethodName();
    if (StringUtils.isBlank(contractMethodName)) {
      addWalletModificationState(transactionDetail.getToWallet(), ETHER_FUNC_SEND_FUNDS, walletsModifications);
    } else if (StringUtils.equals(contractMethodName, ERTTokenV2.FUNC_TRANSFER)
        || StringUtils.equals(contractMethodName, ERTTokenV2.FUNC_TRANSFERFROM)) {
      addWalletModificationState(transactionDetail.getFromWallet(), contractMethodName, walletsModifications);
      addWalletModificationState(transactionDetail.getToWallet(), contractMethodName, walletsModifications);
      addWalletModificationState(transactionDetail.getByWallet(), contractMethodName, walletsModifications);
    } else if (StringUtils.equals(contractMethodName, ERTTokenV2.FUNC_APPROVE)) {
      addWalletModificationState(transactionDetail.getFromWallet(), contractMethodName, walletsModifications);
    } else if (StringUtils.equals(contractMethodName, ERTTokenV2.FUNC_TRANSFEROWNERSHIP)) {
      addWalletModificationState(transactionDetail.getFromWallet(), contractMethodName, walletsModifications);
      addWalletModificationState(transactionDetail.getToWallet(), contractMethodName, walletsModifications);
    } else if (StringUtils.equals(contractMethodName, ERTTokenV2.FUNC_INITIALIZEACCOUNT)
        || StringUtils.equals(contractMethodName, ERTTokenV2.FUNC_REWARD)) {
      addWalletModificationState(transactionDetail.getFromWallet(), ERTTokenV2.FUNC_TRANSFER, walletsModifications);
      addWalletModificationState(transactionDetail.getToWallet(), contractMethodName, walletsModifications);
    } else if (StringUtils.equals(contractMethodName, ERTTokenV2.FUNC_ADDADMIN)
        || StringUtils.equals(contractMethodName, ERTTokenV2.FUNC_REMOVEADMIN)
        || StringUtils.equals(contractMethodName, ERTTokenV2.FUNC_APPROVEACCOUNT)
        || StringUtils.equals(contractMethodName, ERTTokenV2.FUNC_DISAPPROVEACCOUNT)
        || StringUtils.equals(contractMethodName, ERTTokenV2.FUNC_TRANSFORMTOVESTED)) {
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
    if (!walletsModifications.containsKey(address)) {
      walletsModifications.put(address, new HashSet<>());
    }
    walletsModifications.get(address).add(contractMethodName);
  }

  private WalletTransactionService getTransactionService() {
    if (transactionService == null) {
      transactionService = CommonsUtils.getService(WalletTransactionService.class);
    }
    return transactionService;
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
