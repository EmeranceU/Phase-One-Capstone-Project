package com.app.igirepay.lab1;

import com.app.igirepay.lab1.exception.DuplicateTransactionException;
import com.app.igirepay.lab1.exception.InsufficientBalanceException;
import com.app.igirepay.lab1.exception.InvalidAmountException;
import com.app.igirepay.lab1.model.Account;
import com.app.igirepay.lab1.model.Customer;
import com.app.igirepay.lab1.model.SavingsAccount;
import com.app.igirepay.lab1.model.Transaction;
import com.app.igirepay.lab1.model.WalletAccount;
import com.app.igirepay.lab1.service.AccountService;
import com.app.igirepay.lab1.service.TransactionService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Scanner;
public class Main {

    private static int nextCustomerId = 1;
    private static int nextWalletId = 1;
    private static int nextSavingsId = 1;
    private static int nextTransactionId = 1;

    public static void main(String[] args) {
        AccountService accountService = new AccountService();
        TransactionService transactionService = new TransactionService();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            printMenu();
            int choice = readChoice(scanner);

            switch (choice) {
                case 1:
                    createCustomer(scanner, accountService);
                    break;
                case 2:
                    createWalletAccount(scanner, accountService);
                    break;
                case 3:
                    createSavingsAccount(scanner, accountService);
                    break;
                case 4:
                    depositMoney(scanner, accountService, transactionService);
                    break;
                case 5:
                    withdrawMoney(scanner, accountService, transactionService);
                    break;
                case 6:
                    viewCustomerAccounts(scanner, accountService);
                    break;
                case 7:
                    viewTransactionHistory(transactionService);
                    break;
                case 8:
                    System.out.println("Goodbye.");
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid choice.");
                    break;
            }
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("=== Lab 1 Menu ===");
        System.out.println("1. Create Customer");
        System.out.println("2. Create Wallet Account");
        System.out.println("3. Create Savings Account");
        System.out.println("4. Deposit Money");
        System.out.println("5. Withdraw Money");
        System.out.println("6. View Customer Accounts");
        System.out.println("7. View Transaction History");
        System.out.println("8. Exit");
        System.out.print("Choose an option: ");
    }

    private static void createCustomer(Scanner scanner, AccountService accountService) {
        String fullName = readText(scanner, "Full name: ");
        String email = readText(scanner, "Email: ");
        String phoneNumber = readText(scanner, "Phone number: ");

        Customer customer = new Customer(String.valueOf(nextCustomerId++), fullName, email, phoneNumber);
        accountService.addCustomer(customer);
        System.out.println("Customer created with ID: " + customer.getCustomerId());
    }

    private static void createWalletAccount(Scanner scanner, AccountService accountService) {
        Customer customer = selectCustomer(scanner, accountService);
        if (customer == null) {
            return;
        }

        BigDecimal balance = readAmount(scanner, "Initial balance: ");
        String pin = readText(scanner, "PIN: ");
        WalletAccount account = new WalletAccount("WAL-" + nextWalletId++, customer.getCustomerId(), balance, pin);

        if (accountService.addAccountToCustomer(customer.getCustomerId(), account)) {
            System.out.println("Wallet account created: " + account.getAccountId());
        } else {
            System.out.println("Unable to create wallet account.");
        }
    }

    private static void createSavingsAccount(Scanner scanner, AccountService accountService) {
        Customer customer = selectCustomer(scanner, accountService);
        if (customer == null) {
            return;
        }

        BigDecimal balance = readAmount(scanner, "Initial balance: ");
        String pin = readText(scanner, "PIN: ");
        SavingsAccount account = new SavingsAccount("SAV-" + nextSavingsId++, customer.getCustomerId(), balance, pin);

        if (accountService.addAccountToCustomer(customer.getCustomerId(), account)) {
            System.out.println("Savings account created: " + account.getAccountId());
        } else {
            System.out.println("Unable to create savings account.");
        }
    }

    private static void depositMoney(Scanner scanner, AccountService accountService, TransactionService transactionService) {
        Account account = selectAccount(scanner, accountService);
        if (account == null) {
            return;
        }

        BigDecimal amount = readAmount(scanner, "Deposit amount: ");
        String referenceId = readText(scanner, "Reference ID: ");
        Transaction transaction = new Transaction(String.valueOf(nextTransactionId++), referenceId, amount, "DEPOSIT", LocalDateTime.now());

        processAndReport(transactionService, account, transaction, "Deposit");
    }

    private static void withdrawMoney(Scanner scanner, AccountService accountService, TransactionService transactionService) {
        Account account = selectAccount(scanner, accountService);
        if (account == null) {
            return;
        }

        BigDecimal amount = readAmount(scanner, "Withdrawal amount: ");
        String referenceId = readText(scanner, "Reference ID: ");
        Transaction transaction = new Transaction(String.valueOf(nextTransactionId++), referenceId, amount, "WITHDRAWAL", LocalDateTime.now());

        processAndReport(transactionService, account, transaction, "Withdrawal");
    }

    private static void viewCustomerAccounts(Scanner scanner, AccountService accountService) {
        Customer customer = selectCustomer(scanner, accountService);
        if (customer == null) {
            return;
        }

        System.out.println(customer);
        customer.getAccounts().forEach(System.out::println);
    }

    private static void viewTransactionHistory(TransactionService transactionService) {
        System.out.println("Transaction history:");
        transactionService.getTransactionHistory().forEach(System.out::println);
        System.out.println("Failed transaction logs:");
        transactionService.getFailedTransactionLogs().forEach(System.out::println);
    }

    private static Customer selectCustomer(Scanner scanner, AccountService accountService) {
        String customerId = readText(scanner, "Customer ID: ");
        Customer customer = accountService.findCustomerById(customerId);
        if (customer == null) {
            System.out.println("Customer not found.");
        }

        return customer;
    }

    private static Account selectAccount(Scanner scanner, AccountService accountService) {
        String accountId = readText(scanner, "Account ID: ");
        Account account = accountService.findAccountById(accountId);
        if (account == null) {
            System.out.println("Account not found.");
        }

        return account;
    }

    private static void processAndReport(TransactionService transactionService,
                                         Account account,
                                         Transaction transaction,
                                         String label) {
        try {
            transactionService.processTransaction(account, transaction);
            System.out.println(label + " completed: " + transaction.getReferenceId());
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