/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
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
  @SequenceGenerator(name = "SEQ_WALLET_LABEL", sequenceName = "SEQ_WALLET_LABEL", allocationSize = 1)
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
