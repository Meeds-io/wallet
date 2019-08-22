package org.exoplatform.addon.wallet.blockchain.statistic;

import java.util.Map;

/**
 * This service is used to add statistic log entry by using annotation
 * {@link ExoWalletStatistic} on methods
 */
public interface ExoWalletStatisticService {
  /**
   * Retrieve statistic log parameters
   * 
   * @param statisticType
   * @param result 
   * @param methodArgs
   * @return a {@link Map} of parameters to include in statistic log
   */
  Map<String, Object> getParameters(String statisticType, Object result, Object... methodArgs);
}
