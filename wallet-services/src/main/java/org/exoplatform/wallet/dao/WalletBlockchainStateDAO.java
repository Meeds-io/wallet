package org.exoplatform.wallet.dao;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.wallet.entity.WalletBlockchainStateEntity;

public class WalletBlockchainStateDAO extends GenericDAOJPAImpl<WalletBlockchainStateEntity, Long> {

  public WalletBlockchainStateEntity findByWalletIdAndContract(long walletId, String contractAddress) {
    TypedQuery<WalletBlockchainStateEntity> query =
                                                  getEntityManager().createNamedQuery("WalletBlockchainState.findByWalletIdAndContract",
                                                                                      WalletBlockchainStateEntity.class);
    query.setParameter("walletId", walletId);
    query.setParameter("contractAddress", contractAddress);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

}
