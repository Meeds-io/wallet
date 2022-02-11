package org.exoplatform.wallet.test.mock;

import org.exoplatform.wallet.statistic.ExoWalletStatisticService;

import java.util.HashMap;
import java.util.Map;

public class EthereumClientConnectorMock implements ExoWalletStatisticService {
  @Override
  public Map<String, Object> getStatisticParameters(String operation, Object result, Object... methodArgs) {
    return new HashMap<>();
  }
}
