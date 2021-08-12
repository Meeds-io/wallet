<%@ page import="java.math.BigDecimal"%>
<%@ page import="java.math.RoundingMode"%>
<%@ page import="org.exoplatform.social.webui.Utils"%>
<%@ page import="org.exoplatform.webui.application.WebuiRequestContext"%>
<%@ page import="org.exoplatform.web.application.RequestContext"%>
<%@ page import="org.exoplatform.container.ExoContainerContext"%>
<%@ page import="org.exoplatform.wallet.service.WalletService"%>
<%@ page import="org.exoplatform.wallet.service.WalletAccountService"%>
<%@ page import="org.exoplatform.services.resources.ResourceBundleService"%>
<%@ page import="org.exoplatform.wallet.model.settings.GlobalSettings"%>
<%@ page import="org.exoplatform.wallet.model.ContractDetail"%>
<%@ page import="org.exoplatform.wallet.model.Wallet"%>
<%
  String profileOwnerId = Utils.getOwnerIdentityId();

  String title = "Wallet";
  try {
    title = ExoContainerContext.getService(ResourceBundleService.class).getResourceBundle("locale.addon.Wallet", request.getLocale()).getString("exoplatform.wallet.title.walletBalanceTitle");
  } catch (Exception e) {
    // Expected, when the title isn't translated to user locale
  }
  GlobalSettings globalSettings = ExoContainerContext.getService(WalletService.class).getSettings();
  String symbol = globalSettings == null || globalSettings.getContractDetail() == null ? "" : globalSettings.getContractDetail().getSymbol();
  Wallet wallet = ExoContainerContext.getService(WalletAccountService.class).getWalletByIdentityId(Long.parseLong(profileOwnerId));
  double balance = wallet == null || wallet.getTokenBalance() == null ? 0d : wallet.getTokenBalance();
  String balanceFixed = BigDecimal.valueOf(balance).setScale(2, RoundingMode.DOWN).toString().replace(".00", "");
%>
<div class="VuetifyApp">
  <div data-app="true"
    class="v-application v-application--is-ltr theme--light"
    id="walletBalance" flat="">
    <div class="v-application--wrap">
      <main>
        <div class="container pa-0">
          <div class="layout row wrap mx-0" style="cursor: pointer;">
            <div class="flex d-flex sx12">
              <div class="layout white row ma-0">
                <div class="flex d-flex xs12">
                  <div class="v-card v-card--flat v-sheet theme--light">
                    <div class="v-card__text subtitle-2 text-sub-title pa-2">
                      <%=title%>
                    </div>
                  </div>
                </div>
                <div class="flex d-flex xs12 justify-center">
                  <div class="v-card v-card--flat v-sheet theme--light">
                    <div class="v-card__text pa-2">
                      <a href="/portal/dw/wallet"
                        class="text-color display-1 font-weight-bold big-number">
                        <%=balanceFixed%> <%=symbol%>
                      </a>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</div>