package org.exoplatform.wallet.dao;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.wallet.entity.WalletBackupEntity;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.List;

public class WalletAccountBackupDAO extends GenericDAOJPAImpl<WalletBackupEntity, Long> {

  @Override
  public void deleteAll() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteAll(List<WalletBackupEntity> entities) {
    throw new UnsupportedOperationException();
  }

  public WalletBackupEntity findByWalletId(long walletId) {
    TypedQuery<WalletBackupEntity> query = getEntityManager().createNamedQuery("WalletBackupEntity.findByWalletId",
                                                                               WalletBackupEntity.class);
    query.setParameter("walletId", walletId);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

}
