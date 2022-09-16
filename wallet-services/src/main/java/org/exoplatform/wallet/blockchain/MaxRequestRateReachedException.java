package org.exoplatform.wallet.blockchain;

public class MaxRequestRateReachedException extends RuntimeException {

  private static final long serialVersionUID = -4897739164628633063L;

  public MaxRequestRateReachedException(String transactionHash, String message) {
    super("Blockchain Provider Requests Rate limit reached. Transaction " + transactionHash + " wasn't sent. Error Message: "
        + message);
  }

}
