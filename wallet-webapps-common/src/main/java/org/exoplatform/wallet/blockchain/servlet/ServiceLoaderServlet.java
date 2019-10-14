package org.exoplatform.wallet.blockchain.servlet;

import static org.exoplatform.wallet.utils.WalletUtils.*;

import java.io.IOException;
import java.security.Provider;
import java.security.Security;
import java.util.concurrent.*;

import javax.servlet.ServletException;
import javax.servlet.http.*;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.scheduler.CronJob;
import org.exoplatform.services.scheduler.JobSchedulerService;
import org.exoplatform.wallet.blockchain.listener.*;
import org.exoplatform.wallet.blockchain.service.*;
import org.exoplatform.wallet.job.*;
import org.exoplatform.wallet.listener.TransactionNotificationListener;
import org.exoplatform.wallet.model.settings.GlobalSettings;
import org.exoplatform.wallet.service.*;

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
    executor.scheduleAtFixedRate(this::instantiateBlockchainServices, 10, 10, TimeUnit.SECONDS);
  }

  private void instantiateBlockchainServices() {
    PortalContainer container = PortalContainer.getInstance();
    if (container == null || !container.isStarted()) {
      LOG.debug("Portal Container is not yet started");
      return;
    }
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(container);
    try {

      GlobalSettings settings = getSettings();
      if (settings == null) {
        LOG.warn("No wallet settings are found");
        return;
      }

      if (settings.getNetwork() == null || settings.getNetwork().getId() <= 0
          || StringUtils.isBlank(settings.getNetwork().getProviderURL())
          || StringUtils.isBlank(settings.getNetwork().getWebsocketProviderURL())) {
        LOG.warn("No valid blockchain network settings are found: {}", settings.getNetwork());
        return;
      }

      if (StringUtils.isBlank(settings.getContractAddress())) {
        LOG.warn("No contract address is configured");
        return;
      }

      // Replace old bouncy castle provider by the newer version
      ClassLoader webappClassLoader = getWebappClassLoader();
      Class<?> class1 = webappClassLoader.loadClass(BouncyCastleProvider.class.getName());
      Provider provider = (Provider) class1.newInstance();
      Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
      Security.addProvider(provider);
      provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
      LOG.info("BouncyCastleProvider class registered with version {}",
               provider.getVersion());

      // start connection to blockchain
      EthereumClientConnector web3jConnector = new EthereumClientConnector();
      container.registerComponentInstance(EthereumClientConnector.class, web3jConnector);
      web3jConnector.start(true);

      // Blockchain transaction decoder
      EthereumBlockchainTransactionService transactionDecoderService = new EthereumBlockchainTransactionService(web3jConnector);
      container.registerComponentInstance(BlockchainTransactionService.class,
                                          transactionDecoderService);

      // Instantiate service with current webapp classloader
      EthereumWalletTokenAdminService tokenAdminService = new EthereumWalletTokenAdminService(web3jConnector);
      container.registerComponentInstance(WalletTokenAdminService.class, tokenAdminService);

      ListenerService listernerService = CommonsUtils.getService(ListenerService.class);

      // Start listening on blockchain modification for websocket trigger
      listernerService.addListener(NEW_BLOCK_MINED_EVENT,
                                   new BlockMinedListener(container,
                                                          CommonsUtils.getService(WalletTransactionService.class),
                                                          transactionDecoderService));
      listernerService.addListener(KNOWN_TRANSACTION_MINED_EVENT, new TransactionMinedListener());
      listernerService.addListener(TRANSACTION_MODIFIED_EVENT, new TransactionNotificationListener(container));
      listernerService.addListener(TRANSACTION_MODIFIED_EVENT, new WebSocketTransactionListener());
      listernerService.addListener(WALLET_MODIFIED_EVENT, new WebSocketWalletListener());
      listernerService.addListener(CONTRACT_MODIFIED_EVENT, new WebSocketContractListener());

      // Start services after adding listeners
      tokenAdminService.start();
      transactionDecoderService.start();

      addBlockchainScheduledJob(ContractTransactionVerifierJob.class,
                                "Add a job to verify if mined contract transactions are added in database",
                                "0 0/5 * ? * * *");
      addBlockchainScheduledJob(TransactionSenderJob.class,
                                "Configuration for transaction sending to blockchain",
                                "0/30 * * * * ?");
      addBlockchainScheduledJob(PendingTransactionVerifierJob.class,
                                "Configuration for pending transactions check on blockchain",
                                "0 15 7 * * ?");

      WalletService walletService = container.getComponentInstanceOfType(WalletService.class);
      if (walletService.isUseDynamicGasPrice()) {
        addBlockchainScheduledJob(GasPriceUpdaterJob.class,
                                  "A job to update gas price from blockchain",
                                  "0 0/2 * * * ?");
      }

      LOG.debug("Blockchain Service instances created");
    } catch (Throwable e) {
      LOG.error("Error registering service into portal container", e);
    } finally {
      RequestLifeCycle.end();
    }
    executor.shutdown();
  }

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    this.init();
    super.service(req, resp);
  }

  private void addBlockchainScheduledJob(Class<?> jobClass, String description, String defaultCronExpression) throws Exception {
    JobSchedulerService schedulerService = CommonsUtils.getService(JobSchedulerService.class);
    String jobSimpleName = jobClass.getSimpleName();
    InitParams params = new InitParams();
    PropertiesParam propertiesParam = new PropertiesParam();
    propertiesParam.setName("cronjob.info");
    propertiesParam.setDescription(description);
    propertiesParam.setProperty("jobName", jobSimpleName);
    propertiesParam.setProperty("groupName", "Wallet");
    propertiesParam.setProperty("job", jobClass.getName());
    propertiesParam.setProperty("expression",
                                System.getProperty("exo.wallet." + jobSimpleName + ".expression", defaultCronExpression));
    params.addParam(propertiesParam);
    CronJob cronJob = new CronJob(params);
    schedulerService.addCronJob(cronJob);
  }

  private ClassLoader getWebappClassLoader() {
    return getServletContext().getClassLoader();
  }
}
