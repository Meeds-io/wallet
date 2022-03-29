package org.exoplatform.wallet.dao;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.wallet.entity.WalletBackUpEntity;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.List;

public class WalletAccountBackUpDAO extends GenericDAOJPAImpl<WalletBackUpEntity, Long> {

  @Override
  public void deleteAll() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteAll(List<WalletBackUpEntity> entities) {
    throw new UnsupportedOperationException();
  }

  public WalletBackUpEntity findByWalletId(long walletId) {
    TypedQuery<WalletBackUpEntity> query = getEntityManager().createNamedQuery("WalletBackUpEntity.findByWalletId",
                                                                               WalletBackUpEntity.class);
    query.setParameter("walletId", walletId);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

}
