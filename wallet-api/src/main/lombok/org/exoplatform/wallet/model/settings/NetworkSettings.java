package org.exoplatform.wallet.model.settings;

import java.io.Serializable;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class NetworkSettings implements Serializable, Cloneable {

  private static final long serialVersionUID     = 2237799362889884389L;

  private long              id                   = 0L;

  private String            providerURL          = null;

  private String            websocketProviderURL = null;

  private Long              gasLimit             = 0L;

  private Long              minGasPrice          = 0L;

  private Long              normalGasPrice       = 0L;

  private Long              maxGasPrice          = 0L;

  public NetworkSettings clone() { // NOSONAR
    try {
      return (NetworkSettings) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new IllegalStateException("Error while cloning object: " + this, e);
    }
  }
}
