package org.exoplatform.wallet.entity;

import org.exoplatform.wallet.model.WalletProvider;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class WalletEntityKey implements Serializable {

  @Column(name = "IDENTITY_ID")
  private Long identityId;

  @Column(name = "PROVIDER")
  private WalletProvider provider;

  public WalletEntityKey() {
  }

  public WalletEntityKey(Long identityId, WalletProvider provider) {
    this.identityId = identityId;
    this.provider = provider;
  }

  public Long getIdentityId() {
    return identityId;
  }

  public void setIdentityId(Long identityId) {
    this.identityId = identityId;
  }

  public WalletProvider getProvider() {
    return provider;
  }

  public void setProvider(WalletProvider provider) {
    this.provider = provider;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    WalletEntityKey that = (WalletEntityKey) o;
    return identityId.equals(that.identityId) && provider == that.provider;
  }

  @Override
  public int hashCode() {
    return Objects.hash(identityId, provider);
  }

  @Override
  public String toString() {
    return "WalletEntityKey{" +
            "identityId=" + identityId +
            ", provider=" + provider +
            '}';
  }
}
