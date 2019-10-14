package org.exoplatform.wallet.model;

import java.io.Serializable;

import lombok.*;
import lombok.EqualsAndHashCode.Exclude;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ContractDetail implements Serializable, Cloneable {

  private static final long serialVersionUID = 1459881604949041768L;

  private String            address;

  @Exclude
  private String            name;

  @Exclude
  private String            symbol;

  @Exclude
  private Integer           decimals;

  @Exclude
  private Long              networkId;

  @Exclude
  private String            owner;

  @Exclude
  private String            sellPrice;

  @Exclude
  private String            totalSupply;

  @Exclude
  private String            contractType;

  @Exclude
  private Boolean           isPaused;

  @Exclude
  private Double            etherBalance;

  public ContractDetail clone() { // NOSONAR
    try {
      return (ContractDetail) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new IllegalStateException("Error while cloning object: " + this, e);
    }
  }
}
