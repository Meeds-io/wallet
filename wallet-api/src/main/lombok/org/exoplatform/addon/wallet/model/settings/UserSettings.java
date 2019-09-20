package org.exoplatform.addon.wallet.model.settings;

import org.exoplatform.addon.wallet.model.Wallet;
import org.exoplatform.addon.wallet.utils.WalletUtils;

import lombok.*;
import lombok.EqualsAndHashCode.Exclude;

@Data
@ToString
@EqualsAndHashCode(callSuper = true)
public class UserSettings extends GlobalSettings {

  private static final long serialVersionUID = -1053053050527461491L;

  private Wallet            wallet           = null;

  @Exclude
  private WalletSettings    userPreferences;

  @Exclude
  private boolean           walletEnabled    = true;

  @Exclude
  private String            cometdChannel    = WalletUtils.COMETD_CHANNEL;

  @Exclude
  private String            cometdToken;

  @Exclude
  private String            cometdContext;

  public UserSettings(GlobalSettings globalSettings) {
    super(globalSettings);
  }
}
