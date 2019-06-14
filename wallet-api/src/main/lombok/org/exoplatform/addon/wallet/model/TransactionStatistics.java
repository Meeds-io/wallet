package org.exoplatform.addon.wallet.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class TransactionStatistics {

  private List<String> labels;

  private List<String> income  = new ArrayList<>();

  private List<String> outcome = new ArrayList<>();

}
