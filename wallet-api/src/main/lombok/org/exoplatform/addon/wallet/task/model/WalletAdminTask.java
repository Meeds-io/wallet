package org.exoplatform.addon.wallet.task.model;

import java.io.Serializable;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode.Exclude;

@Data
public class WalletAdminTask implements Serializable {
  private static final long serialVersionUID = 4892901604177868381L;

  private long              id;

  @Exclude
  private String            link;

  @Exclude
  private String            message;

  @Exclude
  private String            type;

  @Exclude
  private boolean           completed;

  @Exclude
  private List<String>      parameters;
}
