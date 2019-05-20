package org.exoplatform.addon.wallet.dao;

import java.util.List;

import javax.persistence.TypedQuery;

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

  public WalletPrivateKeyEntity findByWalletId(long id) {
    TypedQuery<WalletPrivateKeyEntity> query = getEntityManager().createNamedQuery("WalletKey.findByWalletId",
                                                                                   WalletPrivateKeyEntity.class);
    query.setParameter("walletId", id);
    return query.getSingleResult();
  }

}
