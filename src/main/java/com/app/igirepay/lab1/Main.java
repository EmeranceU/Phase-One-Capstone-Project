package com.app.igirepay.lab1;

import com.app.igirepay.lab1.exception.DuplicateTransactionException;
import com.app.igirepay.lab1.exception.InsufficientBalanceException;
import com.app.igirepay.lab1.exception.InvalidAmountException;
import com.app.igirepay.lab1.exception.InvalidPinException;
import com.app.igirepay.lab1.model.Account;
import com.app.igirepay.lab1.model.Customer;
import com.app.igirepay.lab1.model.Loan;
import com.app.igirepay.lab1.model.SavingsAccount;
import com.app.igirepay.lab1.model.Transaction;
import com.app.igirepay.lab1.model.WalletAccount;
import com.app.igirepay.lab1.service.AccountService;
import com.app.igirepay.lab1.service.AuthService;
import com.app.igirepay.lab1.service.LoanService;
import com.app.igirepay.lab1.service.TransactionService;
import com.app.igirepay.lab2.dao.AccountDAO;
import com.app.igirepay.lab2.dao.CustomerDAO;
import com.app.igirepay.lab2.dao.impl.AccountDAOImpl;
import com.app.igirepay.lab2.dao.impl.CustomerDAOImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static int nextCustomerId = 1;
    private static int nextWalletId = 1;
    private static int nextSavingsId = 1;
    private static int nextTransactionId = 1;

    public static void main(String[] args) {
        // use a shared FileHandler so reads/writes target the same files
        com.app.igirepay.lab1.util.FileHandler fileHandler = new com.app.igirepay.lab1.util.FileHandler();
        
        // Create DAOs for PostgreSQL integration
        CustomerDAO customerDAO = new CustomerDAOImpl();
        AccountDAO accountDAO = new AccountDAOImpl();
        
        AccountService accountService = new AccountService(fileHandler, customerDAO, accountDAO);
        TransactionService transactionService = new TransactionService(fileHandler);
        AuthService authService = new AuthService(accountService, fileHandler, customerDAO);
        LoanService loanService = new LoanService(accountService, transactionService, fileHandler);

        // Load persisted data into memory before showing menus
        fileHandler.loadAllData(accountService, transactionService, loanService);
        syncNextIds(accountService, transactionService);
        Customer loggedInCustomer = null;

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                if (loggedInCustomer == null) {
                    loggedInCustomer = handleStartMenu(scanner, accountService, authService);
                    continue;
                }

                loggedInCustomer = handleAuthenticatedMenu(scanner, loggedInCustomer, accountService, transactionService, authService, loanService);
            }
        }
    }

    private static Customer handleStartMenu(Scanner scanner, AccountService accountService, AuthService authService) {
        printStartMenu();
        switch (readChoice(scanner)) {
            case 1:
                registerCustomer(scanner, accountService);
                return null;
            case 2:
                return login(scanner, authService);
            case 3:
                System.out.println("Goodbye.");
                System.exit(0);
                return null;
            default:
                System.out.println("Invalid choice.");
                return null;
        }
    }

    private static Customer handleAuthenticatedMenu(Scanner scanner,
                                                    Customer loggedInCustomer,
                                                    AccountService accountService,
                                                    TransactionService transactionService,
                                                    AuthService authService,
                                                    LoanService loanService) {
        printAuthenticatedMenu();
        switch (readChoice(scanner)) {
            case 1:
                createWalletAccount(scanner, accountService, loggedInCustomer);
                return loggedInCustomer;
            case 2:
                createSavingsAccount(scanner, accountService, loggedInCustomer);
                return loggedInCustomer;
            case 3:
                depositMoney(scanner, accountService, transactionService, loggedInCustomer);
                return loggedInCustomer;
            case 4:
                withdrawMoney(scanner, accountService, transactionService, loggedInCustomer);
                return loggedInCustomer;
            case 5:
                transferMoney(scanner, accountService, transactionService, loggedInCustomer);
                return loggedInCustomer;
            case 6:
                checkAccountBalance(loggedInCustomer);
                return loggedInCustomer;
            case 7:
                viewMyAccounts(loggedInCustomer);
                return loggedInCustomer;
            case 8:
                viewTransactionHistory(transactionService, loggedInCustomer);
                return loggedInCustomer;
            case 9:
                requestLoan(scanner, loanService, loggedInCustomer);
                return loggedInCustomer;
            case 10:
                viewLoanHistory(loanService, loggedInCustomer);
                return loggedInCustomer;
            case 11:
                return changePin(scanner, authService, loggedInCustomer);
            case 12:
                System.out.println("Logged out.");
                return null;
            default:
                System.out.println("Invalid choice.");
                return loggedInCustomer;
        }
    }

    private static void printStartMenu() {
        System.out.println();
        System.out.println("=== IgirePay Ltd ===");
        System.out.println("1. Register Customer");
        System.out.println("2. Login");
        System.out.println("3. Exit");
        System.out.print("Choose an option: ");
    }

    private static void printAuthenticatedMenu() {
        System.out.println();
        System.out.println("=== Authenticated Menu ===");
        System.out.println("1. Create Wallet Account");
        System.out.println("2. Create Savings Account");
        System.out.println("3. Deposit Money");
        System.out.println("4. Withdraw Money");
        System.out.println("5. Transfer Money");
        System.out.println("6. Check Balance");
        System.out.println("7. View My Accounts");
        System.out.println("8. View Transaction History");
        System.out.println("9. Request Loan");
        System.out.println("10. View Loan History");
        System.out.println("11. Change PIN");
        System.out.println("12. Logout");
        System.out.print("Choose an option: ");
    }

    private static void syncNextIds(AccountService accountService, TransactionService transactionService) {
        nextCustomerId = accountService.getCustomers().stream()
                .map(Customer::getCustomerId)
                .mapToInt(Main::extractNumericId)
                .max()
                .orElse(0) + 1;

        nextWalletId = accountService.getAccounts().stream()
                .filter(account -> account instanceof WalletAccount)
                .map(Account::getAccountId)
                .mapToInt(Main::extractNumericId)
                .max()
                .orElse(0) + 1;

        nextSavingsId = accountService.getAccounts().stream()
                .filter(account -> account instanceof SavingsAccount)
                .map(Account::getAccountId)
                .mapToInt(Main::extractNumericId)
                .max()
                .orElse(0) + 1;

        nextTransactionId = transactionService.getTransactionHistory().stream()
                .map(Transaction::getTransactionId)
                .mapToInt(Main::extractNumericId)
                .max()
                .orElse(0) + 1;
    }

    private static int extractNumericId(String value) {
        if (value == null) {
            return 0;
        }

        String digits = value.replaceAll("\\D+", "");
        if (digits.isEmpty()) {
            return 0;
        }

        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static void registerCustomer(Scanner scanner, AccountService accountService) {
        String fullName = readText(scanner, "Full name: ");
        String email = readText(scanner, "Email: ");
        String phoneNumber = readText(scanner, "Phone number: ");
        String pin = readText(scanner, "PIN: ");

        Customer customer = new Customer(String.valueOf(nextCustomerId++), fullName, email, phoneNumber, pin);
        if (accountService.addCustomer(customer)) {
            System.out.println("Customer registered with ID: " + customer.getCustomerId());
        } else {
            System.out.println("Unable to register customer.");
        }
    }

    private static Customer login(Scanner scanner, AuthService authService) {
        String phoneNumber = readText(scanner, "Phone number: ");
        String pin = readText(scanner, "PIN: ");

        try {
            Customer customer = authService.login(phoneNumber, pin);
            System.out.println("Login successful for: " + customer.getFullName());
            return customer;
        } catch (InvalidPinException exception) {
            System.out.println("Login failed: " + exception.getMessage());
            return null;
        }
    }

    private static Customer changePin(Scanner scanner, AuthService authService, Customer loggedInCustomer) {
        String phoneNumber = readText(scanner, "Phone number: ");
        String currentPin = readText(scanner, "Current PIN: ");
        String newPin = readText(scanner, "New PIN: ");

        try {
            authService.changePin(phoneNumber, currentPin, newPin);
            System.out.println("PIN changed successfully.");
            return authService.login(phoneNumber, newPin);
        } catch (InvalidPinException exception) {
            System.out.println("PIN change failed: " + exception.getMessage());
            return loggedInCustomer;
        }
    }

    private static void createWalletAccount(Scanner scanner, AccountService accountService, Customer loggedInCustomer) {
        BigDecimal balance = readAmount(scanner, "Initial balance: ");
        WalletAccount account = new WalletAccount("WAL-" + nextWalletId++, loggedInCustomer.getCustomerId(), balance);

        if (accountService.addAccountToCustomer(loggedInCustomer.getCustomerId(), account)) {
            System.out.println("Wallet account created: " + account.getAccountId());
        } else {
            System.out.println("Unable to create wallet account.");
        }
    }

    private static void createSavingsAccount(Scanner scanner, AccountService accountService, Customer loggedInCustomer) {
        BigDecimal balance = readAmount(scanner, "Initial balance: ");
        SavingsAccount account = new SavingsAccount("SAV-" + nextSavingsId++, loggedInCustomer.getCustomerId(), balance);

        if (accountService.addAccountToCustomer(loggedInCustomer.getCustomerId(), account)) {
            System.out.println("Savings account created: " + account.getAccountId());
        } else {
            System.out.println("Unable to create savings account.");
        }
    }

    private static void depositMoney(Scanner scanner,
                                     AccountService accountService,
                                     TransactionService transactionService,
                                     Customer loggedInCustomer) {
        Account account = selectCustomerAccount(scanner, accountService, loggedInCustomer);
        if (account == null) {
            return;
        }

        BigDecimal amount = readAmount(scanner, "Deposit amount: ");
        String referenceId = readText(scanner, "Reference ID: ");
        Transaction transaction = new Transaction(String.valueOf(nextTransactionId++),
                loggedInCustomer.getCustomerId(),
                account.getAccountId(),
            null,
                referenceId,
                amount,
                "DEPOSIT",
                LocalDateTime.now());

        processAndReport(transactionService, account, transaction, "Deposit");
    }

    private static void withdrawMoney(Scanner scanner,
                                      AccountService accountService,
                                      TransactionService transactionService,
                                      Customer loggedInCustomer) {
        Account account = selectCustomerAccount(scanner, accountService, loggedInCustomer);
        if (account == null) {
            return;
        }

        BigDecimal amount = readAmount(scanner, "Withdrawal amount: ");
        String referenceId = readText(scanner, "Reference ID: ");
        Transaction transaction = new Transaction(String.valueOf(nextTransactionId++),
                loggedInCustomer.getCustomerId(),
                account.getAccountId(),
            null,
                referenceId,
                amount,
                "WITHDRAWAL",
                LocalDateTime.now());

        processAndReport(transactionService, account, transaction, "Withdrawal");
    }

    private static void transferMoney(Scanner scanner,
                                      AccountService accountService,
                                      TransactionService transactionService,
                                      Customer loggedInCustomer) {
        Account sourceAccount = selectCustomerAccount(scanner, accountService, loggedInCustomer);
        if (sourceAccount == null) {
            return;
        }

        BigDecimal amount = readAmount(scanner, "Transfer amount: ");
        String destinationAccountId = readText(scanner, "Destination Account ID: ");
        String referenceId = readText(scanner, "Reference ID: ");
        Transaction transaction = new Transaction(String.valueOf(nextTransactionId++),
                loggedInCustomer.getCustomerId(),
                sourceAccount.getAccountId(),
                destinationAccountId,
                referenceId,
                amount,
                "TRANSFER",
                LocalDateTime.now());

        try {
            transactionService.processTransfer(accountService, loggedInCustomer, transaction);
            Account destinationAccount = accountService.findAccountById(destinationAccountId);
            System.out.println("Transfer successful: " + sourceAccount.getAccountId() + " -> " + destinationAccountId + " | Source balance: " + sourceAccount.getBalance() + " | Destination balance: " + destinationAccount.getBalance());
        } catch (DuplicateTransactionException | InvalidAmountException | InsufficientBalanceException exception) {
            System.out.println("Transfer failed: " + exception.getMessage());
        }
    }

    private static void viewMyAccounts(Customer loggedInCustomer) {
        if (loggedInCustomer.getAccounts().isEmpty()) {
            System.out.println("No accounts found. Please create an account first.");
            return;
        }

        loggedInCustomer.getAccounts().forEach(account ->
                System.out.println(account.getClass().getSimpleName() + " | " + account.getAccountId() + " | Balance: " + account.getBalance()));
    }

    private static void checkAccountBalance(Customer loggedInCustomer) {
        if (loggedInCustomer.getAccounts().isEmpty()) {
            System.out.println("No accounts found. Please create an account first.");
            return;
        }

        System.out.println("Account balances:");
        loggedInCustomer.getAccounts().forEach(account ->
                System.out.println(account.getClass().getSimpleName() + " | "
                        + account.getAccountId() + " | Balance: " + account.getBalance()));
    }

    private static void viewTransactionHistory(TransactionService transactionService, Customer loggedInCustomer) {
        List<Transaction> transactions = transactionService.getTransactionHistoryForCustomer(loggedInCustomer.getCustomerId());
        if (transactions.isEmpty()) {
            System.out.println("No transaction history available.");
            return;
        }

        System.out.println("Transaction history:");
        transactions.forEach(System.out::println);
    }

    private static void requestLoan(Scanner scanner, LoanService loanService, Customer loggedInCustomer) {
        BigDecimal amount = readAmount(scanner, "Loan amount: ");
        Loan loan = loanService.requestLoan(loggedInCustomer, amount);
        if (loan.isApproved()) {
            System.out.println("Loan approved: " + loan);
        } else {
            System.out.println(loan.getRepaymentStatus());
        }
    }

    private static void viewLoanHistory(LoanService loanService, Customer loggedInCustomer) {
        List<Loan> loans = loanService.getLoanHistoryForCustomer(loggedInCustomer.getCustomerId());
        if (loans.isEmpty()) {
            System.out.println("No loan history available.");
            return;
        }

        System.out.println("Loan history:");
        loans.forEach(System.out::println);
    }

    private static Account selectCustomerAccount(Scanner scanner, AccountService accountService, Customer loggedInCustomer) {
        List<Account> accounts = accountService.getAccountsForCustomer(loggedInCustomer.getCustomerId());
        if (accounts.isEmpty()) {
            System.out.println("No accounts found for this customer.");
            return null;
        }

        System.out.println("Available accounts:");
        accounts.forEach(System.out::println);
        String accountId = readText(scanner, "Account ID: ");

        for (Account account : accounts) {
            if (account.getAccountId().equals(accountId)) {
                return account;
            }
        }

        System.out.println("Account not found.");
        return null;
    }

    private static void processAndReport(TransactionService transactionService,
                                         Account account,
                                         Transaction transaction,
                                         String label) {
        try {
            transactionService.processTransaction(account, transaction);
            System.out.println(label + " successful: " + account.getAccountId() + " | Balance: " + account.getBalance());
        } catch (DuplicateTransactionException | InvalidAmountException | InsufficientBalanceException exception) {
            System.out.println(label + " failed: " + exception.getMessage());
        }
    }

    private static BigDecimal readAmount(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            try {
                BigDecimal amount = new BigDecimal(input);
                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    return amount;
                }
            } catch (NumberFormatException ignored) {
            }

            System.out.println("Enter a valid positive amount.");
        }
    }

    private static int readChoice(Scanner scanner) {
        while (true) {
            System.out.print("Choose an option: ");
            String input = scanner.nextLine().trim();

            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException exception) {
                System.out.println("Enter a valid menu choice.");
            }
        }
    }

    private static String readText(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            if (!input.isEmpty()) {
                return input;
            }

            System.out.println("Value must not be blank.");
        }
    }
}