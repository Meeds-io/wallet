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
package io.meeds.wallet.model;

import static io.meeds.wallet.utils.WalletUtils.WALLET_RECEIVER_NOTIFICATION_ID;
import static io.meeds.wallet.utils.WalletUtils.WALLET_SENDER_NOTIFICATION_ID;

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
