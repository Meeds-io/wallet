package org.exoplatform.wallet.blockchain.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.javascript.jscomp.jarjar.com.google.common.base.Objects;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.wallet.model.ContractTransactionEvent;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.service.WalletAccountService;
import org.exoplatform.wallet.service.WalletTransactionService;

@RunWith(MockitoJUnitRunner.class)
public class EthereumBlockchainTransactionServiceTest {

  @Mock
  private WalletAccountService                 accountService;

  @Mock
  private WalletTransactionService             walletTransactionService;

  @Mock
  private ListenerService                      listenerService;

  @Mock
  private SettingService                       settingService;

  @Mock
  private EthereumClientConnector              ethereumClientConnector;

  private EthereumBlockchainTransactionService service;

  @Before
  public void setUp() {
    service = new EthereumBlockchainTransactionService(settingService,
                                                       ethereumClientConnector,
                                                       walletTransactionService,
                                                       accountService,
                                                       listenerService);
  }

  @Test
  public void testOnServiceStartWhenNotPermanentListeningAndNoPendingTransactions() throws Exception {
    long blockNumber = 2559l;
    when(ethereumClientConnector.getLastestBlockNumber()).thenReturn(blockNumber);

    service.start();
    verify(settingService, times(1)).set(any(), any(), any(), argThat(value -> Objects.equal(value.getValue(), blockNumber)));
    verify(ethereumClientConnector, times(1)).setLastWatchedBlockNumber(blockNumber);
    verify(ethereumClientConnector, times(0)).renewTransactionListeningSubscription(anyLong());
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  public void testOnServiceStartWhenPermanentListening() throws Exception {
    long blockNumber = 2559l;
    SettingValue value = SettingValue.create(blockNumber);
    when(settingService.get(any(), any(), any())).thenReturn(value);
    when(ethereumClientConnector.isPermanentlyScanBlockchain()).thenReturn(true);

    service.start();

    verify(settingService, times(0)).set(any(), any(), any(), any());
    verify(ethereumClientConnector, times(1)).setLastWatchedBlockNumber(blockNumber);
    verify(ethereumClientConnector, times(1)).renewTransactionListeningSubscription(anyLong());
  }

  @Test
  public void testOnServiceStartWhenNotPermanentListeningButHavePendingTransactions() throws Exception {
    when(walletTransactionService.countPendingTransactions()).thenReturn(1);

    service.start();

    verify(ethereumClientConnector, times(1)).renewTransactionListeningSubscription(anyLong());
  }

  @Test
  public void testHasManagedWalletInTransactionWhenNoTopics() throws Exception {
    ContractTransactionEvent contractTransactionEvent = newContractTransactionEvent();
    contractTransactionEvent.setTopics(Collections.emptyList());
    assertFalse(service.hasManagedWalletInTransaction(contractTransactionEvent));
  }

  @Test
  public void testHasManagedWalletInTransactionWhenNoWalletsAndHavingTopics() throws Exception {
    assertFalse(service.hasManagedWalletInTransaction(newContractTransactionEvent()));
  }

  @Test
  public void testHasManagedWalletInTransactionWhenKnownSenderWallet() throws Exception {
    String fromAddress = "0x2b7e115f52171d164529fdb1ac72571e608a474e";

    when(accountService.getWalletByAddress(argThat(address -> StringUtils.equalsIgnoreCase(fromAddress,
                                                                                           address)))).thenReturn(new Wallet());

    assertTrue(service.hasManagedWalletInTransaction(newContractTransactionEvent()));
  }

  @Test
  public void testHasManagedWalletInTransactionWhenKnownReceiverWallet() throws Exception {
    String toAddress = "0x1d94f732223996e9f773261e82340889934a6c03";

    when(accountService.getWalletByAddress(argThat(address -> StringUtils.equalsIgnoreCase(toAddress,
                                                                                           address)))).thenReturn(new Wallet());

    assertTrue(service.hasManagedWalletInTransaction(newContractTransactionEvent()));
  }

  @Test
  public void testRefreshTransactionFromBlockchainWhenTransactionNotFoundOnBlockchainNorDB() throws Exception {
    assertThrows(IllegalStateException.class, () -> service.refreshTransactionFromBlockchain("transactionHash"));
  }

  private ContractTransactionEvent newContractTransactionEvent() {
    String hash = "transactionHash";
    String contractAddress = "0x334d85047da64738c065d36e10b2adeb965000d0";
    String data = "0x0000000000000000000000000000000000000000000000001bc16d674ec80000";
    List<String> topics = Arrays.asList("0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
                                        "0x0000000000000000000000002b7e115f52171d164529fdb1ac72571e608a474e",
                                        "0x0000000000000000000000001d94f732223996e9f773261e82340889934a6c03");
    long blockNumber = 28047704;
    ContractTransactionEvent contractTransactionEvent = new ContractTransactionEvent(hash,
                                                                                     contractAddress,
                                                                                     data,
                                                                                     topics,
                                                                                     blockNumber);
    return contractTransactionEvent;
  }

}
