package org.exoplatform.wallet.model;

import java.io.Serializable;

import lombok.Data;
import lombok.EqualsAndHashCode.Exclude;
import lombok.ToString;

@Data
@ToString
public class Wallet implements Serializable, Cloneable {

  private static final long serialVersionUID     = 8011288624609310945L;

  // wallet user/space Remote ID
  private String            id;

  // A string is used instead of enum, because of cache clustering
  // problems with enums
  private String            type;

  // Social Identity Id === Wallet technical id
  private long              technicalId;

  @Exclude
  private long              spaceId;

  @Exclude
  private String            name;

  @Exclude
  private String            address;

  @Exclude
  private boolean           isEnabled;

  @Exclude
  private String            initializationState;

  @Exclude
  private boolean           isDisabledUser;

  @Exclude
  private boolean           isDeletedUser;

  @Exclude
  private String            avatar;

  @Exclude
  @ToString.Exclude
  private String            passPhrase;

  @Exclude
  @ToString.Exclude
  private boolean           isSpaceAdministrator = false;

  @Exclude
  @ToString.Exclude
  private boolean           hasPrivateKey        = false;

  @Exclude
  @ToString.Exclude
  private boolean           backedUp             = false;

  /* Wallet state on Blockchain */

  @Exclude
  @ToString.Exclude
  private Double            etherBalance         = null;

  /* Wallet state on Contract on Blockchain */

  @Exclude
  @ToString.Exclude
  private Double            tokenBalance         = null;

  @Exclude
  @ToString.Exclude
  private Double            rewardBalance        = null;

  @Exclude
  @ToString.Exclude
  private Double            vestingBalance       = null;

  @Exclude
  @ToString.Exclude
  private Integer           adminLevel           = null;

  @Exclude
  @ToString.Exclude
  private Boolean           isApproved           = null;

  @Exclude
  @ToString.Exclude
  private Boolean           isInitialized        = null;

  @Override
  public Wallet clone() { // NOSONAR
    try {
      return (Wallet) super.clone();
    } catch (CloneNotSupportedException e) {
      return null; // NOSONAR
    }
  }
}
