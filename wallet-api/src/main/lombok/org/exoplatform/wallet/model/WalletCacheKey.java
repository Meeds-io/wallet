package org.exoplatform.wallet.model;

import java.io.Serializable;

import lombok.Data;

@Data
public class WalletCacheKey implements Serializable {

  private static final long serialVersionUID = -4914971441935903383L;

  public WalletCacheKey(String address) {
    this.address = address;
  }

  public WalletCacheKey(long identityId) {
    this.identityId = identityId;
  }

  private String address;

  private long   identityId;

}
