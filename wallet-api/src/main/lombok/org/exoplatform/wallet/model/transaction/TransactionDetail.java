package org.exoplatform.wallet.model.transaction;

import static org.exoplatform.wallet.utils.WalletUtils.convertFromDecimals;
import static org.exoplatform.wallet.utils.WalletUtils.convertToDecimals;

import java.io.Serializable;
import java.math.BigInteger;

import org.exoplatform.wallet.model.Wallet;

import lombok.Data;
import lombok.EqualsAndHashCode.Exclude;
import lombok.ToString;

@Data
@ToString
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

  private String            label;

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

  private String            rawTransaction;

  private long              sentTimestamp;

  private long              sendingAttemptCount;

  @Override
  public TransactionDetail clone() { // NOSONAR
    try {
      return (TransactionDetail) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new IllegalStateException("Error while cloning object", e);
    }
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

}
