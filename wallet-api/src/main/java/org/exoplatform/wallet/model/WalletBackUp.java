package org.exoplatform.wallet.model;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@ToString
public class WalletBackUp implements Serializable, Cloneable {

  long   id;

  long   walletId;

  String address;

  @Override
  public WalletBackUp clone() { // NOSONAR
    try {
      return (WalletBackUp) super.clone();
    } catch (CloneNotSupportedException e) {
      return null; // NOSONAR
    }
  }
}
