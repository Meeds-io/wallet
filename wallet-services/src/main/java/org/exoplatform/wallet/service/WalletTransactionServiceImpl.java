package org.exoplatform.wallet.service;

import static org.exoplatform.wallet.utils.WalletUtils.*;

import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wallet.model.*;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.model.transaction.TransactionStatistics;
import org.exoplatform.wallet.storage.TransactionStorage;

public class WalletTransactionServiceImpl implements WalletTransactionService {

  private static final Log      LOG                                       =
                                    ExoLogger.getLogger(WalletTransactionServiceImpl.class);

  private static final String   YEAR_PERIODICITY                          = "year";

  private static final String   MONTH_PERIODICITY                         = "month";

  private static final long     DEFAULT_MAX_PARALLEL_PENDING_TRANSACTIONS = 1;

  private static final long     DEFAULT_MAX_SENDING_TRANSACTIONS_ATTEMPTS = 3;

  private WalletAccountService  accountService;

  private WalletContractService contractService;

  private TransactionStorage    transactionStorage;

  private SpaceService          spaceService;

  private ListenerService       listenerService;

  private long                  pendingTransactionMaxDays;

  private long                  maxParallelPendingTransactions;

  private long                  maxAttemptsToSend;

  private boolean               logAllTransaction;

  public WalletTransactionServiceImpl(WalletAccountService accountService,
                                      TransactionStorage transactionStorage,
                                      WalletContractService contractService,
                                      InitParams params) {
    this.transactionStorage = transactionStorage;
    this.accountService = accountService;
    this.contractService = contractService;

    if (params != null) {
      if (params.containsKey(TRANSACTION_PENDING_MAX_DAYS)) {
        String value = params.getValueParam(TRANSACTION_PENDING_MAX_DAYS).getValue();
        this.pendingTransactionMaxDays = Long.parseLong(value);
      }
      if (params.containsKey(MAX_PENDING_TRANSACTIONS_TO_SEND)) {
        String value = params.getValueParam(MAX_PENDING_TRANSACTIONS_TO_SEND).getValue();
        this.maxParallelPendingTransactions = Long.parseLong(value);
      }
      if (params.containsKey(MAX_SENDING_TRANSACTIONS_ATTEMPTS)) {
        String value = params.getValueParam(MAX_SENDING_TRANSACTIONS_ATTEMPTS).getValue();
        this.maxAttemptsToSend = Long.parseLong(value);
      }
      if (params.containsKey(LOG_ALL_CONTRACT_TRANSACTIONS)) {
        String value = params.getValueParam(LOG_ALL_CONTRACT_TRANSACTIONS).getValue();
        this.logAllTransaction = Boolean.parseBoolean(value);
      }
    }
    if (this.maxParallelPendingTransactions <= 0) {
      LOG.warn("Invalid value {} for parameter {}, using default value {}",
               this.maxParallelPendingTransactions,
               MAX_PENDING_TRANSACTIONS_TO_SEND,
               DEFAULT_MAX_PARALLEL_PENDING_TRANSACTIONS);
      this.maxParallelPendingTransactions = DEFAULT_MAX_PARALLEL_PENDING_TRANSACTIONS;
    }
    if (this.maxAttemptsToSend <= 0) {
      LOG.warn("Invalid value {} for parameter {}, using default value {}",
               this.maxAttemptsToSend,
               MAX_SENDING_TRANSACTIONS_ATTEMPTS,
               DEFAULT_MAX_SENDING_TRANSACTIONS_ATTEMPTS);
      this.maxAttemptsToSend = DEFAULT_MAX_SENDING_TRANSACTIONS_ATTEMPTS;
    }
  }

  @Override
  public List<TransactionDetail> getPendingTransactions() {
    return transactionStorage.getPendingTransaction(getNetworkId());
  }

  @Override
  public List<TransactionDetail> getTransactions(String address,
                                                 String contractAddress,
                                                 String contractMethodName,
                                                 String hash,
                                                 int limit,
                                                 boolean onlyPending,
                                                 boolean administration,
                                                 String currentUser) throws IllegalAccessException {
    if (administration && !isUserRewardingAdmin(currentUser)) {
      throw new IllegalAccessException("User " + currentUser + " is not allowed to get administrative transactions");
    }

    if (contractService.isContract(address)) {
      if (isUserRewardingAdmin(currentUser)) {
        return getContractTransactions(address, contractMethodName, limit, currentUser);
      } else {
        throw new IllegalAccessException("User " + currentUser + " is not allowed to get all contract transactions");
      }
    } else if (StringUtils.isNotBlank(address)) {
      return getWalletTransactions(address,
                                   contractAddress,
                                   contractMethodName,
                                   hash,
                                   limit,
                                   onlyPending,
                                   administration,
                                   currentUser);
    } else if (administration) {
      return getTransactions(limit, currentUser);
    } else {
      throw new IllegalStateException("User " + currentUser + " is not allowed to get all contract transactions");
    }
  }

  @Override
  public TransactionStatistics getTransactionStatistics(String address,
                                                        String periodicity,
                                                        String selectedDate,
                                                        Locale locale) {
    if (StringUtils.isBlank(address)) {
      throw new IllegalArgumentException("Wallet address is mandatory");
    }
    if (StringUtils.isBlank(periodicity)) {
      throw new IllegalArgumentException("Periodicity is mandatory");
    }

    TransactionStatistics transactionStatistics = new TransactionStatistics();
    List<LocalDate> periodList = null;

    final Locale userLocale = locale == null ? Locale.getDefault() : locale;
    if (StringUtils.equalsIgnoreCase(periodicity, YEAR_PERIODICITY)) {
      // Compute labels to display in chart
      List<YearMonth> monthsList = new ArrayList<>();

      String[] selectedDateParts = StringUtils.isBlank(selectedDate) ? null : selectedDate.split("-");
      Year selectedYear = selectedDateParts == null ? Year.now() : Year.of(Integer.parseInt(selectedDateParts[0]));

      // to optimise with stream()
      for (int i = 1; i <= 12; i++) {
        monthsList.add(YearMonth.of(selectedYear.getValue(), i));
      }

      transactionStatistics.setPeriodicityLabel(String.valueOf(selectedYear.getValue()));
      transactionStatistics.setLabels(monthsList.stream()
                                                .map(month -> StringUtils.capitalize(month.getMonth()
                                                                                          .getDisplayName(TextStyle.FULL,
                                                                                                          userLocale)))
                                                .collect(Collectors.toList()));

      // Compute list of 12 months to include in chart
      periodList = monthsList.stream().map(yearMonth -> yearMonth.atDay(1)).collect(Collectors.toList());
    } else if (StringUtils.equalsIgnoreCase(periodicity, MONTH_PERIODICITY)) {
      String[] selectedDateParts = StringUtils.isBlank(selectedDate) ? null : selectedDate.split("-");
      YearMonth selectedMonth = selectedDateParts == null ? YearMonth.now()
                                                          : YearMonth.of(Integer.parseInt(selectedDateParts[0]),
                                                                         Integer.parseInt(selectedDateParts[1]));

      int maxDayOfMonth = selectedMonth.lengthOfMonth();
      List<Integer> dayList = IntStream.rangeClosed(1, maxDayOfMonth).boxed().collect(Collectors.toList());
      String monthLabel = StringUtils.capitalize(selectedMonth.getMonth().getDisplayName(TextStyle.FULL, userLocale))
          + " " + selectedMonth.getYear();
      transactionStatistics.setPeriodicityLabel(monthLabel);
      transactionStatistics.setLabels(dayList.stream()
                                             .map(day -> String.format("%02d", day))
                                             .collect(Collectors.toList()));

      // Compte list of days of current month to include in chart
      periodList = dayList.stream().map(dayOfMonth -> selectedMonth.atDay(dayOfMonth)).collect(Collectors.toList());
    } else {
      throw new IllegalArgumentException("Uknown periodicity parameter: " + periodicity);
    }

    // Compute income and outcome
    for (LocalDate startDate : periodList) {
      ZonedDateTime startDateTime = startDate.atStartOfDay(ZoneId.systemDefault());
      ZonedDateTime endDateTime = getEndDate(startDate, periodicity);

      String contractAddress = getContractAddress();
      double receivedContractAmount = transactionStorage.countReceivedContractAmount(contractAddress,
                                                                                     address,
                                                                                     startDateTime,
                                                                                     endDateTime);
      transactionStatistics.getIncome().add(String.valueOf(receivedContractAmount));
      double sentContractAmount = transactionStorage.countSentContractAmount(contractAddress,
                                                                             address,
                                                                             startDateTime,
                                                                             endDateTime);
      transactionStatistics.getOutcome().add(String.valueOf(sentContractAmount));
    }
    return transactionStatistics;
  }

  @Override
  public TransactionDetail getTransactionByHash(String hash, String currentUser) {
    TransactionDetail transactionDetail = transactionStorage.getTransactionByHash(hash);
    if (transactionDetail != null) {
      retrieveWalletsDetails(transactionDetail, currentUser);
    }
    return transactionDetail;
  }

  @Override
  public long getNonce(String fromAddress) {
    if (transactionStorage.countPendingTransactionAsSender(getNetworkId(), fromAddress) == 0) {
      // Let nonce be determined from blockchain on client side
      return 0;
    } else {
      long maxUsedNonce = transactionStorage.getMaxUsedNonce(getNetworkId(), fromAddress);
      return maxUsedNonce == 0 ? 0 : maxUsedNonce + 1;
    }
  }

  @Override
  public long getNonce(String fromAddress, String currentUser) throws IllegalAccessException {
    Wallet wallet = accountService.getWalletByAddress(fromAddress);
    if (wallet == null || !accountService.isWalletOwner(wallet, currentUser)) {
      throw new IllegalAccessException("User '" + currentUser
          + "' is attempting to access last transaction nonce for wallet of user " + wallet);
    }
    return getNonce(fromAddress);
  }

  @Override
  public TransactionDetail getTransactionByHash(String hash) {
    TransactionDetail transactionDetail = transactionStorage.getTransactionByHash(hash);
    if (transactionDetail != null) {
      retrieveWalletsDetails(transactionDetail);
    }
    return transactionDetail;
  }

  @Override
  public void saveTransactionDetail(TransactionDetail transactionDetail,
                                    boolean broadcastMinedTransaction) {
    transactionStorage.saveTransactionDetail(transactionDetail);
    if (broadcastMinedTransaction) {
      broadcastTransactionMinedEvent(transactionDetail);
    }
  }

  @Override
  public void saveTransactionDetail(TransactionDetail transactionDetail, String currentUser) throws IllegalAccessException {
    if (StringUtils.isBlank(currentUser)) {
      throw new IllegalArgumentException("username is mandatory");
    }
    String senderAddress = StringUtils.isBlank(transactionDetail.getBy()) ? transactionDetail.getFrom()
                                                                          : transactionDetail.getBy();
    Wallet senderWallet = accountService.getWalletByAddress(senderAddress);
    if (senderWallet == null || !accountService.isWalletOwner(senderWallet, currentUser)) {
      throw new IllegalAccessException("User '" + currentUser
          + "' is attempting to save a new transaction for wallet of user " + senderWallet);
    }

    Wallet issuerWallet = accountService.getWalletByTypeAndId(WalletType.USER.getId(), currentUser);
    transactionDetail.setIssuer(issuerWallet);
    transactionDetail.setNetworkId(getNetworkId());
    transactionStorage.saveTransactionDetail(transactionDetail);
  }

  @Override
  public long getPendingTransactionMaxDays() {
    return pendingTransactionMaxDays;
  }

  @Override
  public List<TransactionDetail> getTransactionsToSend() {
    List<TransactionDetail> transactionsToSend = transactionStorage.getTransactionsToSend(getNetworkId());

    transactionsToSend.stream().forEach(transactionDetail -> retrieveWalletsDetails(transactionDetail));
    return transactionsToSend;
  }

  @Override
  public boolean canSendTransactionToBlockchain(String fromAddress) {
    return transactionStorage.countPendingTransactionSent(getNetworkId(), fromAddress) < this.maxParallelPendingTransactions;
  }

  @Override
  public long getMaxAttemptsToSend() {
    return maxAttemptsToSend;
  }

  @Override
  public long getMaxParallelPendingTransactions() {
    return maxParallelPendingTransactions;
  }

  @Override
  public boolean isLogAllTransaction() {
    return logAllTransaction;
  }

  private List<TransactionDetail> getTransactions(int limit, String currentUser) {
    List<TransactionDetail> transactionDetails = transactionStorage.getTransactions(getNetworkId(), limit);
    transactionDetails.stream().forEach(transactionDetail -> retrieveWalletsDetails(transactionDetail, currentUser));
    return transactionDetails;
  }

  private List<TransactionDetail> getContractTransactions(String contractAddress,
                                                          String contractMethodName,
                                                          int limit,
                                                          String currentUser) throws IllegalAccessException {
    ContractDetail contractDetail = contractService.getContractDetail(contractAddress);
    if (contractDetail == null) {
      throw new IllegalStateException("Can't find contract with address " + contractAddress);
    }

    if (!isUserRewardingAdmin(currentUser)) {
      throw new IllegalAccessException("User " + currentUser + " attempts to access contract transactions with address "
          + contractAddress);
    }

    List<TransactionDetail> transactionDetails = transactionStorage.getContractTransactions(contractAddress,
                                                                                            contractMethodName,
                                                                                            limit);
    transactionDetails.stream().forEach(transactionDetail -> retrieveWalletsDetails(transactionDetail, currentUser));
    return transactionDetails;
  }

  private List<TransactionDetail> getWalletTransactions(String address,
                                                        String contractAddress,
                                                        String contractMethodName,
                                                        String hash,
                                                        int limit,
                                                        boolean pending,
                                                        boolean administration,
                                                        String currentUser) throws IllegalAccessException {
    Wallet wallet = accountService.getWalletByAddress(address);
    if (wallet == null) {
      return Collections.emptyList();
    }
    if (!canAccessWallet(wallet, currentUser)) {
      throw new IllegalAccessException("Can't access wallet with address " + address);
    }

    List<TransactionDetail> transactionDetails = transactionStorage.getWalletTransactions(getNetworkId(),
                                                                                          address,
                                                                                          contractAddress,
                                                                                          contractMethodName,
                                                                                          hash,
                                                                                          limit,
                                                                                          pending,
                                                                                          administration);

    transactionDetails.stream().forEach(transactionDetail -> retrieveWalletsDetails(transactionDetail, currentUser));
    return transactionDetails;
  }

  private void retrieveWalletsDetails(TransactionDetail transactionDetail) {
    if (transactionDetail == null || StringUtils.isBlank(transactionDetail.getFrom())) {
      return;
    }
    if (transactionDetail.getFromWallet() == null) {
      Wallet senderWallet = accountService.getWalletByAddress(transactionDetail.getFrom());
      transactionDetail.setFromWallet(senderWallet);
      hideWalletOwnerPrivateInformation(senderWallet);
    }
    if (transactionDetail.getToWallet() == null && StringUtils.isNotBlank(transactionDetail.getTo())) {
      Wallet receiverWallet = accountService.getWalletByAddress(transactionDetail.getTo());
      hideWalletOwnerPrivateInformation(receiverWallet);
      transactionDetail.setToWallet(receiverWallet);
    }
    if (transactionDetail.getByWallet() == null && StringUtils.isNotBlank(transactionDetail.getBy())) {
      Wallet senderWalletBy = accountService.getWalletByAddress(transactionDetail.getBy());
      hideWalletOwnerPrivateInformation(senderWalletBy);
      transactionDetail.setByWallet(senderWalletBy);
    }
    if (transactionDetail.getIssuer() == null && transactionDetail.getIssuerId() > 0) {
      Wallet issuerWallet = accountService.getWalletByIdentityId(transactionDetail.getIssuerId());
      transactionDetail.setIssuer(issuerWallet);
    }
  }

  private void retrieveWalletsDetails(TransactionDetail transactionDetail, String currentUser) {
    if (transactionDetail == null || StringUtils.isBlank(transactionDetail.getFrom())) {
      return;
    }
    retrieveWalletsDetails(transactionDetail);
    if (StringUtils.isNotBlank(transactionDetail.getBy())) {
      if (!displayTransactionsLabel(transactionDetail.getByWallet(), currentUser)) {
        transactionDetail.setLabel(null);
      }
    } else if (!displayTransactionsLabel(transactionDetail.getFromWallet(), currentUser)) {
      transactionDetail.setLabel(null);
    }
  }

  private boolean displayTransactionsLabel(Wallet senderWallet, String currentUserId) {
    if (senderWallet == null || isAdminAccount(senderWallet.getAddress())) {
      return isUserRewardingAdmin(currentUserId);
    }
    String accountId = senderWallet.getId();
    String accountType = senderWallet.getType();
    if (StringUtils.isBlank(accountId) || StringUtils.isBlank(accountType)) {
      return isUserRewardingAdmin(currentUserId);
    }

    if (WalletType.isSpace(senderWallet.getType())) {
      if (getSpaceService().isSuperManager(currentUserId)) {
        return true;
      }
      Space space = getSpace(accountId);
      return space != null && getSpaceService().isManager(space, currentUserId);
    } else {
      return StringUtils.equalsIgnoreCase(accountId, currentUserId);
    }
  }

  private void broadcastTransactionMinedEvent(TransactionDetail transactionDetail) {
    try {
      Map<String, Object> transaction = new HashMap<>();
      transaction.put("hash", transactionDetail.getHash());
      transaction.put("from", transactionDetail.getFromWallet() == null ? 0 : transactionDetail.getFromWallet().getTechnicalId());
      transaction.put("to", transactionDetail.getToWallet() == null ? 0 : transactionDetail.getToWallet().getTechnicalId());
      transaction.put("contractAddress", transactionDetail.getContractAddress());
      transaction.put("contractAmount", transactionDetail.getContractAmount());
      transaction.put("contractMethodName", transactionDetail.getContractMethodName());
      transaction.put("etherAmount", transactionDetail.getValue());
      transaction.put("status", transactionDetail.isSucceeded());
      transaction.put("issuerId", transactionDetail.getIssuerId());
      getListenerService().broadcast(KNOWN_TRANSACTION_MINED_EVENT, null, transaction);
    } catch (Exception e) {
      LOG.warn("Error while broadcasting transaction mined event: {}", transactionDetail, e);
    }
  }

  private SpaceService getSpaceService() {
    if (spaceService == null) {
      spaceService = CommonsUtils.getService(SpaceService.class);
    }
    return spaceService;
  }

  private ListenerService getListenerService() {
    if (listenerService == null) {
      listenerService = CommonsUtils.getService(ListenerService.class);
    }
    return listenerService;
  }

  private ZonedDateTime getEndDate(LocalDate selectedDay, String periodicity) {
    return StringUtils.equalsIgnoreCase(periodicity, YEAR_PERIODICITY)
                                                                       ? YearMonth.of(selectedDay.getYear(),
                                                                                      selectedDay.getMonthValue())
                                                                                  .atEndOfMonth()
                                                                                  .plusDays(1)
                                                                                  .atStartOfDay(ZoneId.systemDefault())
                                                                       : selectedDay.plusDays(1)
                                                                                    .atStartOfDay(ZoneId.systemDefault());
  }
}
