<%@ page import="org.exoplatform.social.webui.Utils"%>
<%
  String profileOwnerId = Utils.getOwnerIdentityId();
%>
<div class="VuetifyApp">
  <div data-app="true"
    class="v-application v-application--is-ltr theme--light walletOverviewApplication"
    id="WalletOverview">
    <v-cacheable-dom-app cache-id="WalletOverview_<%=profileOwnerId%>"></v-cacheable-dom-app>
    <script type="text/javascript" defer="defer">
      eXo.env.portal.addOnLoadCallback(() => {
        require(['PORTLET/wallet/WalletOverview'], app => app.init());
      });
    </script>
  </div>
</div>