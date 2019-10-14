package org.exoplatform.wallet.service;

import java.io.IOException;
import java.util.Set;

import org.json.JSONArray;

import org.exoplatform.wallet.model.ContractDetail;

/**
 * Manages token contract properties
 */
public interface WalletContractService {

  /**
   * @param address contract address to check
   * @return true if contract address is a watched contract
   */
  boolean isContract(String address);

  /**
   * Save a new contract details
   * 
   * @param contractDetail contract details to save
   */
  void saveContractDetail(ContractDetail contractDetail);

  /**
   * Get contract detail
   * 
   * @param address contract address to get from watched list
   * @return {@link ContractDetail} contract details
   */
  ContractDetail getContractDetail(String address);

  /**
   * Get Contract ABI
   * 
   * @return {@link JSONArray} ABI of contract in JSON format
   */
  JSONArray getContractAbi();

  /**
   * Get Contract BINARY to deploy
   * 
   * @return UTF-8 String of contract BIN
   */
  String getContractBinary();

  /**
   * Retreive the ABI or BIN content of a contract
   * 
   * @param name contract name
   * @param extension contract ABI file extension ('json' or 'abi')
   * @return ABI of contract in JSON format represented in {@link String}
   * @throws IOException when an error occurs while getting contract ABI file
   *           from filesystem
   */
  String getContractFileContent(String name, String extension) throws IOException;

  /**
   * Refresh contract details from blockchain
   * 
   * @param contractModifications list of called method names to change contract
   *          state on blockchain
   */
  void refreshContractDetail(Set<String> contractModifications);
}
