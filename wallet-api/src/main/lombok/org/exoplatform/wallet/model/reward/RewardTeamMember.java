package org.exoplatform.wallet.model.reward;

import java.io.Serializable;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;

import lombok.Data;

@Data
public class RewardTeamMember implements Serializable {
  private static final long serialVersionUID = -2614989453007394487L;

  private String            id;

  private String            providerId       = OrganizationIdentityProvider.NAME;

  private Long              technicalId;

  private Long              identityId;

}
