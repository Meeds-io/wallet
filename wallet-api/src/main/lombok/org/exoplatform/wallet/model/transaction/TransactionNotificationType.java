package org.exoplatform.wallet.model.transaction;

import static org.exoplatform.wallet.utils.WalletUtils.WALLET_RECEIVER_NOTIFICATION_ID;
import static org.exoplatform.wallet.utils.WalletUtils.WALLET_SENDER_NOTIFICATION_ID;

public enum TransactionNotificationType {
  RECEIVER(WALLET_RECEIVER_NOTIFICATION_ID),
  SENDER(WALLET_SENDER_NOTIFICATION_ID);

  private String notificationId;

  private TransactionNotificationType(String notificationId) {
    this.notificationId = notificationId;
  }

  public String getNotificationId() {
    return notificationId;
  }
}
