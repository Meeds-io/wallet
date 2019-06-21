/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.addon.wallet.external;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.*;

import org.apache.commons.lang3.StringUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.websocket.*;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.exoplatform.addon.wallet.model.settings.GlobalSettings;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * A Web3j connector class to interact with Ethereum Blockchain
 */
public class EthereumClientConnectorForTransaction {

  private static final Log         LOG                        = ExoLogger.getLogger(EthereumClientConnectorForTransaction.class);

  private Web3j                    web3j                      = null;

  private WebSocketClient          webSocketClient            = null;

  private WebSocketService         web3jService               = null;

  private GlobalSettings           globalSettings             = null;

  private ScheduledExecutorService connectionVerifierExecutor = null;

  private boolean                  connectionInProgress       = false;

  private boolean                  serviceStarted             = false;

  private boolean                  serviceStopping            = false;

  private ClassLoader              contextCL;

  public EthereumClientConnectorForTransaction(ClassLoader contextCL) {
    ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("Ethereum-websocket-connector-%d").build();
    connectionVerifierExecutor = Executors.newSingleThreadScheduledExecutor(namedThreadFactory);
    this.contextCL = contextCL;
  }

  public void start(GlobalSettings storedSettings) {
    this.globalSettings = storedSettings;
    this.serviceStarted = true;

    // Blockchain connection verifier
    connectionVerifierExecutor.scheduleWithFixedDelay(() -> {
      Thread.currentThread().setContextClassLoader(contextCL);
      try {
        if (!initWeb3Connection()) {
          return;
        }
      } catch (Throwable e) {
        LOG.warn("Error while checking connection status to Etherreum Websocket endpoint: {}", e);
        return;
      }
    }, 5, 10, TimeUnit.SECONDS);
  }

  public void stop() {
    this.serviceStopping = true;
    connectionVerifierExecutor.shutdownNow();
    resetConnection();
  }

  /**
   * @return true if the connection to the blockchain is established
   */
  public boolean isConnected() {
    return web3j != null && web3jService != null && webSocketClient != null && webSocketClient.isOpen();
  }

  public Web3j getWeb3j() throws InterruptedException {
    this.waitConnection();
    return web3j;
  }

  public void waitConnection() throws InterruptedException {
    Thread.currentThread().setContextClassLoader(contextCL);
    if (this.serviceStarted && StringUtils.isBlank(getWebsocketProviderURL())) {
      throw new IllegalStateException("No websocket connection is configured for ethereum blockchain");
    }
    if (this.serviceStopping) {
      throw new IllegalStateException("Server is stopping, thus no Web3 request should be emitted");
    }
    while (!isConnected()) {
      if (this.serviceStarted && StringUtils.isBlank(getWebsocketProviderURL())) {
        throw new IllegalStateException("No websocket connection is configured for ethereum blockchain");
      }
      if (this.serviceStopping) {
        throw new IllegalStateException("Server is stopping, thus no Web3 request should be emitted");
      }
      LOG.info("Wait until Websocket connection to blockchain is established to retrieve information");
      Thread.sleep(5000);
    }
  }

  private String getWebsocketProviderURL() {
    return globalSettings == null ? null : globalSettings.getNetwork().getWebsocketProviderURL();
  }

  private void resetConnection() {
    if (web3j != null) {
      LOG.info("Resetting blockchain connection");
      try {
        web3j.shutdown();
      } catch (Throwable e) {
        LOG.warn("Error closing old web3j connection: {}", e.getMessage());
        if (this.web3jService != null && webSocketClient != null && webSocketClient.isOpen()) {
          try {
            web3jService.close();
          } catch (Throwable e1) {
            LOG.warn("Error closing old websocket connection: {}", e1.getMessage());
          }
        } else if (webSocketClient != null && webSocketClient.isOpen()) {
          try {
            webSocketClient.close();
          } catch (Throwable e1) {
            LOG.warn("Error closing old websocket connection: {}", e1.getMessage());
          }
        }
      }
      web3j = null;
      web3jService = null;
      webSocketClient = null;
    }
  }

  private boolean initWeb3Connection() throws Exception { // NOSONAR
    if (this.connectionInProgress) {
      LOG.info("Web3 connection initialization in progress, skip transaction processing until it's initialized");
      return false;
    }
    if (this.serviceStopping) {
      LOG.info("Stopping server, thus no new connection is attempted again");
      return false;
    }
    String websocketProviderURL = getWebsocketProviderURL();
    if (StringUtils.isBlank(websocketProviderURL)) {
      LOG.info("No configured URL for Ethereum Websocket connection");
      resetConnection();
      return false;
    }
    if (!websocketProviderURL.startsWith("ws:") && !websocketProviderURL.startsWith("wss:")) {
      LOG.warn("Bad format for configured URL " + websocketProviderURL + " for Ethereum Websocket connection");
      resetConnection();
      return false;
    }
    if (isConnected()) {
      return false;
    }

    this.connectionInProgress = true;
    try {
      if (this.web3j != null && this.web3jService != null && this.webSocketClient != null) {
        LOG.info("Reconnect to blockchain endpoint {}", getWebsocketProviderURL());
        boolean reconnected = this.webSocketClient.reconnectBlocking();
        if (!reconnected) {
          throw new IllegalStateException("Can't reconnect to ethereum blockchain: " + getWebsocketProviderURL());
        }
      } else {
        LOG.info("Connecting to Ethereum network endpoint {}", getWebsocketProviderURL());
        this.webSocketClient = new WebSocketClient(new URI(getWebsocketProviderURL()));
        this.webSocketClient.setConnectionLostTimeout(10);
        this.web3jService = new WebSocketService(webSocketClient, true);
        this.webSocketClient.setListener(getWebsocketListener());
        this.web3jService.connect();
        Thread.sleep(10000);
        web3j = Web3j.build(web3jService);
        LOG.info("Connection established to Ethereum network endpoint {}", getWebsocketProviderURL());
      }
      return true;
    } finally {
      this.connectionInProgress = false;
    }
  }

  private WebSocketListener getWebsocketListener() {
    return new WebSocketListener() {
      @Override
      public void onMessage(String message) throws IOException {
        LOG.debug("A new message is received in testConnection method");
      }

      @Override
      public void onError(Exception e) {
        LOG.warn(getConnectionFailedMessage());
      }

      @Override
      public void onClose() {
        LOG.debug("Websocket connection closed for testConnection method");
      }
    };
  }

  private String getConnectionFailedMessage() {
    return "Connection failed to " + getWebsocketProviderURL();
  }

}
