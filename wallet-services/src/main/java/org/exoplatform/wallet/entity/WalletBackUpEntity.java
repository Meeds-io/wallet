package org.exoplatform.wallet.entity;

import org.exoplatform.commons.api.persistence.ExoEntity;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.io.Serializable;

@Entity(name = "WalletBackUpEntity")
@ExoEntity
@DynamicUpdate
@Table(name = "ADDONS_WALLET_ACCOUNT_BACKUP")
@NamedQueries({
    @NamedQuery(name = "WalletBackUpEntity.findByWalletId", query = "SELECT wb FROM WalletBackUpEntity wb WHERE wb.wallet.id = :walletId"), })
public class WalletBackUpEntity implements Serializable {

  @Id
  @SequenceGenerator(name = "SEQ_WALLET_BACKUP_ID", sequenceName = "SEQ_WALLET_BACKUP_ID", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_WALLET_BACKUP_ID")
  @Column(name = "ID")

  private Long         id;

  @Column(name = "ADDRESS", unique = true, nullable = false)
  private String       address;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "WALLET_ID")
  private WalletEntity wallet;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public WalletEntity getWallet() {
    return wallet;
  }

  public void setWallet(WalletEntity wallet) {
    this.wallet = wallet;
  }
}
