<%@ page import="jakarta.servlet.http.HttpServletRequest" %>
<%@ page import="jakarta.servlet.http.HttpSession" %>
<%@ page import="org.exoplatform.webui.application.WebuiRequestContext" %>
<%@ page import="org.exoplatform.web.application.RequestContext" %>
<%@ page import="org.exoplatform.wallet.utils.WalletUtils" %>
<%
    HttpServletRequest httpRequest = ((WebuiRequestContext) RequestContext.getCurrentInstance()).getRequest();
    HttpSession httpSession = httpRequest.getSession();
    String generatedToken = WalletUtils.generateToken(httpSession);
    %>
<div class="VuetifyApp">
  <div id="WalletApp">
    <script>
      require(['PORTLET/wallet/Wallet'], app => app.init('<%=generatedToken%>'))
    </script>
  </div>
</div>