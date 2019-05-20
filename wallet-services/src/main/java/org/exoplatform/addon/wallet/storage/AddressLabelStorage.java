package org.exoplatform.addon.wallet.storage;

import java.util.*;
import java.util.stream.Collectors;

import org.exoplatform.addon.wallet.dao.AddressLabelDAO;
import org.exoplatform.addon.wallet.entity.AddressLabelEntity;
import org.exoplatform.addon.wallet.model.AddressLabel;

public class AddressLabelStorage {

  private AddressLabelDAO   addressLabelDAO;

  private Set<AddressLabel> allLabels;

  public AddressLabelStorage(AddressLabelDAO addressLabelDAO) {
    this.addressLabelDAO = addressLabelDAO;
  }

  public Set<AddressLabel> getAllLabels() {
    if (this.allLabels != null) {
      return this.allLabels;
    }
    List<AddressLabelEntity> allLabelsEntities = addressLabelDAO.findAll();
    if (allLabelsEntities == null || allLabelsEntities.isEmpty()) {
      return Collections.emptySet();
    }
    this.allLabels = allLabelsEntities.stream()
                                      .map(this::fromEntity)
                                      .collect(Collectors.toSet());
    return this.allLabels;
  }

  public AddressLabel saveLabel(AddressLabel label) {
    if (label == null) {
      throw new IllegalArgumentException("label is mandatory");
    }
    AddressLabelEntity entity = toEntity(label);
    if (entity.getId() == null) {
      entity = addressLabelDAO.create(entity);
    } else {
      entity = addressLabelDAO.update(entity);
    }

    this.allLabels = null;
    return fromEntity(entity);
  }

  public AddressLabel getLabel(long id) {
    return fromEntity(addressLabelDAO.find(id));
  }

  public void removeLabel(AddressLabel label) {
    if (label.getId() > 0) {
      addressLabelDAO.delete(toEntity(label));
      this.allLabels = null;
    }
  }

  private AddressLabelEntity toEntity(AddressLabel label) {
    if (label == null) {
      return null;
    }
    AddressLabelEntity entity = new AddressLabelEntity();
    entity.setId(label.getId() == 0 ? null : label.getId());
    entity.setIdentityId(label.getIdentityId());
    entity.setAddress(label.getAddress());
    entity.setLabel(label.getLabel());
    return entity;
  }

  private AddressLabel fromEntity(AddressLabelEntity entity) {
    if (entity == null) {
      return null;
    }
    return new AddressLabel(entity.getId(),
                            entity.getIdentityId(),
                            entity.getLabel(),
                            entity.getAddress());
  }

}
