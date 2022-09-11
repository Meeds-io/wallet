package org.exoplatform.wallet.test.mock;

import org.exoplatform.wallet.model.ContractDetail;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.WalletTokenAdminService;

import java.math.BigInteger;
import java.util.Set;

public class EthereumWalletTokenAdminServiceMock implements WalletTokenAdminService {
  @Override
  public void createAdminAccount() {

  }

  @Override
  public Wallet createAdminAccount(String privateKey, String issuerUsername) throws IllegalAccessException {
    return new Wallet();
  }

  @Override
  public String getAdminWalletAddress() {
    return "0x00112233445566778899";
  }

  @Override
  public String generateHash(String rawTransaction) {
    return null;
  }

  @Override
  public TransactionDetail reward(TransactionDetail transactionDetail, String issuerUsername) throws Exception {
    return null;
  }

  @Override
  public TransactionDetail sendEther(TransactionDetail transactionDetail, String issuerUsername) throws Exception {
    return null;
  }

  @Override
  public TransactionDetail sendToken(TransactionDetail transactionDetail, String issuerUsername) throws Exception {
    return null;
  }

  @Override
  public BigInteger getTokenBalanceOf(String address) throws Exception {
    return null;
  }

  @Override
  public BigInteger getEtherBalanceOf(String address) throws Exception {
    return null;
  }

  @Override
  public boolean isInitializedAccount(Wallet wallet) throws Exception {
    return false;
  }

  @Override
  public void refreshContractDetailFromBlockchain(ContractDetail contractDetail) {

  }

  @Override
  public void retrieveWalletInformationFromBlockchain(Wallet wallet, ContractDetail contractDetail, Set<String> walletModifications) throws Exception {

  }

  @Override
  public void boostAdminTransactions() throws Exception {

  }
}
