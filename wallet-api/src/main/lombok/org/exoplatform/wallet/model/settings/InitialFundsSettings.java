package org.exoplatform.wallet.model.settings;

import java.io.Serializable;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class InitialFundsSettings implements Serializable, Cloneable {

  private static final long serialVersionUID = -1602089025876785771L;

  private String            fundsHolder      = null;

  private String            fundsHolderType  = null;

  private String            requestMessage   = null;

  private double            etherAmount      = 0;

  private double            tokenAmount      = 0;

  public InitialFundsSettings clone() { // NOSONAR
    try {
      return (InitialFundsSettings) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new IllegalStateException("Error while cloning object: " + this, e);
    }
  }
}
