package org.exoplatform.addon.wallet.model;

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
  @Exclude
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

  @Override
  public Wallet clone() { // NOSONAR
    try {
      return (Wallet) super.clone();
    } catch (CloneNotSupportedException e) {
      return null; // NOSONAR
    }
  }
}
