<%@page import="org.exoplatform.portal.application.PortalRequestContext"%>
<%@ page import="jakarta.servlet.http.HttpServletRequest" %>
<%@ page import="jakarta.servlet.http.HttpSession" %>
<%@ page import="org.exoplatform.web.application.RequestContext" %>
<%@ page import="io.meeds.wallet.utils.WalletUtils" %>
<%
  HttpServletRequest httpRequest = PortalRequestContext.getCurrentInstance().getRequest();
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