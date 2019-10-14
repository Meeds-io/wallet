package org.exoplatform.wallet.entity;

import java.io.Serializable;

import javax.persistence.*;

import org.hibernate.annotations.DynamicUpdate;

import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity(name = "Label")
@ExoEntity
@DynamicUpdate
@Table(name = "ADDONS_WALLET_LABEL")
public class AddressLabelEntity implements Serializable {
  private static final long serialVersionUID = -1622032986992776281L;

  @Id
  @SequenceGenerator(name = "SEQ_WALLET_LABEL", sequenceName = "SEQ_WALLET_LABEL")
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_WALLET_LABEL")
  @Column(name = "LABEL_ID")
  private Long              id;

  @Column(name = "IDENTITY_ID", nullable = false)
  private long              identityId;

  @Column(name = "ADDRESS", nullable = false)
  private String            address;

  @Column(name = "LABEL", nullable = false)
  private String            label;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getIdentityId() {
    return identityId;
  }

  public void setIdentityId(Long identityId) {
    this.identityId = identityId;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

}
