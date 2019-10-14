package org.exoplatform.wallet.model.transaction;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class TransactionStatistics {

  private String       periodicityLabel;

  private List<String> labels;

  private List<String> income  = new ArrayList<>();

  private List<String> outcome = new ArrayList<>();

}
