package com.app.igirepay.lab1.service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class DailyTransactionSummary {

    private final int deposits;
    private final int withdrawals;
    private final int transfers;
    private final BigDecimal moneyIn;
    private final BigDecimal moneyOut;

    public DailyTransactionSummary(int deposits, int withdrawals, int transfers, BigDecimal moneyIn, BigDecimal moneyOut) {
        this.deposits = deposits;
        this.withdrawals = withdrawals;
        this.transfers = transfers;
        this.moneyIn = moneyIn == null ? BigDecimal.ZERO : moneyIn;
        this.moneyOut = moneyOut == null ? BigDecimal.ZERO : moneyOut;
    }

    public int getDeposits() {
        return deposits;
    }

    public int getWithdrawals() {
        return withdrawals;
    }

    public int getTransfers() {
        return transfers;
    }

    public BigDecimal getMoneyIn() {
        return moneyIn;
    }

    public BigDecimal getMoneyOut() {
        return moneyOut;
    }

    public String toDisplayString() {
        return "Daily Transaction Summary\n\n"
                + "Deposits: " + deposits + "\n"
                + "Withdrawals: " + withdrawals + "\n"
                + "Transfers: " + transfers + "\n\n"
                + "Money In: " + formatAmount(moneyIn) + " RWF\n"
                + "Money Out: " + formatAmount(moneyOut) + " RWF";
    }

    private String formatAmount(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        return NumberFormat.getNumberInstance(Locale.US).format(value.stripTrailingZeros());
    }
}