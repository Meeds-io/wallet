package org.exoplatform.addon.wallet.model;

import java.io.Serializable;

import lombok.Data;
import lombok.EqualsAndHashCode.Exclude;

@Data
public class Wallet implements Serializable, Cloneable {

  private static final long serialVersionUID = 8011288624609310945L;

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
  private boolean           isSpaceAdministrator;

  @Exclude
  private boolean           isEnabled;

  @Exclude
  private String            initializationState;

  @Exclude
  private boolean           isDisabledUser;

  @Exclude
  private boolean           isDeletedUser;

  @Exclude
  private boolean           hasKeyOnServerSide;

  @Exclude
  private String            avatar;

  @Exclude
  private String            passPhrase;

  @Override
  public Wallet clone() { // NOSONAR
    try {
      return (Wallet) super.clone();
    } catch (CloneNotSupportedException e) {
      return null; // NOSONAR
    }
  }
}
