package org.exoplatform.wallet.entity;

import java.io.Serializable;

import javax.persistence.*;

import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity(name = "WalletBackupEntity")
@ExoEntity
@Table(name = "ADDONS_WALLET_ACCOUNT_BACKUP")
@NamedQuery(name = "WalletBackupEntity.findByWalletId", query = "SELECT wb FROM WalletBackupEntity wb WHERE wb.wallet.id = :walletId")
public class WalletBackupEntity implements Serializable {

  private static final long serialVersionUID = -8212394514591241477L;

  @Id
  @SequenceGenerator(name = "SEQ_WALLET_BACKUP_ID", sequenceName = "SEQ_WALLET_BACKUP_ID", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_WALLET_BACKUP_ID")
  @Column(name = "WALLET_BACKUP_ID")

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
