package org.exoplatform.addon.wallet.model.settings;

import org.exoplatform.addon.wallet.model.Wallet;

import lombok.*;
import lombok.EqualsAndHashCode.Exclude;

@Data
@ToString
@EqualsAndHashCode(callSuper = false)
public class UserSettings extends GlobalSettings {

  private static final long serialVersionUID = -1053053050527461491L;

  private Wallet            wallet           = null;

  @Exclude
  private WalletSettings    userPreferences;

  @Exclude
  private boolean           isAdmin          = false;

  @Exclude
  private boolean           walletEnabled    = true;

  public UserSettings(GlobalSettings globalSettings) {
    super(globalSettings);
  }
}
