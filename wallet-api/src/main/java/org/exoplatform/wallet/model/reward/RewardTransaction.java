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
package org.exoplatform.wallet.model.reward;

import static org.exoplatform.wallet.utils.WalletUtils.*;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.wallet.model.WalletType;

import lombok.*;
import lombok.EqualsAndHashCode.Exclude;

@Getter
@Setter
@ToString
public class RewardTransaction implements Serializable {

  private static final long serialVersionUID = 658273092293607458L;

  private String            hash;

  @Exclude
  private String            periodType;

  @Exclude
  private long              startDateInSeconds;

  @Exclude
  private String            receiverType;

  @Exclude
  private String            receiverId;

  @Exclude
  private long              receiverIdentityId;

  @Exclude
  private double            tokensSent;

  @Exclude
  private String            status;

  public static RewardTransaction fromStoredValue(String storedTransactionDetails) {
    RewardTransaction transaction = new RewardTransaction();
    if (StringUtils.isNotBlank(storedTransactionDetails)) {
      String[] transactionDetailsArray = storedTransactionDetails.split(";");
      transaction.setHash(transactionDetailsArray[0]);
      transaction.setReceiverType(transactionDetailsArray.length > 1 ? decodeString(transactionDetailsArray[1]) : null);
      transaction.setReceiverId(transactionDetailsArray.length > 2 ? decodeString(transactionDetailsArray[2]) : null);

      String tokensSentString = transactionDetailsArray.length > 3 ? transactionDetailsArray[3] : null;
      transaction.setTokensSent(StringUtils.isBlank(tokensSentString) ? 0 : Double.parseDouble(tokensSentString));

      String receiverIdentityId = transactionDetailsArray.length > 4 ? transactionDetailsArray[4] : null;
      transaction.setReceiverIdentityId(StringUtils.isBlank(receiverIdentityId)
          || StringUtils.equals("null", receiverIdentityId) ? 0 : Long.parseLong(receiverIdentityId));
    }
    return transaction;
  }

  /**
   * Determine the value to store on transactions list.
   * 
   * @return String to store representing the reward transaction
   */
  public String getToStoreValue() {
    Identity receiverIdentity = null;
    if (receiverIdentityId > 0) {
      receiverIdentity = getIdentityById(receiverIdentityId);
    } else {
      if (StringUtils.isBlank(receiverType)) {
        throw new IllegalStateException("receiverType is mandatory");
      }
      if (StringUtils.isBlank(receiverId)) {
        throw new IllegalStateException("receiverId is mandatory");
      }
      receiverIdentity = getIdentityByTypeAndId(WalletType.getType(receiverType), receiverId);
    }
    // ;;; kept for retro compatibility
    return hash + ";;;" + tokensSent + ";" + receiverIdentity.getId();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof RewardTransaction)) {
      return false;
    }
    return StringUtils.equalsIgnoreCase(hash, ((RewardTransaction) obj).getHash());
  }

  @Override
  public int hashCode() {
    return hash == null ? 0 : hash.hashCode();
  }

}
