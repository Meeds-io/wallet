<%@ page import="org.exoplatform.social.webui.Utils"%>
<%
  String profileOwnerId = Utils.getOwnerIdentityId();
%>
<div class="VuetifyApp">
  <div data-app="true"
    class="v-application v-application--is-ltr theme--light"
    id="walletBalance">
    <v-cacheable-dom-app cache-id="walletBalance_<%=profileOwnerId%>"></v-cacheable-dom-app>
    <script type="text/javascript" defer="defer">
      eXo.env.portal.addOnLoadCallback(() => {
        require(['SHARED/walletBalanceBundle'], app => app.init());
      });
    </script>
  </div>
</div>