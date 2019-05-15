package org.exoplatform.addon.wallet.reward.model;

import static org.exoplatform.addon.wallet.utils.WalletUtils.decodeString;
import static org.exoplatform.addon.wallet.utils.WalletUtils.encodeString;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;

import lombok.*;
import lombok.EqualsAndHashCode.Exclude;

@Getter
@Setter
@ToString
public class RewardTransaction implements Serializable {

  private static final long serialVersionUID = 658273092293607458L;

  private Long              networkId;

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
      transaction.setReceiverIdentityId(StringUtils.isBlank(receiverIdentityId) ? 0 : Long.parseLong(receiverIdentityId));
    }
    return transaction;
  }

  /**
   * Determine the value to store on transactions list.
   * 
   * @return
   */
  public String getToStoreValue() {
    if (StringUtils.isBlank(receiverType)) {
      throw new IllegalStateException("receiverType is mandatory");
    }
    if (StringUtils.isBlank(receiverId)) {
      throw new IllegalStateException("receiverId is mandatory");
    }
    return hash + ";" + encodeString(receiverType) + ";" + encodeString(receiverId) + ";" + tokensSent + ";" + receiverIdentityId;
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
