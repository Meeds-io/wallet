/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.wallet.entity;

import java.io.Serializable;

import jakarta.persistence.*;

import org.hibernate.annotations.DynamicUpdate;

import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity(name = "WalletTransaction")
@ExoEntity
@DynamicUpdate
@Table(name = "ADDONS_WALLET_TRANSACTION")
@NamedQuery(name = "WalletTransaction.countReceivedContractAmount", query = "SELECT SUM(tx.contractAmount) FROM WalletTransaction tx WHERE tx.contractAddress = :contractAddress AND tx.toAddress = :address AND (tx.contractMethodName = 'reward' OR tx.contractMethodName = 'initializeAccount' OR tx.contractMethodName = 'transfer' OR tx.contractMethodName = 'transferFrom') AND tx.createdDate >= :startDate AND tx.createdDate < :endDate")
@NamedQuery(name = "WalletTransaction.countSentContractAmount", query = "SELECT SUM(tx.contractAmount) FROM WalletTransaction tx WHERE tx.contractAddress = :contractAddress AND tx.fromAddress = :address AND (tx.contractMethodName = 'reward' OR tx.contractMethodName = 'initializeAccount' OR tx.contractMethodName = 'transfer' OR tx.contractMethodName = 'transferFrom') AND tx.createdDate >= :startDate AND tx.createdDate < :endDate")
@NamedQuery(name = "WalletTransaction.getContractTransactions", query = "SELECT tx FROM WalletTransaction tx WHERE (tx.contractAddress = :contractAddress OR tx.toAddress = :contractAddress) ORDER BY tx.createdDate DESC")
@NamedQuery(name = "WalletTransaction.getContractTransactionsWithMethodName", query = "SELECT tx FROM WalletTransaction tx WHERE (tx.contractAddress = :contractAddress OR tx.toAddress = :contractAddress) AND tx.contractMethodName = :methodName ORDER BY tx.createdDate DESC")
@NamedQuery(name = "WalletTransaction.getNetworkTransactions", query = "SELECT tx FROM WalletTransaction tx WHERE tx.networkId = :networkId ORDER BY tx.createdDate DESC")
@NamedQuery(name = "WalletTransaction.getPendingEtherTransactions", query = "SELECT tx FROM WalletTransaction tx WHERE tx.networkId = :networkId AND (tx.fromAddress = :address OR tx.toAddress = :address) AND tx.isPending = TRUE AND tx.contractAddress IS NULL AND tx.sentDate > 0 ORDER BY tx.nonce ASC")
@NamedQuery(name = "WalletTransaction.getPendingWalletTransactionsSent", query = "SELECT tx FROM WalletTransaction tx WHERE tx.networkId = :networkId AND tx.fromAddress = :address AND tx.isPending = TRUE AND tx.sentDate > 0 ORDER BY tx.nonce ASC")
@NamedQuery(name = "WalletTransaction.getPendingWalletTransactionsNotSent", query = "SELECT tx FROM WalletTransaction tx WHERE tx.networkId = :networkId AND tx.fromAddress = :address AND tx.isPending = TRUE AND tx.sentDate = 0 ORDER BY tx.nonce ASC")
@NamedQuery(name = "WalletTransaction.countContractPendingTransactionsSent", query = "SELECT count(tx) FROM WalletTransaction tx WHERE tx.networkId = :networkId AND tx.isPending = TRUE AND tx.contractAddress IS NOT NULL AND tx.sentDate > 0")
@NamedQuery(name = "WalletTransaction.getTransactionByHash", query = "SELECT tx FROM WalletTransaction tx WHERE tx.hash = :hash order by tx.id desc")
@NamedQuery(name = "WalletTransaction.getPendingTransactionByHash", query = "SELECT tx FROM WalletTransaction tx WHERE tx.hash = :hash and tx.isPending=true order by tx.createdDate DESC")
@NamedQuery(name = "WalletTransaction.getPendingTransactionsWithSameNonce", query = "SELECT tx FROM WalletTransaction tx WHERE tx.isPending = TRUE AND tx.nonce = :nonce AND tx.networkId = :networkId AND tx.fromAddress = :address AND tx.hash <> :hash")
@NamedQuery(name = "WalletTransaction.countPendingTransactionsWithSameNonce", query = "SELECT count(tx) FROM WalletTransaction tx WHERE tx.isPending = TRUE AND tx.nonce = :nonce AND tx.networkId = :networkId AND tx.fromAddress = :address AND tx.hash <> :hash")
@NamedQuery(name = "WalletTransaction.getMaxUsedNonce", query = "SELECT MAX(tx.nonce) FROM WalletTransaction tx WHERE tx.networkId = :networkId AND tx.fromAddress = :address")
@NamedQuery(name = "WalletTransaction.getTransactionsToSend", query = "SELECT tx FROM WalletTransaction tx WHERE tx.networkId = :networkId AND tx.isPending = TRUE AND tx.rawTransaction IS NOT NULL ORDER BY tx.nonce ASC")
@NamedQuery(name = "WalletTransaction.countContractPendingTransactionsToSend", query = "SELECT count(tx) FROM WalletTransaction tx WHERE tx.networkId = :networkId AND tx.isPending = TRUE AND tx.contractAddress IS NOT NULL AND tx.rawTransaction IS NOT NULL AND tx.sentDate = 0")
@NamedQuery(name = "WalletTransaction.countPendingTransactionSent", query = "SELECT count(tx) FROM WalletTransaction tx WHERE tx.networkId = :networkId AND tx.isPending = TRUE AND tx.fromAddress = :address AND tx.rawTransaction IS NOT NULL AND tx.sentDate > 0")
@NamedQuery(name = "WalletTransaction.countPendingTransactionAsSender", query = "SELECT count(tx) FROM WalletTransaction tx WHERE tx.networkId = :networkId AND tx.isPending = TRUE AND tx.fromAddress = :address")
public class TransactionEntity implements Serializable {

  private static final long serialVersionUID = 485850826850947238L;

  @Id
  @SequenceGenerator(name = "SEQ_WALLET_TRANSACTION", sequenceName = "SEQ_WALLET_TRANSACTION", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_WALLET_TRANSACTION")
  @Column(name = "TRANSACTION_ID")
  private long              id;

  @Column(name = "NETWORK_ID", nullable = false)
  private long              networkId;

  @Column(name = "ISSUER_ID")
  private long              issuerIdentityId;

  @Column(name = "HASH", unique = true, nullable = false)
  private String            hash;

  @Column(name = "PENDING")
  private boolean           isPending;

  @Column(name = "SUCCESS")
  private boolean           isSuccess;

  @Column(name = "DROPPED")
  private boolean           isDropped;

  @Column(name = "ADMIN_OP")
  private boolean           isAdminOperation;

  @Column(name = "FROM_ADDRESS", nullable = false)
  private String            fromAddress;

  @Column(name = "TO_ADDRESS")
  private String            toAddress;

  @Column(name = "BY_ADDRESS")
  private String            byAddress;

  @Column(name = "LABEL")
  private String            label;

  @Column(name = "MESSAGE")
  private String            message;

  @Column(name = "VALUE")
  private double            value;

  @Column(name = "CONTRACT_ADDRESS")
  private String            contractAddress;

  @Column(name = "CONTRACT_METHOD")
  private String            contractMethodName;

  @Column(name = "CONTRACT_AMOUNT")
  private double            contractAmount;

  @Column(name = "CREATED_DATE")
  private long              createdDate;

  @Column(name = "GAS_USED")
  private int               gasUsed;

  @Column(name = "GAS_PRICE")
  private double            gasPrice;

  @Column(name = "TOKEN_FEE")
  private double            tokenFee;

  @Column(name = "ETHER_FEE")
  private double            etherFee;

  @Column(name = "NO_CONTRACT_FUNDS")
  private boolean           noContractFunds;

  @Column(name = "NONCE")
  private long              nonce;

  @Column(name = "BOOST")
  private boolean           boost;

  @Column(name = "RAW_TRANSACTION")
  private String            rawTransaction;

  @Column(name = "SENT_DATE")
  private long              sentDate;

  @Column(name = "SENDING_ATTEMPT_COUNT")
  private long              sendingAttemptCount;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public long getNetworkId() {
    return networkId;
  }

  public void setNetworkId(long networkId) {
    this.networkId = networkId;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public boolean isPending() {
    return isPending;
  }

  public void setPending(boolean isPending) {
    this.isPending = isPending;
  }

  public boolean isSuccess() {
    return isSuccess;
  }

  public void setSuccess(boolean isSuccess) {
    this.isSuccess = isSuccess;
  }

  public boolean isDropped() {
    return isDropped;
  }

  public void setDropped(boolean isDropped) {
    this.isDropped = isDropped;
  }

  public boolean isAdminOperation() {
    return isAdminOperation;
  }

  public void setAdminOperation(boolean isAdminOperation) {
    this.isAdminOperation = isAdminOperation;
  }

  public String getFromAddress() {
    return fromAddress;
  }

  public void setFromAddress(String fromAddress) {
    this.fromAddress = fromAddress;
  }

  public String getToAddress() {
    return toAddress;
  }

  public String getByAddress() {
    return byAddress;
  }

  public void setByAddress(String byAddress) {
    this.byAddress = byAddress;
  }

  public void setToAddress(String toAddress) {
    this.toAddress = toAddress;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public double getValue() {
    return value;
  }

  public void setValue(double value) {
    this.value = value;
  }

  public String getContractAddress() {
    return contractAddress;
  }

  public void setContractAddress(String contractAddress) {
    this.contractAddress = contractAddress;
  }

  public String getContractMethodName() {
    return contractMethodName;
  }

  public void setContractMethodName(String contractMethodName) {
    this.contractMethodName = contractMethodName;
  }

  public double getContractAmount() {
    return contractAmount;
  }

  public void setContractAmount(double contractAmount) {
    this.contractAmount = contractAmount;
  }

  public long getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(long createdDate) {
    this.createdDate = createdDate;
  }

  public long getIssuerIdentityId() {
    return issuerIdentityId;
  }

  public void setIssuerIdentityId(long issuerIdentityId) {
    this.issuerIdentityId = issuerIdentityId;
  }

  public int getGasUsed() {
    return gasUsed;
  }

  public void setGasUsed(int gasUsed) {
    this.gasUsed = gasUsed;
  }

  public double getGasPrice() {
    return gasPrice;
  }

  public void setGasPrice(double gasPrice) {
    this.gasPrice = gasPrice;
  }

  public double getTokenFee() {
    return tokenFee;
  }

  public void setTokenFee(double tokenFee) {
    this.tokenFee = tokenFee;
  }

  public double getEtherFee() {
    return etherFee;
  }

  public void setEtherFee(double etherFee) {
    this.etherFee = etherFee;
  }

  public boolean isNoContractFunds() {
    return noContractFunds;
  }

  public void setNoContractFunds(boolean noContractFunds) {
    this.noContractFunds = noContractFunds;
  }

  public long getNonce() {
    return nonce;
  }

  public void setNonce(long nonce) {
    this.nonce = nonce;
  }

  public boolean isBoost() {
    return boost;
  }

  public void setBoost(boolean boost) {
    this.boost = boost;
  }

  public String getRawTransaction() {
    return rawTransaction;
  }

  public void setRawTransaction(String rawTransaction) {
    this.rawTransaction = rawTransaction;
  }

  public long getSentDate() {
    return sentDate;
  }

  public void setSentDate(long sentDate) {
    this.sentDate = sentDate;
  }

  public long getSendingAttemptCount() {
    return sendingAttemptCount;
  }

  public void setSendingAttemptCount(long sendingAttemptCount) {
    this.sendingAttemptCount = sendingAttemptCount;
  }

}
