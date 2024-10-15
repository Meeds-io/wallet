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
package org.exoplatform.wallet.test.mock;

import java.util.List;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.social.core.application.PortletPreferenceRequiredPlugin;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.space.*;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceLifeCycleListener;
import org.exoplatform.social.core.space.spi.SpaceService;

@SuppressWarnings("all")
public class SpaceServiceMock implements SpaceService {

  public Space getSpaceByPrettyName(String spacePrettyName) {
    Space space = new Space();
    space.setPrettyName(spacePrettyName);
    space.setId(String.valueOf(spacePrettyName.charAt(spacePrettyName.length() - 1)));
    space.setGroupId("/spaces/" + spacePrettyName);
    return space;
  }

  public Space getSpaceById(String spaceId) {
    return null;
  }

  public Space updateSpace(Space existingSpace) {
    String SpacePrettyName = existingSpace.getPrettyName();
    String description = "updateSpace";
    existingSpace.setPrettyName(existingSpace.getPrettyName());
    existingSpace.setDescription(description);
    existingSpace.setId(String.valueOf(SpacePrettyName.charAt(SpacePrettyName.length() - 1)));
    existingSpace.setGroupId("/spaces/" + SpacePrettyName);
    return existingSpace;
  }

}
