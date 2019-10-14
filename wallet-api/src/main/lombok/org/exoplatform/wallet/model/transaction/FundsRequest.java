package org.exoplatform.wallet.model.transaction;

import java.io.Serializable;

import lombok.Data;

@Data
public class FundsRequest implements Serializable {
  private static final long serialVersionUID = -18387327085608555L;

  private String            address;

  private String            receipientType;

  private String            receipient;

  private String            contract;

  private Double            amount;

  private String            message;
}
