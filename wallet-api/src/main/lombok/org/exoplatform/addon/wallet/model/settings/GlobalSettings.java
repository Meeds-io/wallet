package org.exoplatform.addon.wallet.model.settings;

import java.io.Serializable;

import org.apache.commons.beanutils.BeanUtils;
import org.json.JSONArray;

import org.exoplatform.addon.wallet.model.ContractDetail;

import lombok.*;
import lombok.EqualsAndHashCode.Exclude;

@Data
@ToString
@NoArgsConstructor
public class GlobalSettings implements Serializable, Cloneable {

  private static final long    serialVersionUID = 8987967110410722896L;

  private InitialFundsSettings initialFunds     = new InitialFundsSettings();

  private NetworkSettings      network          = new NetworkSettings();

  private ContractDetail       contractDetail   = new ContractDetail();

  private String               contractAddress  = null;

  private String               accessPermission = null;

  @Exclude
  @ToString.Exclude
  private JSONArray            contractAbi      = null;                      // NOSONAR

  @Exclude
  @ToString.Exclude
  private String               contractBin      = null;

  public GlobalSettings(GlobalSettings globalSettings) {
    if (globalSettings != null) {
      try {
        BeanUtils.copyProperties(this, globalSettings);
      } catch (Exception e) {
        throw new IllegalStateException("Error while cloning attributes of global settings to current instance", e);
      }
    }
  }

  public GlobalSettings clone() { // NOSONAR
    try {
      return (GlobalSettings) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new IllegalStateException("Error while cloning object: " + this, e);
    }
  }

}
