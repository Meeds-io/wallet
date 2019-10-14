package org.exoplatform.wallet.plugin;

import org.exoplatform.social.core.identity.IdentityProvider;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;

public class WalletAdminIdentityProvider extends IdentityProvider<String> {

  public static final String NAME = "WALLET_ADMIN";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String findByRemoteId(String remoteId) {
    return remoteId;
  }

  @Override
  public Identity createIdentity(String remoteId) {
    return new Identity(NAME, remoteId);
  }

  @Override
  public void populateProfile(Profile profile, String remoteId) {
    // No specific properties
  }

}
