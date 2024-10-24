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
package io.meeds.wallet.wallet.model.transaction;

import static io.meeds.wallet.wallet.utils.WalletUtils.convertFromDecimals;
import static io.meeds.wallet.wallet.utils.WalletUtils.convertToDecimals;

import java.io.Serializable;
import java.math.BigInteger;

import io.meeds.wallet.wallet.model.Wallet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode.Exclude;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDetail implements Serializable, Cloneable {

  private static final long serialVersionUID = 658273092293607458L;

  private long              id;

  @Exclude
  private Wallet            issuer;

  private long              issuerId;

  private long              networkId;

  private String            hash;

  private String            contractAddress;

  private String            contractMethodName;

  private boolean           pending;

  private boolean           succeeded;

  private boolean           dropped;

  private boolean           isAdminOperation;

  private String            from;

  @Exclude
  private Wallet            fromWallet;

  private String            to;

  @Exclude
  private Wallet            toWallet;

  private String            by;

  @Exclude
  private Wallet            byWallet;

  @ToString.Exclude
  private String            label;

  @ToString.Exclude
  private String            message;

  private double            value;

  private double            contractAmount;

  private int               gasUsed;

  private double            gasPrice;

  private double            tokenFee;

  private double            etherFee;

  private boolean           noContractFunds;

  private long              timestamp;

  private long              nonce;

  private boolean           boost;

  private String            rawTransaction;

  private long              sentTimestamp;

  private long              sendingAttemptCount;

  @Override
  public TransactionDetail clone() { // NOSONAR
    return new TransactionDetail(id,
                                 issuer,
                                 issuerId,
                                 networkId,
                                 hash,
                                 contractAddress,
                                 contractMethodName,
                                 pending,
                                 succeeded,
                                 dropped,
                                 isAdminOperation,
                                 from,
                                 fromWallet,
                                 to,
                                 toWallet,
                                 by,
                                 byWallet,
                                 label,
                                 message,
                                 value,
                                 contractAmount,
                                 gasUsed,
                                 gasPrice,
                                 tokenFee,
                                 etherFee,
                                 noContractFunds,
                                 timestamp,
                                 nonce,
                                 boost,
                                 rawTransaction,
                                 sentTimestamp,
                                 sendingAttemptCount);
  }

  public BigInteger getContractAmountDecimal(int decimals) {
    if (contractAmount == 0) {
      return BigInteger.ZERO;
    }
    return convertToDecimals(contractAmount, decimals);
  }

  public BigInteger getValueDecimal(int decimals) {
    if (value == 0) {
      return BigInteger.ZERO;
    }
    return convertToDecimals(value, decimals);
  }

  public void setContractAmountDecimal(BigInteger amount, int decimals) {
    if (amount == null) {
      this.contractAmount = 0;
      return;
    }
    this.contractAmount = convertFromDecimals(amount, decimals);
  }

  public void setValueDecimal(BigInteger amount, int decimals) {
    if (amount == null) {
      this.value = 0;
    } else {
      this.value = convertFromDecimals(amount, decimals);
    }
  }

  public void increaseSendingAttemptCount() {
    setSendingAttemptCount(sendingAttemptCount + 1);
  }

}
