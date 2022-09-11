package org.exoplatform.wallet.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.WalletTransactionService;
import org.exoplatform.wallet.utils.WalletUtils;

@RunWith(MockitoJUnitRunner.class)
public class TransactionCreatedListenerTest {

  @Mock
  private WalletTransactionService         walletTransactionService;

  @Mock
  private ListenerService                  listenerService;

  private TransactionCreatedListener       listener;

  private Event<Object, TransactionDetail> event;

  private TransactionDetail                transactionDetail;

  @Before
  public void setUp() {
    transactionDetail = new TransactionDetail();
    listener = new TransactionCreatedListener(listenerService, walletTransactionService);
    event = new Event<Object, TransactionDetail>(WalletUtils.TRANSACTION_MINED_EVENT, transactionDetail, transactionDetail);
  }

  @Test
  public void testOnEventForInternalWalletWithPendingTransaction() throws Exception {
    transactionDetail.setRawTransaction("RawTransaction");
    transactionDetail.setPending(true);
    listener.onEvent(event);
    verify(walletTransactionService, times(1)).cancelTransactionsWithSameNonce(transactionDetail);
    verify(listenerService, times(0)).broadcast(anyString(), any(), any());
  }

  @Test
  public void testOnEventForInternalWalletWithNonPendingTransaction() throws Exception {
    transactionDetail.setRawTransaction("RawTransaction");
    transactionDetail.setPending(false);
    listener.onEvent(event);
    verify(walletTransactionService, times(1)).cancelTransactionsWithSameNonce(transactionDetail);
    verify(listenerService, times(0)).broadcast(anyString(), any(), any());
  }

  @Test
  public void testOnEventForExternalWalletWithPendingTransaction() throws Exception {
    transactionDetail.setPending(true);
    listener.onEvent(event);
    verify(walletTransactionService, times(1)).cancelTransactionsWithSameNonce(transactionDetail);
    verify(listenerService, times(1)).broadcast(WalletUtils.TRANSACTION_SENT_TO_BLOCKCHAIN_EVENT,
                                                transactionDetail,
                                                transactionDetail);
  }

  @Test
  public void testOnEventForExternalWalletWithNotPendingTransaction() throws Exception {
    transactionDetail.setPending(false);
    listener.onEvent(event);
    verify(walletTransactionService, times(1)).cancelTransactionsWithSameNonce(transactionDetail);
    verify(listenerService, times(0)).broadcast(anyString(), any(), any());
  }

}
