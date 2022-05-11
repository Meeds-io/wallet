<!--
This file is part of the Meeds project (https://meeds.io/).
Copyright (C) 2020 Meeds Association
contact@meeds.io
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3 of the License, or (at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.
You should have received a copy of the GNU Lesser General Public License
along with this program; if not, write to the Free Software Foundation,
Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
-->
<%@ page import="javax.servlet.http.HttpServletRequest" %>
<%@ page import="javax.servlet.http.HttpSession" %>
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