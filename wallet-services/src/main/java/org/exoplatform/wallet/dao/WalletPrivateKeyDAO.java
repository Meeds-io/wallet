package org.exoplatform.wallet.dao;

import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.wallet.entity.WalletPrivateKeyEntity;

public class WalletPrivateKeyDAO extends GenericDAOJPAImpl<WalletPrivateKeyEntity, Long> {

  public WalletPrivateKeyEntity findByWalletId(long walletId) {
    TypedQuery<WalletPrivateKeyEntity> query = getEntityManager().createNamedQuery("WalletKey.findByWalletId",
                                                                                   WalletPrivateKeyEntity.class);
    query.setParameter("walletId", walletId);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  @Override
  public void deleteAll() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteAll(List<WalletPrivateKeyEntity> entities) {
    throw new UnsupportedOperationException();
  }

}
