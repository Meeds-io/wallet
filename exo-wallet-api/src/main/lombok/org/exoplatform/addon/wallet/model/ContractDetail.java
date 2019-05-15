package org.exoplatform.addon.wallet.model;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContractDetail implements Serializable {

  private static final String DEFAULT_CONTRACT_PARAM = "defaultContract";

  private static final String CONTRACT_TYPE_PARAM    = "contractType";

  private static final String SELL_PRICE_PARAM       = "sellPrice";

  private static final String OWNER_PARAM            = "owner";

  private static final String NETWORK_ID_PARAM       = "networkId";

  private static final String DECIMALS_PARAM         = "decimals";

  private static final String SYMBOL_PARAM           = "symbol";

  private static final String NAME_PARAM             = "name";

  private static final String ADDRESS_PARAM          = "address";

  private static final long   serialVersionUID       = 1459881604949041768L;

  private String              address;

  private String              name;

  private String              symbol;

  private Integer             decimals;

  private Long                networkId;

  private String              owner;

  private String              sellPrice;

  private String              contractType;

  private boolean             defaultContract;

  public String toJSONString() {
    return toJSONObject().toString();
  }

  public JSONObject toJSONObject() {
    JSONObject jsonObject = new JSONObject();
    try {
      if (StringUtils.isNotBlank(name)) {
        jsonObject.put(ADDRESS_PARAM, address);
      }
      if (StringUtils.isNotBlank(name)) {
        jsonObject.put(NAME_PARAM, name);
      }
      if (StringUtils.isNotBlank(symbol)) {
        jsonObject.put(SYMBOL_PARAM, symbol);
      }
      if (decimals != null) {
        jsonObject.put(DECIMALS_PARAM, decimals);
      }
      if (networkId != null) {
        jsonObject.put(NETWORK_ID_PARAM, networkId);
      }
      if (StringUtils.isNotBlank(owner)) {
        jsonObject.put(OWNER_PARAM, owner);
      }
      if (StringUtils.isNotBlank(sellPrice)) {
        jsonObject.put(SELL_PRICE_PARAM, sellPrice);
      }
      if (StringUtils.isNotBlank(contractType)) {
        jsonObject.put(CONTRACT_TYPE_PARAM, contractType);
      }
      jsonObject.put(DEFAULT_CONTRACT_PARAM, defaultContract);
    } catch (JSONException e) {
      throw new IllegalStateException("Error while converting Object to JSON", e);
    }
    return jsonObject;
  }

  public static final ContractDetail parseStringToObject(String jsonString) {
    if (StringUtils.isBlank(jsonString)) {
      return null;
    }
    try {
      JSONObject jsonObject = new JSONObject(jsonString);
      ContractDetail contractDetail = new ContractDetail();
      contractDetail.setNetworkId(jsonObject.has(NETWORK_ID_PARAM) ? jsonObject.getLong(NETWORK_ID_PARAM) : 0);
      contractDetail.setAddress(jsonObject.has(ADDRESS_PARAM) ? jsonObject.getString(ADDRESS_PARAM) : null);
      contractDetail.setName(jsonObject.has(NAME_PARAM) ? jsonObject.getString(NAME_PARAM) : null);
      contractDetail.setSymbol(jsonObject.has(SYMBOL_PARAM) ? jsonObject.getString(SYMBOL_PARAM) : null);
      contractDetail.setDecimals(jsonObject.has(DECIMALS_PARAM) ? jsonObject.getInt(DECIMALS_PARAM) : 0);
      contractDetail.setOwner(jsonObject.has(OWNER_PARAM) ? jsonObject.getString(OWNER_PARAM) : null);
      contractDetail.setSellPrice(jsonObject.has(SELL_PRICE_PARAM) ? jsonObject.getString(SELL_PRICE_PARAM) : null);
      contractDetail.setContractType(jsonObject.has(CONTRACT_TYPE_PARAM) ? jsonObject.getString(CONTRACT_TYPE_PARAM) : null);
      contractDetail.setDefaultContract(jsonObject.has(DEFAULT_CONTRACT_PARAM) ? jsonObject.getBoolean(DEFAULT_CONTRACT_PARAM)
                                                                               : Boolean.TRUE);
      return contractDetail;
    } catch (JSONException e) {
      throw new IllegalStateException("Error while converting JSON String to Object", e);
    }
  }

  @Override
  public String toString() {
    return toJSONString();
  }
}
