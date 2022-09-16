package org.exoplatform.wallet.blockchain;

public class BlockchainRequestException extends RuntimeException {

  private static final long serialVersionUID = -2980322954170797218L;

  public BlockchainRequestException(String transactionHash, Throwable e) {
    super("Error while sending transaction " + transactionHash + " to blockchain", e);
  }

}
