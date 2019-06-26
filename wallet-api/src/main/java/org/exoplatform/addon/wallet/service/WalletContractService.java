package org.exoplatform.addon.wallet.service;

import java.io.IOException;

import org.json.JSONArray;

import org.exoplatform.addon.wallet.model.ContractDetail;

/**
 * Manages token contract properties
 */
public interface WalletContractService {

  /**
   * @param address contract address to check
   * @return true if contract address is a watched contract
   */
  public boolean isContract(String address);

  /**
   * Save a new contract details
   * 
   * @param contractDetail contract details to save
   */
  public void saveContractDetail(ContractDetail contractDetail);

  /**
   * Get contract detail
   * 
   * @param address contract address to get from watched list
   * @return {@link ContractDetail} contract details
   */
  public ContractDetail getContractDetail(String address);

  /**
   * Get Contract ABI
   * 
   * @return {@link JSONArray} ABI of contract in JSON format
   */
  public JSONArray getContractAbi();

  /**
   * Get Contract BINARY to deploy
   * 
   * @return UTF-8 String of contract BIN
   */
  public String getContractBinary();

  /**
   * Retreive the ABI or BIN content of a contract
   * 
   * @param name contract name
   * @param extension contract ABI file extension ('json' or 'abi')
   * @return ABI of contract in JSON format represented in {@link String}
   * @throws IOException when an error occurs while getting contract ABI file
   *           from filesystem
   */
  public String getContractFileContent(String name, String extension) throws IOException;

  /**
   * Refresh contract details from blockchain
   */
  public void refreshContractDetail();
}
