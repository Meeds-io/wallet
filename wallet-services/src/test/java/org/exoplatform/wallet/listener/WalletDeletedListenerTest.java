package org.exoplatform.wallet.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.WalletTransactionService;
import org.exoplatform.wallet.utils.WalletUtils;

@RunWith(MockitoJUnitRunner.class)
public class WalletDeletedListenerTest {

  private static final String      ADDRESS = "address";

  @Mock
  private WalletTransactionService transactionService;

  @Mock
  private ListenerService          listenerService;

  private WalletDeletedListener    listener;

  private Event<Wallet, String>    event;

  private Wallet                   wallet;

  @Before
  public void setUp() {
    wallet = new Wallet();
    wallet.setAddress(ADDRESS);
    listener = new WalletDeletedListener(transactionService);
    event = new Event<Wallet, String>(WalletUtils.WALLET_DELETED_EVENT, wallet, null);
  }

  @Test
  public void testOnEventNoTransactionSave() throws Exception {
    listener.onEvent(event);
    verify(transactionService, never()).saveTransactionDetail(any(), anyBoolean());
  }

  @Test
  public void testOnEventCancelTransactionsNotSent() throws Exception {
    TransactionDetail transactionDetail = new TransactionDetail();
    transactionDetail.setPending(true);
    when(transactionService.getPendingWalletTransactionsNotSent(ADDRESS)).thenReturn(Collections.singletonList(transactionDetail));

    listener.onEvent(event);

    verify(transactionService, times(1)).saveTransactionDetail(argThat(transaction -> !transaction.isPending()), eq(true));
  }

}
