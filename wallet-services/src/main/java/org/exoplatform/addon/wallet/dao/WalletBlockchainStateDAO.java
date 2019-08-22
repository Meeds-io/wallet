package org.exoplatform.addon.wallet.dao;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.exoplatform.addon.wallet.entity.WalletBlockchainStateEntity;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;

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
