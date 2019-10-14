package org.exoplatform.wallet.entity;

import java.io.Serializable;

import javax.persistence.*;

import org.hibernate.annotations.DynamicUpdate;

import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity(name = "WalletKey")
@ExoEntity
@DynamicUpdate
@Table(name = "ADDONS_WALLET_KEY")
@NamedQueries({
    @NamedQuery(name = "WalletKey.findByWalletId", query = "SELECT wk FROM WalletKey wk WHERE wk.wallet.id = :walletId"),
})
public class WalletPrivateKeyEntity implements Serializable {
  private static final long serialVersionUID = -7294965683405044055L;

  @Id
  @SequenceGenerator(name = "SEQ_WALLET_KEY", sequenceName = "SEQ_WALLET_KEY")
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_WALLET_KEY")
  @Column(name = "KEY_ID")
  private Long              id;

  @Column(name = "CONTENT", nullable = false)
  private String            keyContent;

  @OneToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE })
  @JoinColumn(name = "WALLET_ID")
  private WalletEntity      wallet;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getKeyContent() {
    return keyContent;
  }

  public void setKeyContent(String keyContent) {
    this.keyContent = keyContent;
  }

  public WalletEntity getWallet() {
    return wallet;
  }

  public void setWallet(WalletEntity wallet) {
    this.wallet = wallet;
  }

}
