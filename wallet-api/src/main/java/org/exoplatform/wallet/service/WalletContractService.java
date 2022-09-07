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
package org.exoplatform.wallet.service;

import java.io.IOException;

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

}
