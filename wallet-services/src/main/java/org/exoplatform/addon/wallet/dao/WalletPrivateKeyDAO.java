package org.exoplatform.addon.wallet.dao;

import java.util.List;

import org.exoplatform.addon.wallet.entity.WalletPrivateKeyEntity;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;

public class WalletPrivateKeyDAO extends GenericDAOJPAImpl<WalletPrivateKeyEntity, Long> {

  @Override
  public void deleteAll() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteAll(List<WalletPrivateKeyEntity> entities) {
    throw new UnsupportedOperationException();
  }

}
