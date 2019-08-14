package org.exoplatform.addon.wallet.service;

import static org.exoplatform.addon.wallet.utils.WalletUtils.*;

import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import org.exoplatform.addon.wallet.model.*;
import org.exoplatform.addon.wallet.model.transaction.TransactionDetail;
import org.exoplatform.addon.wallet.model.transaction.TransactionStatistics;
import org.exoplatform.addon.wallet.storage.TransactionStorage;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

public class WalletTransactionServiceImpl implements WalletTransactionService {

  private static final Log             LOG               = ExoLogger.getLogger(WalletTransactionServiceImpl.class);

  private static final String          YEAR_PERIODICITY  = "year";

  private static final String          MONTH_PERIODICITY = "month";

  private WalletAccountService         accountService;

  private WalletContractService        contractService;

  private TransactionStorage           transactionStorage;

  private BlockchainTransactionService blockchainTransactionService;

  private SpaceService                 spaceService;

  private ListenerService              listenerService;

  private long                         watchedTreatedTransactionsCount;

  private long                         pendingTransactionMaxDays;

  public WalletTransactionServiceImpl(WalletAccountService accountService,
                                      TransactionStorage transactionStorage,
                                      WalletContractService contractService,
                                      InitParams params) {
    this.transactionStorage = transactionStorage;
    this.accountService = accountService;
    this.contractService = contractService;

    if (params != null && params.containsKey(TRANSACTION_PENDING_MAX_DAYS)) {
      String value = params.getValueParam(TRANSACTION_PENDING_MAX_DAYS).getValue();
      this.pendingTransactionMaxDays = Long.parseLong(value);
    }
  }

  @Override
  public List<TransactionDetail> getPendingTransactions() {
    List<TransactionDetail> pendingTransactions = transactionStorage.getPendingTransactions(getNetworkId());
    pendingTransactions.forEach(transactionDetail -> retrieveWalletsDetails(transactionDetail));
    return pendingTransactions;
  }

  @Override
  public long checkPendingTransactions(String currentUser) throws IllegalAccessException {
    if (!isUserRewardingAdmin(currentUser)) {
      throw new IllegalAccessException("User " + currentUser + " is not allowed to check pending transaction statuses");
    }
    return getBlockchainTransactionService().checkPendingTransactions(0);
  }

  @Override
  public Set<String> getPendingTransactionHashes() {
    List<TransactionDetail> pendingTransactions = transactionStorage.getPendingTransactions(getNetworkId());
    return pendingTransactions.stream().map(transactionDetail -> transactionDetail.getHash()).collect(Collectors.toSet());
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
    if (contractService.isContract(address)) {
      return getContractTransactions(address, contractMethodName, limit, currentUser);
    } else {
      return getWalletTransactions(address,
                                   contractAddress,
                                   contractMethodName,
                                   hash,
                                   limit,
                                   onlyPending,
                                   administration,
                                   currentUser);
    }
  }

  @Override
  public TransactionStatistics getTransactionStatistics(String address,
                                                        String periodicity,
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
      // to optimise with stream()
      for (long i = 11; i >= 1; i--) {
        monthsList.add(YearMonth.now().minusMonths(i));
      }
      monthsList.add(YearMonth.now());
      transactionStatistics.setPeriodicityLabel(String.valueOf(Year.now().getValue()));
      transactionStatistics.setLabels(monthsList.stream()
                                                .map(month -> StringUtils.capitalize(month.getMonth()
                                                                                          .getDisplayName(TextStyle.FULL,
                                                                                                          userLocale)))
                                                .collect(Collectors.toList()));

      // Compte list of 12 months to include in chart
      periodList = monthsList.stream().map(yearMonth -> yearMonth.atDay(1)).collect(Collectors.toList());
    } else if (StringUtils.equalsIgnoreCase(periodicity, MONTH_PERIODICITY)) {
      Month currentMonth = MonthDay.now().getMonth();
      int maxDayOfMonth = currentMonth.maxLength();
      List<Integer> dayList = IntStream.rangeClosed(1, maxDayOfMonth).boxed().collect(Collectors.toList());
      String monthLabel = StringUtils.capitalize(currentMonth.getDisplayName(TextStyle.FULL, userLocale));
      transactionStatistics.setPeriodicityLabel(monthLabel);
      transactionStatistics.setLabels(dayList.stream()
                                             .map(day -> String.format("%02d", day))
                                             .collect(Collectors.toList()));

      // Compte list of days of current month to include in chart
      periodList = dayList.stream().map(dayOfMonth -> YearMonth.now().atDay(dayOfMonth)).collect(Collectors.toList());
    } else {
      throw new IllegalArgumentException("Uknown periodicity parameter: " + periodicity);
    }

    // Compute income and outcome
    for (LocalDate startDate : periodList) {
      ZonedDateTime startDateTime = startDate.atStartOfDay(ZoneOffset.systemDefault());
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
  public void saveTransactionDetail(TransactionDetail transactionDetail,
                                    String currentUser,
                                    boolean broadcastMinedTransaction) throws IllegalAccessException {
    if (!broadcastMinedTransaction) {
      String senderAddress = StringUtils.isBlank(transactionDetail.getBy()) ? transactionDetail.getFrom()
                                                                            : transactionDetail.getBy();
      Wallet senderWallet = accountService.getWalletByAddress(senderAddress);
      if (senderWallet != null) {
        accountService.checkCanSaveWallet(senderWallet, senderWallet, currentUser);
      }
    }
    transactionStorage.saveTransactionDetail(transactionDetail);
    if (broadcastMinedTransaction) {
      broadcastTransactionMinedEvent(transactionDetail);
    }
  }

  @Override
  public long getWatchedTreatedTransactionsCount() {
    return watchedTreatedTransactionsCount;
  }

  @Override
  public long getPendingTransactionMaxDays() {
    return pendingTransactionMaxDays;
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
    Wallet senderWallet = accountService.getWalletByAddress(transactionDetail.getFrom());
    transactionDetail.setFromWallet(senderWallet);
    hideWalletOwnerPrivateInformation(senderWallet);
    if (StringUtils.isNotBlank(transactionDetail.getTo())) {
      Wallet receiverWallet = accountService.getWalletByAddress(transactionDetail.getTo());
      hideWalletOwnerPrivateInformation(receiverWallet);
      transactionDetail.setToWallet(receiverWallet);
    }
    if (StringUtils.isNotBlank(transactionDetail.getBy())) {
      Wallet senderWalletBy = accountService.getWalletByAddress(transactionDetail.getBy());
      hideWalletOwnerPrivateInformation(senderWalletBy);
      transactionDetail.setByWallet(senderWalletBy);
    }
  }

  private void retrieveWalletsDetails(TransactionDetail transactionDetail, String currentUser) {
    if (transactionDetail == null || StringUtils.isBlank(transactionDetail.getFrom())) {
      return;
    }
    Wallet senderWallet = accountService.getWalletByAddress(transactionDetail.getFrom());
    transactionDetail.setFromWallet(senderWallet);
    hideWalletOwnerPrivateInformation(senderWallet);
    if (StringUtils.isNotBlank(transactionDetail.getTo())) {
      Wallet receiverWallet = accountService.getWalletByAddress(transactionDetail.getTo());
      hideWalletOwnerPrivateInformation(receiverWallet);
      transactionDetail.setToWallet(receiverWallet);
    }
    if (StringUtils.isNotBlank(transactionDetail.getBy())) {
      Wallet senderWalletBy = accountService.getWalletByAddress(transactionDetail.getBy());
      hideWalletOwnerPrivateInformation(senderWalletBy);
      transactionDetail.setByWallet(senderWalletBy);
      if (!displayTransactionsLabel(senderWalletBy, currentUser)) {
        transactionDetail.setLabel(null);
      }
    } else if (!displayTransactionsLabel(senderWallet, currentUser)) {
      transactionDetail.setLabel(null);
    }
    if (transactionDetail.getIssuerId() > 0 && isUserRewardingAdmin(currentUser)) {
      Wallet issuerWallet = accountService.getWalletByIdentityId(transactionDetail.getIssuerId());
      transactionDetail.setIssuer(issuerWallet);
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
      JSONObject transaction = new JSONObject();
      transaction.put("hash", transactionDetail.getHash());
      transaction.put("address", transactionDetail.getFrom());
      transaction.put("status", transactionDetail.isSucceeded());
      getListenerService().broadcast(KNOWN_TRANSACTION_MINED_EVENT, null, transaction);
    } catch (Exception e) {
      LOG.warn("Error while broadcasting transaction mined event: {}", transactionDetail, e);
    }
    this.watchedTreatedTransactionsCount++;
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
                                                                                  .atStartOfDay(ZoneOffset.systemDefault())
                                                                       : selectedDay.plusDays(1)
                                                                                    .atStartOfDay(ZoneOffset.systemDefault());
  }

  /**
   * Not injected via configuration, thus not added in constructor
   * 
   * @return instance of {@link BlockchainTransactionService}
   */
  private BlockchainTransactionService getBlockchainTransactionService() {
    if (blockchainTransactionService == null) {
      blockchainTransactionService = CommonsUtils.getService(BlockchainTransactionService.class);
    }
    return blockchainTransactionService;
  }
}
