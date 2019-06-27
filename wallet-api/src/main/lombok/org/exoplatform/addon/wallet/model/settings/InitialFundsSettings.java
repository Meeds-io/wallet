package org.exoplatform.addon.wallet.model.settings;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class InitialFundsSettings implements Serializable, Cloneable {

  private static final long   serialVersionUID = -1602089025876785771L;

  private Map<String, Double> funds            = new HashMap<>();

  private String              fundsHolder      = null;

  private String              fundsHolderType  = null;

  private String              requestMessage   = null;

  public InitialFundsSettings clone() { // NOSONAR
    try {
      return (InitialFundsSettings) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new IllegalStateException("Error while cloning object: " + this, e);
    }
  }
}
