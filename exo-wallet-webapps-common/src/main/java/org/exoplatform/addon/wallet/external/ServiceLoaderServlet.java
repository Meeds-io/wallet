package org.exoplatform.addon.wallet.external;

import java.io.IOException;
import java.security.Provider;
import java.security.Security;
import java.util.concurrent.*;

import javax.servlet.ServletException;
import javax.servlet.http.*;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.exoplatform.addon.wallet.service.WalletService;
import org.exoplatform.addon.wallet.service.WalletTokenAdminService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * A Servlet added to replace old bouncy castle provider loaded in parent class
 * loader by a new one that defines more algorithms and a newer implementation.
 * Workaround for PLF-8123
 */
public class ServiceLoaderServlet extends HttpServlet {

  private static final long                     serialVersionUID = 4629318431709644350L;

  private static final Log                      LOG              = ExoLogger.getLogger(ServiceLoaderServlet.class);

  private static final ScheduledExecutorService executor         = Executors.newScheduledThreadPool(1);

  @Override
  public void init() throws ServletException {
    executor.scheduleAtFixedRate(() -> {
      PortalContainer container = PortalContainer.getInstance();
      if (container == null || !container.isStarted()) {
        LOG.debug("Portal Container is not yet started");
        return;
      }

      Thread currentThread = Thread.currentThread();
      ClassLoader currentClassLoader = currentThread.getContextClassLoader();
      ClassLoader contextCL = getServletContext().getClassLoader();
      currentThread.setContextClassLoader(contextCL);
      try {
        // Replace old bouncy castle provider by the newer version
        Class<?> class1 = contextCL.loadClass(BouncyCastleProvider.class.getName());
        Provider provider = (Provider) class1.newInstance();
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        Security.addProvider(provider);
        provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
        LOG.info("BouncyCastleProvider class registered with version {}",
                 provider.getVersion());

        // Instantiate service with current webapp classloader
        WalletService walletservice = container.getComponentInstanceOfType(WalletService.class);
        EthereumClientConnectorForTransaction web3jConnector = new EthereumClientConnectorForTransaction(contextCL);
        web3jConnector.start(walletservice.getSettings());
        EthereumWalletTokenAdminService service = new EthereumWalletTokenAdminService(web3jConnector, contextCL);
        container.registerComponentInstance(WalletTokenAdminService.class,
                                            service);
        LOG.debug("EthereumWalletTokenAdminService instance created.");

        ListenerService listenerService = CommonsUtils.getService(ListenerService.class);
        listenerService.addListener("exo.addon.wallet.settings.changed", new GlobalSettingsModificationListener(web3jConnector));
      } catch (Exception e) {
        LOG.warn("Can't create service with class EthereumWalletTokenAdminService", e);
      } finally {
        currentThread.setContextClassLoader(currentClassLoader);
      }
      executor.shutdown();
    }, 10, 10, TimeUnit.SECONDS);
  }

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    this.init();
    super.service(req, resp);
  }
}
