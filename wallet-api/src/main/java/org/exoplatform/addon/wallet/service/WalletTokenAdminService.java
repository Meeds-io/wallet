package org.exoplatform.addon.wallet.service;

import java.math.BigInteger;

import org.exoplatform.addon.wallet.model.ContractDetail;
import org.exoplatform.addon.wallet.model.Wallet;
import org.exoplatform.addon.wallet.model.transaction.TransactionDetail;

public interface WalletTokenAdminService {

  /**
   * Generates admin account wallet in server side
   */
  public void createAdminAccount();

  /**
   * Creates admin account wallet in server side
   * 
   * @param privateKey admin account wallet private key
   * @param currentUser current user creating wallet
   * @throws IllegalAccessException if current user is not allowed to create
   *           admin wallet account
   */
  public void createAdminAccount(String privateKey, String currentUser) throws IllegalAccessException;

  /**
   * @return Admin wallet object
   */
  public Wallet getAdminWallet();

  /**
   * @return Admin wallet address
   */
  public String getAdminWalletAddress();

  BigInteger getEtherBalanceOf(String address) throws Exception;// NOSONAR

  TransactionDetail reward(String receiver,
                           double tokenAmount,
                           double rewardAmount,
                           String label,
                           String message,
                           String username) throws Exception;// NOSONAR

  TransactionDetail reward(TransactionDetail transactionDetail, String issuerUsername) throws Exception;// NOSONAR

  TransactionDetail transfer(String receiver,
                             double tokenAmount,
                             String label,
                             String message,
                             String issuerUsername,
                             boolean enableChecksBeforeSending) throws Exception;// NOSONAR

  TransactionDetail transfer(TransactionDetail transactionDetail,
                             String issuerUsername,
                             boolean enableChecksBeforeSending) throws Exception;// NOSONAR

  TransactionDetail initialize(String receiver, String issuerUsername) throws Exception;// NOSONAR

  TransactionDetail initialize(TransactionDetail transactionDetail, String issuerUsername) throws Exception;// NOSONAR

  TransactionDetail disapproveAccount(String receiver, String issuerUsername) throws Exception;// NOSONAR

  TransactionDetail disapproveAccount(TransactionDetail transactionDetail, String issuerUsername) throws Exception;// NOSONAR

  TransactionDetail approveAccount(String receiver, String issuerUsername) throws Exception;// NOSONAR

  TransactionDetail approveAccount(TransactionDetail transactionDetail, String issuerUsername) throws Exception;// NOSONAR

  BigInteger balanceOf(String address) throws Exception;// NOSONAR

  boolean isInitializedAccount(String address) throws Exception;// NOSONAR

  boolean isAdminAccount(String address) throws Exception;// NOSONAR

  int getAdminLevel(String address) throws Exception;// NOSONAR

  boolean isApprovedAccount(String address) throws Exception;// NOSONAR

  String getContractAddress();

  void reinit();

  ContractDetail loadContractDetailFromBlockchain(String contractAddress);

}
