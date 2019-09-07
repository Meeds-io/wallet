package org.exoplatform.addon.wallet.model.reward;

import static org.exoplatform.addon.wallet.utils.RewardUtils.timeToSeconds;

import java.time.*;

public enum RewardPeriodType {
  WEEK,
  MONTH,
  QUARTER,
  SEMESTER,
  YEAR;

  public static final RewardPeriodType DEFAULT = MONTH;

  public RewardPeriod getPeriodOfTime(LocalDateTime localDateTime) {
    RewardPeriod rewardPeriod = new RewardPeriod(this);
    YearMonth yearMonth = YearMonth.from(localDateTime);
    switch (this) {
    case WEEK:
      LocalDateTime firstDayOfThisWeek = localDateTime.with(DayOfWeek.MONDAY);
      LocalDateTime firstDayOfNextWeek = firstDayOfThisWeek.plusWeeks(1);
      rewardPeriod.setStartDateInSeconds(timeToSeconds(firstDayOfThisWeek));
      rewardPeriod.setEndDateInSeconds(timeToSeconds(firstDayOfNextWeek));
      break;
    case MONTH:
      YearMonth currentMonth = yearMonth;
      YearMonth nextMonth = currentMonth.plusMonths(1);
      rewardPeriod.setStartDateInSeconds(timeToSeconds(currentMonth.atDay(1).atStartOfDay()));
      rewardPeriod.setEndDateInSeconds(timeToSeconds(nextMonth.atDay(1).atStartOfDay()));
      break;
    case QUARTER:
      int monthQuarterIndex = ((yearMonth.getMonthValue() - 1) / 3) * 3 + 1;

      YearMonth startQuarterMonth = YearMonth.of(yearMonth.getYear(), monthQuarterIndex);
      YearMonth endQuarterMonth = startQuarterMonth.plusMonths(3);
      rewardPeriod.setStartDateInSeconds(timeToSeconds(startQuarterMonth.atDay(1).atStartOfDay()));
      rewardPeriod.setEndDateInSeconds(timeToSeconds(endQuarterMonth.atDay(1).atStartOfDay()));
      break;
    case SEMESTER:
      int monthSemesterIndex = ((yearMonth.getMonthValue() - 1) / 6) * 6 + 1;

      YearMonth startSemesterMonth = YearMonth.of(yearMonth.getYear(), monthSemesterIndex);
      YearMonth endSemesterMonth = startSemesterMonth.plusMonths(6);
      rewardPeriod.setStartDateInSeconds(timeToSeconds(startSemesterMonth.atDay(1).atStartOfDay()));
      rewardPeriod.setEndDateInSeconds(timeToSeconds(endSemesterMonth.atDay(1).atStartOfDay()));
      break;
    case YEAR:
      rewardPeriod.setStartDateInSeconds(timeToSeconds(Year.from(localDateTime).atDay(1).atStartOfDay()));
      rewardPeriod.setEndDateInSeconds(timeToSeconds(Year.from(localDateTime).plusYears(1).atDay(1).atStartOfDay()));
      break;
    }
    return rewardPeriod;
  }
}
