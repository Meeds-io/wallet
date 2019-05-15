package org.exoplatform.addon.wallet.service;

import java.io.IOException;
import java.util.Set;

import org.json.JSONArray;

import org.exoplatform.addon.wallet.model.ContractDetail;

public interface WalletContractService {

  /**
   * @param address contract address to check
   * @param networkId blockchain network id
   * @return true if contract address is a watched contract
   */
  public boolean isContract(String address, long networkId);

  /**
   * Save a new contract details
   * 
   * @param contractDetail contract details to save
   */
  public void saveContract(ContractDetail contractDetail);

  /**
   * Removes a contract address from default contracts displayed in wallet of
   * all users
   * 
   * @param address contract address to remove from watched list
   * @param networkId blockchain network id where contract is deployed
   * @return true if removed
   */
  public boolean removeDefaultContract(String address, Long networkId);

  /**
   * Get contract detail
   * 
   * @param address contract address to get from watched list
   * @param networkId blockchain network id where contract is deployed
   * @return {@link ContractDetail} contract details
   */
  public ContractDetail getContractDetail(String address, Long networkId);

  /**
   * Retrieves the list of default contract addreses
   * 
   * @param networkId blockchain network id where contract is deployed
   * @return {@link Set} of watched contracts addresses
   */
  public Set<String> getDefaultContractsAddresses(Long networkId);

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
}
