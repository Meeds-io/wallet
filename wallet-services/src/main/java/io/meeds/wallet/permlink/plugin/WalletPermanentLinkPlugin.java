/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.wallet.permlink.plugin;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.WalletType;
import org.exoplatform.wallet.service.WalletAccountService;
import org.exoplatform.wallet.utils.WalletUtils;

import io.meeds.portal.permlink.model.PermanentLinkObject;
import io.meeds.portal.permlink.plugin.PermanentLinkPlugin;

public class WalletPermanentLinkPlugin implements PermanentLinkPlugin {

  private static final String     OBJECT_TYPE = "wallet";

  public static final String      URL_FORMAT  = "/portal/%s/wallet";

  private SpaceService            spaceService;

  private WalletAccountService    walletAccountService;

  private UserPortalConfigService portalConfigService;

  public WalletPermanentLinkPlugin(SpaceService spaceService,
                                   WalletAccountService walletAccountService,
                                   UserPortalConfigService portalConfigService) {
    this.spaceService = spaceService;
    this.walletAccountService = walletAccountService;
    this.portalConfigService = portalConfigService;
  }

  @Override
  public String getObjectType() {
    return OBJECT_TYPE;
  }

  @Override
  public boolean canAccess(PermanentLinkObject object, Identity identity) throws ObjectNotFoundException {
    String identityId = object.getObjectId();
    Wallet wallet = walletAccountService.getWalletByIdentityId(Long.parseLong(identityId));
    return wallet != null && WalletUtils.canAccessWallet(wallet, identity.getUserId());
  }

  @Override
  public String getDirectAccessUrl(PermanentLinkObject object) throws ObjectNotFoundException {
    String identityId = object.getObjectId();
    Wallet wallet = walletAccountService.getWalletByIdentityId(Long.parseLong(identityId));
    String url = WalletType.isSpace(wallet.getType()) ? getSpaceUrl(spaceService.getSpaceByPrettyName(wallet.getName())) :
                                                      getProfileUrl();
    if (object.getParameters() != null && object.getParameters().containsKey("transactionHash")) {
      url += "?hash=" + object.getParameters().get("transactionHash");
    }
    return url;
  }

  public String getSpaceUrl(Space space) {
    return String.format("/portal/s/%s/SpaceWallet", space.getId());
  }

  public String getProfileUrl() {
    return String.format(URL_FORMAT, portalConfigService.getMetaPortal());
  }

}
