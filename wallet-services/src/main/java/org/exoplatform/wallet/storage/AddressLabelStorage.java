package org.exoplatform.wallet.storage;

import java.util.*;
import java.util.stream.Collectors;

import org.exoplatform.wallet.dao.AddressLabelDAO;
import org.exoplatform.wallet.entity.AddressLabelEntity;
import org.exoplatform.wallet.model.WalletAddressLabel;

public class AddressLabelStorage {

  private AddressLabelDAO         addressLabelDAO;

  private Set<WalletAddressLabel> allLabels;

  public AddressLabelStorage(AddressLabelDAO addressLabelDAO) {
    this.addressLabelDAO = addressLabelDAO;
  }

  public Set<WalletAddressLabel> getAllLabels() {
    if (this.allLabels != null) {
      return this.allLabels;
    }
    List<AddressLabelEntity> allLabelsEntities = addressLabelDAO.findAll();
    if (allLabelsEntities == null || allLabelsEntities.isEmpty()) {
      return Collections.emptySet();
    }
    this.allLabels = Collections.unmodifiableSet(allLabelsEntities.stream()
                                                                  .map(this::fromEntity)
                                                                  .collect(Collectors.toSet()));
    return this.allLabels;
  }

  public WalletAddressLabel saveLabel(WalletAddressLabel label) {
    if (label == null) {
      throw new IllegalArgumentException("label is mandatory");
    }
    AddressLabelEntity entity = toEntity(label);
    if (entity.getId() == null) {
      entity = addressLabelDAO.create(entity);
    } else {
      entity = addressLabelDAO.update(entity);
    }

    clearCache();
    return fromEntity(entity);
  }

  public WalletAddressLabel getLabel(long id) {
    return fromEntity(addressLabelDAO.find(id));
  }

  public void removeLabel(WalletAddressLabel label) {
    if (label.getId() > 0) {
      addressLabelDAO.delete(toEntity(label));
    }
    clearCache();
  }

  public void clearCache() {
    this.allLabels = null;
  }

  private AddressLabelEntity toEntity(WalletAddressLabel label) {
    AddressLabelEntity entity = new AddressLabelEntity();
    entity.setId(label.getId() == 0 ? null : label.getId());
    entity.setIdentityId(label.getIdentityId());
    entity.setAddress(label.getAddress());
    entity.setLabel(label.getLabel());
    return entity;
  }

  private WalletAddressLabel fromEntity(AddressLabelEntity entity) {
    if (entity == null) {
      return null;
    }
    return new WalletAddressLabel(entity.getId(),
                                  entity.getIdentityId(),
                                  entity.getLabel(),
                                  entity.getAddress());
  }

}
