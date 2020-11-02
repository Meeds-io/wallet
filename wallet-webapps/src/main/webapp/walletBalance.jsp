<%@ page import="org.exoplatform.social.webui.Utils"%>
<%
  String profileOwnerId = Utils.getOwnerIdentityId();
%>
<div class="VuetifyApp">
  <div data-app="true"
    class="v-application v-application--is-ltr theme--light"
    id="walletBalance">
    <v-cacheable-dom-app cache-id="walletBalance_<%=profileOwnerId%>"></v-cacheable-dom-app>
    <script>
      require(['SHARED/walletBalanceBundle'], function(walletBalanceApp) {
        walletBalanceApp.init();
      })
    </script>
  </div>
</div>