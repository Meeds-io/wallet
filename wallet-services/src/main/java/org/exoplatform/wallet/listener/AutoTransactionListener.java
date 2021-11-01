package org.exoplatform.wallet.listener;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.WalletInitializationState;
import org.exoplatform.wallet.model.settings.InitialFundsSettings;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.WalletAccountService;
import org.exoplatform.wallet.service.WalletService;
import org.exoplatform.wallet.service.WalletTokenAdminService;

@Asynchronous
public class AutoTransactionListener extends Listener<Object, String> {

  private static final Log        LOG                           = ExoLogger.getLogger(AutoTransactionListener.class);

  private WalletAccountService    walletAccountService;

  private WalletTokenAdminService walletTokenAdminService;

  private WalletService           walletService;

  private static final String     BAD_REQUEST_SENT_TO_SERVER_BY = "Bad request sent to server by '";

  private static final String     WALLET_TYPE                   = "user";

  @Override
  public void onEvent(Event<Object, String> event) throws Exception {
    Wallet wallet = (Wallet) event.getSource();
    InitialFundsSettings initialFundsSettings = getWalletService().getInitialFundsSettings();
    double tokenAmount = initialFundsSettings.getTokenAmount();
    double etherAmount = initialFundsSettings.getEtherAmount();
    String transactionLabel = initialFundsSettings.getRequestMessage();
    String transactionMessage = initialFundsSettings.getRequestMessage();
    if (tokenAmount == 0 || etherAmount == 0 || !WALLET_TYPE.equals(wallet.getType())) {
      return;
    }
    if (StringUtils.isBlank(wallet.getAddress())) {
      LOG.warn(BAD_REQUEST_SENT_TO_SERVER_BY + wallet.getId() + "' with empty address");
      return;
    }
    try {
      TransactionDetail transactionDetail = new TransactionDetail();
      transactionDetail.setTo(wallet.getAddress());
      transactionDetail.setContractAmount(tokenAmount);
      transactionDetail.setLabel(transactionLabel);
      transactionDetail.setMessage(transactionMessage);
      transactionDetail = getWalletTokenAdminService().sendToken(transactionDetail, wallet.getId());
      LOG.info("wallet {} is initialized with Tokens, the transaction hash is {}", wallet.getId(), transactionDetail.getHash());

      // Send Ether
      TransactionDetail etherTransactionDetail = new TransactionDetail();
      etherTransactionDetail.setTo(wallet.getAddress());
      etherTransactionDetail.setValue(etherAmount);
      etherTransactionDetail.setLabel(transactionLabel);
      etherTransactionDetail.setMessage(transactionMessage);
      etherTransactionDetail = getWalletTokenAdminService().sendEther(etherTransactionDetail, wallet.getId());
      LOG.info("wallet {} is initialized with Ethers, the transaction hash is {}", wallet.getId(), etherTransactionDetail.getHash());

      // Set Wallet status to : Initialized
      getWalletAccountService().setInitializationStatus(wallet.getAddress(), WalletInitializationState.INITIALIZED);
    } catch (Exception e) {
      LOG.error("Error initializing wallet {}", wallet.getId(), e);
    }

  }

  private WalletAccountService getWalletAccountService() {
    if (walletAccountService == null) {
      walletAccountService = CommonsUtils.getService(WalletAccountService.class);
    }
    return walletAccountService;
  }

  private WalletTokenAdminService getWalletTokenAdminService() {
    if (walletTokenAdminService == null) {
      walletTokenAdminService = CommonsUtils.getService(WalletTokenAdminService.class);
    }
    return walletTokenAdminService;
  }

  private WalletService getWalletService() {
    if (walletService == null) {
      walletService = CommonsUtils.getService(WalletService.class);
    }
    return walletService;
  }

}
