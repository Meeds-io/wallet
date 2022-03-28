package org.exoplatform.wallet.entity;

import org.exoplatform.commons.api.persistence.ExoEntity;
import org.exoplatform.wallet.model.WalletProvider;
import org.exoplatform.wallet.model.WalletState;
import org.exoplatform.wallet.model.WalletType;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.util.Collection;

@Entity(name = "WalletBackUpEntity")
@ExoEntity
@DynamicUpdate
@Table(name = "ADDONS_WALLET_ACCOUNT_BACKUP")
public class WalletBackUpEntity {

  @Id
  @Column(name = "IDENTITY_ID")
  private Long                                    id;

  @Column(name = "IDENTITY_TYPE", nullable = false)
  private WalletType                              type;

  @Column(name = "ADDRESS", unique = true, nullable = false)
  private String                                  address;

  @Column(name = "PHRASE", nullable = false)
  private String                                  passPhrase;

  @Column(name = "ENABLED", nullable = false)
  private boolean                                 isEnabled;

  @Column(name = "BACKED_UP", nullable = false)
  private boolean                                 isBackedUp;

  @Column(name = "INITIALIZATION_STATE")
  private WalletState                             initializationState;

  @Column(name = "ACTIVE", nullable = false)
  private boolean                                 isActive;

  @Enumerated(EnumType.ORDINAL)
  @Column(name = "PROVIDER", nullable = false)
  protected WalletProvider                        walletProvider;

  @OneToOne(fetch = FetchType.EAGER, mappedBy = "wallet", cascade = CascadeType.REMOVE)
  private WalletPrivateKeyEntity                  privateKey;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "wallet", cascade = CascadeType.REMOVE)
  private Collection<WalletBlockchainStateEntity> blockchainState;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public WalletType getType() {
    return type;
  }

  public void setType(WalletType type) {
    this.type = type;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getPassPhrase() {
    return passPhrase;
  }

  public void setPassPhrase(String passPhrase) {
    this.passPhrase = passPhrase;
  }

  public boolean isEnabled() {
    return isEnabled;
  }

  public void setEnabled(boolean enabled) {
    isEnabled = enabled;
  }

  public boolean isBackedUp() {
    return isBackedUp;
  }

  public void setBackedUp(boolean backedUp) {
    isBackedUp = backedUp;
  }

  public WalletState getInitializationState() {
    return initializationState;
  }

  public void setInitializationState(WalletState initializationState) {
    this.initializationState = initializationState;
  }

  public boolean isActive() {
    return isActive;
  }

  public void setActive(boolean active) {
    isActive = active;
  }

  public WalletProvider getWalletProvider() {
    return walletProvider;
  }

  public void setWalletProvider(WalletProvider walletProvider) {
    this.walletProvider = walletProvider;
  }

  public WalletPrivateKeyEntity getPrivateKey() {
    return privateKey;
  }

  public void setPrivateKey(WalletPrivateKeyEntity privateKey) {
    this.privateKey = privateKey;
  }

  public Collection<WalletBlockchainStateEntity> getBlockchainState() {
    return blockchainState;
  }

  public void setBlockchainState(Collection<WalletBlockchainStateEntity> blockchainState) {
    this.blockchainState = blockchainState;
  }
}
