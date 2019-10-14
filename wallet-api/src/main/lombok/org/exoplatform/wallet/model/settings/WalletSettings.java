package org.exoplatform.wallet.model.settings;

import java.io.Serializable;
import java.util.Set;

import org.exoplatform.wallet.model.WalletAddressLabel;

import lombok.*;
import lombok.EqualsAndHashCode.Exclude;

@Data
@NoArgsConstructor
@ToString
public class WalletSettings implements Serializable, Cloneable {

  private static final long       serialVersionUID = -5725443183560646198L;

  @Exclude
  @ToString.Exclude
  private Integer                 dataVersion      = 0;

  private String                  walletAddress    = null;

  @Exclude
  @ToString.Exclude
  private String                  phrase           = null;

  @Exclude
  @ToString.Exclude
  private boolean                 hasKeyOnServerSide;

  @Exclude
  @ToString.Exclude
  private Set<WalletAddressLabel> addresesLabels;

  public WalletSettings clone() { // NOSONAR
    try {
      return (WalletSettings) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new IllegalStateException("Error while cloning object: " + this, e);
    }
  }
}
