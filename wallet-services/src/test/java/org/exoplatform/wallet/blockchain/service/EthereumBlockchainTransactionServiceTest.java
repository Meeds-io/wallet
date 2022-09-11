package org.exoplatform.wallet.blockchain.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import com.google.javascript.jscomp.jarjar.com.google.common.base.Objects;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.wallet.contract.MeedsToken;
import org.exoplatform.wallet.model.ContractTransactionEvent;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.WalletAccountService;
import org.exoplatform.wallet.service.WalletTransactionService;
import org.exoplatform.wallet.test.BaseWalletTest;
import org.exoplatform.wallet.utils.WalletUtils;

@RunWith(MockitoJUnitRunner.class)
public class EthereumBlockchainTransactionServiceTest extends BaseWalletTest {

  @Mock
  private WalletAccountService                 accountService;

  @Mock
  private WalletTransactionService             transactionService;

  @Mock
  private ListenerService                      listenerService;

  @Mock
  private SettingService                       settingService;

  @Mock
  private EthereumClientConnector              ethereumClientConnector;

  private EthereumBlockchainTransactionService service;

  @Before
  public void setUp() {
    service = new EthereumBlockchainTransactionService(container,
                                                       null,
                                                       settingService,
                                                       ethereumClientConnector,
                                                       transactionService,
                                                       accountService,
                                                       listenerService);
  }

  @Test
  public void testOnServiceStartWhenNotPermanentListeningAndNoPendingTransactions() throws Exception {
    long blockNumber = 2559l;
    when(ethereumClientConnector.getLastestBlockNumber()).thenReturn(blockNumber);

    service.startAsync();
    verify(ethereumClientConnector, timeout(10000).times(1)).setLastWatchedBlockNumber(anyLong());

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

    service.startAsync();
    verify(ethereumClientConnector, timeout(10000).times(1)).setLastWatchedBlockNumber(anyLong());

    verify(settingService, times(0)).set(any(), any(), any(), any());
    verify(ethereumClientConnector, times(1)).setLastWatchedBlockNumber(blockNumber);
    verify(ethereumClientConnector, times(1)).renewTransactionListeningSubscription(anyLong());
  }

  @Test
  public void testOnServiceStartWhenNotPermanentListeningButHavePendingTransactions() throws Exception {
    when(transactionService.countContractPendingTransactionsSent()).thenReturn(1);

    service.startAsync();
    verify(ethereumClientConnector, timeout(10000).times(1)).renewTransactionListeningSubscription(anyLong());
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

  @Test
  public void testRefreshTransactionFromBlockchainWhenTransactionFoundOnBlockchainButDB() throws Exception {
    String transactionHash = "transactionHash";
    Transaction transaction = mock(Transaction.class);
    when(ethereumClientConnector.getTransaction(transactionHash)).thenReturn(transaction);

    when(transaction.getHash()).thenReturn(transactionHash);
    assertThrows(IllegalStateException.class, () -> service.refreshTransactionFromBlockchain(transactionHash));

    when(transaction.getBlockHash()).thenReturn(WalletUtils.EMPTY_HASH);
    assertThrows(IllegalStateException.class, () -> service.refreshTransactionFromBlockchain(transactionHash));

    when(transaction.getBlockHash()).thenReturn("blockHash");
    when(transaction.getBlockNumber()).thenReturn(BigInteger.TEN);
    service.refreshTransactionFromBlockchain(transactionHash);
    verify(ethereumClientConnector, times(0)).getTransactionReceipt(any());
  }

  @Test
  public void testRefreshTransactionFromBlockchainWhenTransactionFoundOnDBButBlockchain_NoTimeoutNoSendingAttempt() throws Exception {
    String transactionHash = "transactionHash";
    TransactionDetail transactionDetail = new TransactionDetail();
    when(transactionService.getTransactionByHash(transactionHash)).thenReturn(transactionDetail);

    transactionDetail.setHash(transactionHash);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setPending(true);
    transactionDetail.setRawTransaction(null);
    transactionDetail.setNonce(0);
    transactionDetail.setSendingAttemptCount(0);

    when(transactionService.getPendingTransactionMaxDays()).thenReturn(1l);
    when(transactionService.getMaxAttemptsToSend()).thenReturn(1l);

    service.refreshTransactionFromBlockchain(transactionHash);
    verify(transactionService, times(0)).saveTransactionDetail(any(), anyBoolean());
  }

  @Test
  public void testRefreshTransactionFromBlockchainWhenTransactionFoundOnDBButBlockchain_WithTimeoutNoSendingAttemptMaxReached() throws Exception {
    String transactionHash = "transactionHash";
    TransactionDetail transactionDetail = new TransactionDetail();
    when(transactionService.getTransactionByHash(transactionHash)).thenReturn(transactionDetail);

    transactionDetail.setHash(transactionHash);
    transactionDetail.setTimestamp(System.currentTimeMillis() - 24 * 3600000l - 1);
    transactionDetail.setPending(true);
    transactionDetail.setRawTransaction(null);
    transactionDetail.setNonce(25l);
    transactionDetail.setSendingAttemptCount(0);

    when(transactionService.getPendingTransactionMaxDays()).thenReturn(1l);
    when(transactionService.getMaxAttemptsToSend()).thenReturn(1l);

    service.refreshTransactionFromBlockchain(transactionHash);
    verify(transactionService, times(1)).saveTransactionDetail(
                                                               argThat(transaction -> !transaction.isPending()
                                                                   && !transaction.isSucceeded() && transaction.getNonce() == 0),
                                                               eq(true));
  }

  @Test
  public void testRefreshTransactionFromBlockchainWhenTransactionFoundOnDBButBlockchain_NoTimeoutWithSendingAttemptMaxReached() throws Exception {
    String transactionHash = "transactionHash";
    TransactionDetail transactionDetail = new TransactionDetail();
    when(transactionService.getTransactionByHash(transactionHash)).thenReturn(transactionDetail);

    transactionDetail.setHash(transactionHash);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setSentTimestamp(System.currentTimeMillis());
    transactionDetail.setPending(true);
    transactionDetail.setRawTransaction("rawTransaction");
    transactionDetail.setNonce(25l);
    transactionDetail.setSendingAttemptCount(1);

    when(transactionService.getPendingTransactionMaxDays()).thenReturn(1l);
    when(transactionService.getMaxAttemptsToSend()).thenReturn(transactionDetail.getSendingAttemptCount());

    service.refreshTransactionFromBlockchain(transactionHash);
    verify(transactionService, times(0)).saveTransactionDetail(any(), anyBoolean());
  }

  @Test
  public void testRefreshTransactionFromBlockchainWhenTransactionFoundOnDBButBlockchain_NoTimeoutWithSendingAttemptMaxAndInvalidNonce() throws Exception {
    String transactionHash = "transactionHash";
    String fromAddress = "fromAddress";
    long nonce = 25l;

    TransactionDetail transactionDetail = new TransactionDetail();
    when(transactionService.getTransactionByHash(transactionHash)).thenReturn(transactionDetail);

    transactionDetail.setHash(transactionHash);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setSentTimestamp(System.currentTimeMillis());
    transactionDetail.setPending(true);
    transactionDetail.setRawTransaction("rawTransaction");
    transactionDetail.setNonce(nonce);
    transactionDetail.setSendingAttemptCount(0);
    transactionDetail.setFrom(fromAddress);

    when(transactionService.getPendingTransactionMaxDays()).thenReturn(1l);
    when(transactionService.getMaxAttemptsToSend()).thenReturn(3l);
    when(ethereumClientConnector.getNonce(fromAddress)).thenReturn(BigInteger.valueOf(nonce + 1));

    service.refreshTransactionFromBlockchain(transactionHash);
    verify(transactionService, times(0)).saveTransactionDetail(any(), anyBoolean());

    transactionDetail.setSendingAttemptCount(transactionService.getMaxAttemptsToSend());
    service.refreshTransactionFromBlockchain(transactionHash);
    verify(transactionService, times(0)).saveTransactionDetail(any(), anyBoolean());

    when(ethereumClientConnector.getNonce(fromAddress)).thenReturn(BigInteger.valueOf(nonce + 2));
    service.refreshTransactionFromBlockchain(transactionHash);
    verify(transactionService, times(1)).saveTransactionDetail(
                                                               argThat(transaction -> !transaction.isPending()
                                                                   && !transaction.isSucceeded() && transaction.getNonce() == 0),
                                                               eq(true));
  }

  @Test
  public void testRefreshTransactionFromBlockchainWhenTransactionFoundOnDBAndBlockchain_EtherTransaction() throws Exception {
    String transactionHash = "transactionHash";
    String fromAddress = "fromAddress";
    String toAddress = "toAddress";
    long nonce = 25l;
    BigInteger transactionNonce = BigInteger.valueOf(nonce);

    TransactionDetail transactionDetail = new TransactionDetail();
    when(transactionService.getTransactionByHash(transactionHash)).thenReturn(transactionDetail);

    Transaction transaction = mock(Transaction.class);
    when(ethereumClientConnector.getTransaction(transactionHash)).thenReturn(transaction);
    when(transaction.getHash()).thenReturn(transactionHash);
    when(transaction.getBlockHash()).thenReturn("blockHash");
    when(transaction.getBlockNumber()).thenReturn(BigInteger.TEN);

    assertThrows(IllegalStateException.class, () -> service.refreshTransactionFromBlockchain(transactionHash));

    transactionDetail.setHash(transactionHash);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setSentTimestamp(System.currentTimeMillis());
    transactionDetail.setPending(true);
    transactionDetail.setRawTransaction("rawTransaction");
    transactionDetail.setNonce(nonce);
    transactionDetail.setSendingAttemptCount(1);
    transactionDetail.setFrom(fromAddress);

    assertThrows(IllegalStateException.class, () -> service.refreshTransactionFromBlockchain(transactionHash));
    verify(transactionService, times(0)).saveTransactionDetail(any(), anyBoolean());

    TransactionReceipt transactionReceipt = mock(TransactionReceipt.class);
    when(ethereumClientConnector.getTransactionReceipt(transactionHash)).thenReturn(transactionReceipt);
    BigInteger gasUsed = BigInteger.ONE;
    BigInteger gasPrice = BigInteger.TWO;
    BigInteger etherValue = BigInteger.valueOf(4);

    when(transactionReceipt.getGasUsed()).thenReturn(gasUsed);
    when(transaction.getGasPrice()).thenReturn(gasPrice);
    when(transaction.getNonce()).thenReturn(transactionNonce);
    when(transaction.getValue()).thenReturn(etherValue);
    when(transaction.getFrom()).thenReturn(fromAddress);
    when(transaction.getTo()).thenReturn(toAddress);

    service.refreshTransactionFromBlockchain(transactionHash);
    verify(transactionService, times(1)).saveTransactionDetail(argThat(transactionTmp -> {
      assertFalse(transactionTmp.isPending());
      assertFalse(transactionTmp.isSucceeded());
      assertTrue(StringUtils.isBlank(transactionTmp.getContractAddress()));
      assertTrue(StringUtils.isBlank(transactionTmp.getContractMethodName()));
      assertEquals(0, transactionTmp.getContractAmount(), 0);
      assertEquals(gasPrice.doubleValue(), transactionTmp.getGasPrice(), 0);
      assertEquals(gasUsed.intValue(), transactionTmp.getGasUsed());
      assertEquals(transactionNonce.longValue(), transactionTmp.getNonce());
      assertEquals(toAddress, transactionTmp.getTo());
      assertEquals(fromAddress, transactionTmp.getFrom());
      assertEquals(WalletUtils.convertFromDecimals(etherValue, WalletUtils.ETHER_TO_WEI_DECIMALS), transactionTmp.getValue(), 0);
      return true;
    }), eq(true));

    verify(transactionService, times(0)).cancelTransactionsWithSameNonce(any());
    when(transactionReceipt.isStatusOK()).thenReturn(true);
    service.refreshTransactionFromBlockchain(transactionHash);
    verify(transactionService, times(1)).cancelTransactionsWithSameNonce(transactionDetail);
  }

  @Test
  public void testRefreshTransactionFromBlockchainWhenTransactionFoundOnDBAndBlockchain_ContractTransaction() throws Exception {
    ContractTransactionEvent contractTransactionEvent = newContractTransactionEvent();
    ;
    String transactionHash = contractTransactionEvent.getTransactionHash();
    String fromAddress = "0x2b7e115f52171d164529fdb1ac72571e608a474e";
    long nonce = 25l;
    BigInteger transactionNonce = BigInteger.valueOf(nonce);
    String contractAddress = WalletUtils.getContractAddress();

    TransactionDetail transactionDetail = new TransactionDetail();
    when(transactionService.getTransactionByHash(transactionHash)).thenReturn(transactionDetail);

    Transaction transaction = mock(Transaction.class);
    when(ethereumClientConnector.getTransaction(transactionHash)).thenReturn(transaction);

    TransactionReceipt transactionReceipt = mock(TransactionReceipt.class);
    when(ethereumClientConnector.getTransactionReceipt(transactionHash)).thenReturn(transactionReceipt);

    org.web3j.protocol.core.methods.response.Log log = mock(org.web3j.protocol.core.methods.response.Log.class);
    when(log.getTopics()).thenReturn(contractTransactionEvent.getTopics());
    when(log.getData()).thenReturn(contractTransactionEvent.getData());

    BigInteger gasUsed = BigInteger.ONE;
    BigInteger gasPrice = BigInteger.TWO;
    BigInteger etherValue = BigInteger.valueOf(4);

    transactionDetail.setHash(transactionHash);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setSentTimestamp(System.currentTimeMillis());
    transactionDetail.setPending(true);
    transactionDetail.setRawTransaction("rawTransaction");
    transactionDetail.setNonce(nonce);
    transactionDetail.setSendingAttemptCount(1);
    transactionDetail.setFrom(fromAddress);

    when(transactionReceipt.getGasUsed()).thenReturn(gasUsed);
    when(transactionReceipt.isStatusOK()).thenReturn(true);
    when(transactionReceipt.getLogs()).thenReturn(Collections.singletonList(log));
    when(transaction.getGasPrice()).thenReturn(gasPrice);
    when(transaction.getNonce()).thenReturn(transactionNonce);
    when(transaction.getValue()).thenReturn(etherValue);
    when(transaction.getFrom()).thenReturn(fromAddress);
    when(transaction.getTo()).thenReturn(contractAddress);
    when(transaction.getHash()).thenReturn(transactionHash);
    when(transaction.getBlockHash()).thenReturn("blockHash");
    when(transaction.getBlockNumber()).thenReturn(BigInteger.TEN);

    service.refreshTransactionFromBlockchain(transactionHash);
    verify(transactionService, times(1)).saveTransactionDetail(argThat(transactionTmp -> {
      assertFalse(transactionTmp.isPending());
      assertTrue(transactionTmp.isSucceeded());
      assertEquals(gasPrice.doubleValue(), transactionTmp.getGasPrice(), 0);
      assertEquals(gasUsed.intValue(), transactionTmp.getGasUsed());
      assertEquals(transactionNonce.longValue(), transactionTmp.getNonce());
      assertEquals(fromAddress, transactionTmp.getFrom());
      assertEquals(WalletUtils.convertFromDecimals(etherValue, WalletUtils.ETHER_TO_WEI_DECIMALS), transactionTmp.getValue(), 0);
      assertEquals(MeedsToken.FUNC_TRANSFER, transactionTmp.getContractMethodName());
      assertTrue(transactionTmp.getContractAmount() > 0);
      assertEquals(contractAddress, transactionTmp.getContractAddress());
      return true;
    }), eq(true));

    verify(transactionService, times(1)).cancelTransactionsWithSameNonce(transactionDetail);
  }

}
