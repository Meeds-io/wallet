package org.exoplatform.addon.wallet.blockchain.listener;

import static org.exoplatform.addon.wallet.statistic.StatisticUtils.OPERATION;
import static org.exoplatform.addon.wallet.utils.WalletUtils.*;

import java.util.*;
import java.util.stream.Collectors;

import org.web3j.protocol.core.methods.response.EthBlock.Block;
import org.web3j.protocol.core.methods.response.EthBlock.TransactionResult;

import org.exoplatform.addon.wallet.model.transaction.TransactionDetail;
import org.exoplatform.addon.wallet.service.BlockchainTransactionService;
import org.exoplatform.addon.wallet.service.WalletTransactionService;
import org.exoplatform.addon.wallet.statistic.ExoWalletStatistic;
import org.exoplatform.addon.wallet.statistic.ExoWalletStatisticService;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.listener.*;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

@Asynchronous
public class BlockMinedListener extends Listener<Block, Boolean> implements ExoWalletStatisticService {
  private static final Log             LOG = ExoLogger.getLogger(BlockMinedListener.class);

  private ExoContainer                 container;

  private WalletTransactionService     transactionService;

  private BlockchainTransactionService blockchainTransactionService;

  public BlockMinedListener(ExoContainer container,
                            WalletTransactionService transactionService,
                            BlockchainTransactionService blockchainTransactionService) {
    this.container = container;
    this.transactionService = transactionService;
    this.blockchainTransactionService = blockchainTransactionService;
  }

  @Override
  @ExoWalletStatistic(local = false, service = "wallet", operation = OPERATION_GET_BLOCK)
  public void onEvent(Event<Block, Boolean> event) throws Exception {
    Block block = event.getSource();
    long blockNumber = block.getNumber().longValue();

    ExoContainerContext.setCurrentContainer(this.container);
    RequestLifeCycle.begin(this.container);
    try {
      List<TransactionDetail> pendingTransactions = transactionService.getPendingTransactions();

      if (pendingTransactions.isEmpty()) {
        LOG.debug("No pending transaction to check for block '{}'", blockNumber);
      } else {
        LOG.debug("Checking on blockchain the status of {} transactions marked as pending in database in block {}",
                  pendingTransactions.size(),
                  blockNumber);

        @SuppressWarnings("rawtypes")
        List<TransactionResult> transactions = block.getTransactions();
        Set<String> minedTransactionHashes = transactions.stream()
                                                         .map(tx -> tx.get().toString().toLowerCase())
                                                         .collect(Collectors.toSet());
        Set<String> pendingTransactionHashes = pendingTransactions.stream()
                                                                  .map(TransactionDetail::getHash)
                                                                  .collect(Collectors.toSet());
        minedTransactionHashes.retainAll(pendingTransactionHashes);
        if (!minedTransactionHashes.isEmpty()) {
          for (String hash : minedTransactionHashes) {
            try { // NOSONAR
              // Transaction has been mined, thus, we should update its status
              // in database by retrieving its data from blockchain for more
              // integrity
              blockchainTransactionService.checkTransactionStatusOnBlockchain(hash, true);
            } catch (Exception e) {
              LOG.warn("Error checking mined transaction on blockchain: {}", hash, e);
            }
          }
        }
      }
    } catch (Exception e) {
      LOG.error("Error while checking pending transactions on mined block '{}'", blockNumber, e);
    } finally {
      RequestLifeCycle.end();
    }
  }

  @Override
  public Map<String, Object> getStatisticParameters(String operation, Object result, Object... methodArgs) {
    Map<String, Object> parameters = new HashMap<>();

    @SuppressWarnings("unchecked")
    Event<Block, Boolean> event = (Event<Block, Boolean>) methodArgs[0];

    Block block = event.getSource();
    boolean newBlock = event.getData();
    if (newBlock) {
      parameters.put(OPERATION, OPERATION_GET_BLOCK_BY_HASH);
    } else {
      parameters.put(OPERATION, OPERATION_GET_BLOCK_BY_NUMBER);
    }
    parameters.put("blockchain_network_id", getNetworkId());
    parameters.put("blockchain_network_url_suffix", getBlockchainURLSuffix());
    parameters.put("block_hash", block.getHash());
    parameters.put("block_number", block.getNumber().longValue());
    return parameters;
  }

}
