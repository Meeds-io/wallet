package org.exoplatform.addon.wallet.blockchain.listener;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.web3j.protocol.core.methods.response.EthBlock.Block;
import org.web3j.protocol.core.methods.response.EthBlock.TransactionResult;

import org.exoplatform.addon.wallet.service.BlockchainTransactionService;
import org.exoplatform.addon.wallet.service.WalletTransactionService;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.listener.*;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

@Asynchronous
public class BlockMinedListener extends Listener<Block, Object> {
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
  public void onEvent(Event<Block, Object> event) throws Exception {
    Block block = event.getSource();
    long blockNumber = block.getNumber().longValue();

    ExoContainerContext.setCurrentContainer(this.container);
    RequestLifeCycle.begin(this.container);
    try {
      List<String> pendingTransactionsHashes = transactionService.getPendingTransactionHashes();

      if (pendingTransactionsHashes.isEmpty()) {
        LOG.debug("No pending transaction to check for block '{}'", blockNumber);
      } else {
        LOG.debug("Checking on blockchain the status of {} transactions marked as pending in database in block {}",
                  pendingTransactionsHashes.size(),
                  blockNumber);

        @SuppressWarnings("rawtypes")
        List<TransactionResult> transactions = block.getTransactions();
        Set<String> minedTransactionHashes = transactions.stream()
                                                         .map(tx -> tx.get().toString().toLowerCase())
                                                         .collect(Collectors.toSet());
        minedTransactionHashes.retainAll(pendingTransactionsHashes);
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

}
