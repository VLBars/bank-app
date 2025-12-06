package client;

import common.Account;
import common.BankResponse;
import common.Transaction;
import utils.Config;
import utils.CurrencyConverter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Scanner;

public class ClientUI {
    private BankClient client;
    private Scanner scanner;
    private boolean loggedIn;
    private static final NumberFormat currencyFormatter = new DecimalFormat("#,##0.00");
    
    public ClientUI() {
        InputStream inputStream = System.in;
        this.scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name());
    }
    
    public void start() {
        System.out.println("=== Банковский клиент ===");
        
        try {
            client = new BankClient(Config.SERVER_HOST, Config.SERVER_PORT);
            System.out.println("Подключение к серверу установлено");
            
            showLoginMenu();
            
            if (loggedIn) {
                showMainMenu();
            }
            
        } catch (Exception e) {
            System.out.println("Ошибка подключения к серверу: " + e.getMessage());
        } finally {
            if (client != null) {
                client.logout();
            }
        }
    }
    
    private void showLoginMenu() {
        while (!loggedIn) {
            System.out.println("\n--- Вход в систему ---");
            System.out.println("1. Войти");
            System.out.println("2. Зарегистрироваться");
            System.out.print("Выберите действие: ");
            
            String choice = scanner.nextLine().trim();
            
            if (choice.equals("1")) {
                performLogin();
            } else if (choice.equals("2")) {
                performRegister();
            } else {
                System.out.println("Неверный выбор");
            }
        }
    }
    
    private void performLogin() {
        System.out.print("Логин: ");
        String login = scanner.nextLine().trim();
        
        if (login.isEmpty()) {
            System.out.println("Ошибка: Логин не может быть пустым");
            return;
        }
        
        System.out.print("Пароль: ");
        String password = scanner.nextLine().trim();
        
        if (password.isEmpty()) {
            System.out.println("Ошибка: Пароль не может быть пустым");
            return;
        }
        
        BankResponse response = client.login(login, password);
        System.out.println(response.getMessage());
        
        if (response.isSuccess()) {
            loggedIn = true;
        } else {
            System.out.println("Повторите попытку");
        }
    }
    
    private void performRegister() {
        System.out.print("Логин: ");
        String login = scanner.nextLine().trim();
        
        if (login.isEmpty()) {
            System.out.println("Ошибка: Логин не может быть пустым");
            return;
        }
        
        System.out.print("Пароль: ");
        String password = scanner.nextLine().trim();
        
        if (password.isEmpty()) {
            System.out.println("Ошибка: Пароль не может быть пустым");
            return;
        }
        
        BankResponse response = client.register(login, password);
        System.out.println(response.getMessage());
    }
    
    private void showMainMenu() {
        while (loggedIn) {
            System.out.println("\n--- Главное меню ---");
            System.out.println("1. Создать счет");
            System.out.println("2. Удалить счет");
            System.out.println("3. Просмотреть счета");
            System.out.println("4. Получить баланс");
            System.out.println("5. Пополнить счет");
            System.out.println("6. Снять средства");
            System.out.println("7. Перевести средства");
            System.out.println("8. История транзакций");
            System.out.println("9. Выйти");
            System.out.print("Выберите действие: ");
            
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    createAccount();
                    break;
                case "2":
                    deleteAccount();
                    break;
                case "3":
                    viewAccounts();
                    break;
                case "4":
                    getBalance();
                    break;
                case "5":
                    deposit();
                    break;
                case "6":
                    withdraw();
                    break;
                case "7":
                    transfer();
                    break;
                case "8":
                    viewTransactions();
                    break;
                case "9":
                    logout();
                    return;
                default:
                    System.out.println("Неверный выбор");
            }
        }
    }
    
    private void createAccount() {
        System.out.print("Введите валюту (RUB/USD/EUR): ");
        String currency = scanner.nextLine().trim().toUpperCase();
        
        if (!isValidCurrency(currency)) {
            System.out.println("Ошибка: Неверная валюта. Допустимые значения: RUB, USD, EUR");
            return;
        }
        
        BankResponse response = client.createAccount(currency);
        System.out.println(response.getMessage());
    }
    
    private void deleteAccount() {
        Account account = selectAccount("Выберите счет для удаления");
        if (account == null) {
            return;
        }
        
        System.out.println("\nИнформация о счете:");
        System.out.println(formatAccount(account));
        
        if (account.getBalance() > 0) {
            System.out.println("\nВнимание! На счете остались средства.");
        }
        
        System.out.print("\nВы уверены, что хотите удалить этот счет? (да/нет): ");
        String confirmation = scanner.nextLine().trim().toLowerCase();
        
        if (!isConfirmationPositive(confirmation)) {
            System.out.println("Операция отменена");
            return;
        }
        
        BankResponse response = client.deleteAccount(account.getAccountNumber());
        System.out.println(response.getMessage());
    }
    
    private void viewAccounts() {
        BankResponse response = client.getAccounts();
        if (response.isSuccess() && response.getAccounts() != null) {
            List<Account> accounts = response.getAccounts();
            if (accounts.isEmpty()) {
                System.out.println("Счета не найдены");
            } else {
                System.out.println("\nВаши счета:");
                System.out.println("─".repeat(60));
                for (Account account : accounts) {
                    BankResponse balanceResponse = client.getBalance(account.getAccountNumber());
                    if (balanceResponse.isSuccess() && balanceResponse.getBalance() != null) {
                        System.out.println("Счет: " + account.getAccountNumber() + 
                                         " | Баланс: " + formatBalance(balanceResponse.getBalance(), account.getCurrency()) + 
                                         " | Валюта: " + account.getCurrency());
                    } else {
                        System.out.println(formatAccount(account));
                    }
                }
                System.out.println("─".repeat(60));
            }
        } else {
            System.out.println("Ошибка получения счетов: " + (response.getMessage() != null ? response.getMessage() : "Неизвестная ошибка"));
        }
    }
    
    private void getBalance() {
        Account account = selectAccount("Выберите счет для просмотра баланса");
        if (account == null) {
            return;
        }
        
        BankResponse response = client.getBalance(account.getAccountNumber());
        if (response.isSuccess() && response.getBalance() != null) {
            System.out.println("\nСчет: " + account.getAccountNumber() + 
                             " | Баланс: " + formatBalance(response.getBalance(), account.getCurrency()) + 
                             " | Валюта: " + account.getCurrency());
        } else {
            System.out.println(response.getMessage());
        }
    }
    
    private void deposit() {
        Account account = selectAccount("Выберите счет для пополнения");
        if (account == null) {
            return;
        }
        
        System.out.println("\nТекущий баланс: " + formatBalance(account.getBalance(), account.getCurrency()));
        
        double amount = readAmount("Введите сумму для пополнения");
        if (amount <= 0) {
            return;
        }
        
        BankResponse response = client.deposit(account.getAccountNumber(), amount);
        System.out.println(response.getMessage());
        
        if (response.isSuccess()) {
            BankResponse balanceResponse = client.getBalance(account.getAccountNumber());
            if (balanceResponse.isSuccess() && balanceResponse.getBalance() != null) {
                System.out.println("Новый баланс: " + formatBalance(
                    balanceResponse.getBalance(), account.getCurrency()));
            }
        }
    }
    
    private void withdraw() {
        Account account = selectAccount("Выберите счет для снятия средств");
        if (account == null) {
            return;
        }
        
        System.out.println("\nТекущий баланс: " + formatBalance(account.getBalance(), account.getCurrency()));
        
        double amount = readAmount("Введите сумму для снятия");
        if (amount <= 0) {
            return;
        }
        
        if (amount > account.getBalance()) {
            System.out.println("Ошибка: Недостаточно средств на счете");
            return;
        }
        
        BankResponse response = client.withdraw(account.getAccountNumber(), amount);
        System.out.println(response.getMessage());
        
        if (response.isSuccess()) {
            BankResponse balanceResponse = client.getBalance(account.getAccountNumber());
            if (balanceResponse.isSuccess() && balanceResponse.getBalance() != null) {
                System.out.println("Новый баланс: " + formatBalance(
                    balanceResponse.getBalance(), account.getCurrency()));
            }
        }
    }
    
    private void transfer() {
        Account fromAccount = selectAccount("Выберите счет для перевода (откуда)");
        if (fromAccount == null) {
            return;
        }
        
        BankResponse balanceResponse = client.getBalance(fromAccount.getAccountNumber());
        if (balanceResponse.isSuccess() && balanceResponse.getBalance() != null) {
            fromAccount.setBalance(balanceResponse.getBalance());
        }
        
        System.out.println("\nТекущий баланс: " + formatBalance(fromAccount.getBalance(), fromAccount.getCurrency()));
        
        System.out.print("Введите номер счета получателя: ");
        String toAccountNumber = scanner.nextLine().trim();
        
        if (toAccountNumber.isEmpty()) {
            System.out.println("Ошибка: Номер счета получателя не может быть пустым");
            return;
        }
        
        String toAccountCurrency = getAccountCurrency(toAccountNumber);
        
        if (toAccountCurrency == null) {
            System.out.println("Счет получателя принадлежит другому пользователю.");
            System.out.println("Валюта будет определена автоматически при переводе.");
            toAccountCurrency = "UNKNOWN";
        }
        
        double amount = readAmount("Введите сумму перевода");
        if (amount <= 0) {
            return;
        }
        
        if (amount > fromAccount.getBalance()) {
            System.out.println("Ошибка: Недостаточно средств на счете");
            return;
        }
        
        boolean needsConversion = false;
        double convertedAmount = amount;
        
        if (!toAccountCurrency.equals("UNKNOWN") && !fromAccount.getCurrency().equals(toAccountCurrency)) {
            needsConversion = true;
            convertedAmount = CurrencyConverter.convert(amount, fromAccount.getCurrency(), toAccountCurrency);
            
            System.out.println("\n⚠️  ВНИМАНИЕ: Перевод между разными валютами!");
            System.out.println("Курс обмена: " + CurrencyConverter.getExchangeRateString(
                fromAccount.getCurrency(), toAccountCurrency));
            System.out.println("Сумма к списанию: " + formatBalance(amount, fromAccount.getCurrency()));
            System.out.println("Сумма к зачислению: " + formatBalance(convertedAmount, toAccountCurrency));
        } else if (toAccountCurrency.equals("UNKNOWN")) {
            System.out.println("\n⚠️  ВНИМАНИЕ: Если валюты счетов различаются, будет выполнена конвертация.");
        }
        
        System.out.println("\nДетали перевода:");
        System.out.println("От: " + formatAccount(fromAccount));
        if (!toAccountCurrency.equals("UNKNOWN")) {
            System.out.println("Кому: " + toAccountNumber + " (" + toAccountCurrency + ")");
        } else {
            System.out.println("Кому: " + toAccountNumber);
        }
        System.out.println("Сумма: " + formatBalance(amount, fromAccount.getCurrency()));
        if (needsConversion) {
            System.out.println("Сумма получателю: " + formatBalance(convertedAmount, toAccountCurrency));
        }
        
        System.out.print("\nПодтвердите перевод (да/нет): ");
        String confirmation = scanner.nextLine().trim().toLowerCase();
        
        if (!isConfirmationPositive(confirmation)) {
            System.out.println("Операция отменена");
            return;
        }
        
        BankResponse response;
        if (toAccountCurrency.equals("UNKNOWN")) {
            response = client.transfer(fromAccount.getAccountNumber(), toAccountNumber, amount);
        } else {
            response = client.transfer(fromAccount.getAccountNumber(), toAccountNumber, amount, 
                fromAccount.getCurrency(), toAccountCurrency);
        }
        System.out.println(response.getMessage());
        
        if (response.isSuccess()) {
            BankResponse newBalanceResponse = client.getBalance(fromAccount.getAccountNumber());
            if (newBalanceResponse.isSuccess() && newBalanceResponse.getBalance() != null) {
                System.out.println("Новый баланс на вашем счете: " + formatBalance(
                    newBalanceResponse.getBalance(), fromAccount.getCurrency()));
            }
        }
    }
    
    private String getAccountCurrency(String accountNumber) {
        BankResponse accountsResponse = client.getAccounts();
        if (accountsResponse.isSuccess() && accountsResponse.getAccounts() != null) {
            for (Account acc : accountsResponse.getAccounts()) {
                if (acc.getAccountNumber().equals(accountNumber)) {
                    return acc.getCurrency();
                }
            }
        }
        return null;
    }
    
    private void viewTransactions() {
        Account account = selectAccount("Выберите счет для просмотра истории транзакций");
        if (account == null) {
            return;
        }
        
        BankResponse response = client.getTransactions(account.getAccountNumber());
        if (response.isSuccess() && response.getTransactions() != null) {
            List<Transaction> transactions = response.getTransactions();
            if (transactions.isEmpty()) {
                System.out.println("\nИстория транзакций пуста");
            } else {
                System.out.println("\nИстория транзакций для счета " + account.getAccountNumber() + ":");
                System.out.println("─".repeat(80));
                for (Transaction transaction : transactions) {
                    System.out.println(transaction);
                }
                System.out.println("─".repeat(80));
            }
        } else {
            System.out.println(response.getMessage());
        }
    }
    
    private Account selectAccount(String prompt) {
        BankResponse response = client.getAccounts();
        if (!response.isSuccess() || response.getAccounts() == null || response.getAccounts().isEmpty()) {
            System.out.println("У вас нет счетов");
            return null;
        }
        
        List<Account> accounts = response.getAccounts();
        System.out.println("\n" + prompt + ":");
        System.out.println("─".repeat(60));
        for (int i = 0; i < accounts.size(); i++) {
            Account account = accounts.get(i);
            BankResponse balanceResponse = client.getBalance(account.getAccountNumber());
            if (balanceResponse.isSuccess() && balanceResponse.getBalance() != null) {
                System.out.println((i + 1) + ". Счет: " + account.getAccountNumber() + 
                                 " | Баланс: " + formatBalance(balanceResponse.getBalance(), account.getCurrency()) + 
                                 " | Валюта: " + account.getCurrency());
            } else {
                System.out.println((i + 1) + ". " + formatAccount(account));
            }
        }
        System.out.println("─".repeat(60));
        System.out.print("Выберите номер счета (1-" + accounts.size() + "): ");
        
        String choice = scanner.nextLine().trim();
        try {
            int index = Integer.parseInt(choice) - 1;
            if (index >= 0 && index < accounts.size()) {
                Account selectedAccount = accounts.get(index);
                BankResponse balanceResponse = client.getBalance(selectedAccount.getAccountNumber());
                if (balanceResponse.isSuccess() && balanceResponse.getBalance() != null) {
                    selectedAccount.setBalance(balanceResponse.getBalance());
                }
                return selectedAccount;
            } else {
                System.out.println("Неверный номер счета");
                return null;
            }
        } catch (NumberFormatException e) {
            System.out.println("Ошибка: Введите число");
            return null;
        }
    }
    
    private double readAmount(String prompt) {
        System.out.print(prompt + ": ");
        String amountStr = scanner.nextLine().trim();
        
        if (amountStr.isEmpty()) {
            System.out.println("Ошибка: Сумма не может быть пустой");
            return -1;
        }
        
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                System.out.println("Ошибка: Сумма должна быть положительным числом");
                return -1;
            }
            return amount;
        } catch (NumberFormatException e) {
            System.out.println("Ошибка: Неверный формат суммы. Введите число");
            return -1;
        }
    }
    
    private boolean isValidCurrency(String currency) {
        return currency != null && (currency.equals("RUB") || currency.equals("USD") || currency.equals("EUR"));
    }
    
    private boolean isConfirmationPositive(String confirmation) {
        if (confirmation == null || confirmation.isEmpty()) {
            return false;
        }
        
        String trimmed = confirmation.trim();
        
        if (trimmed.length() == 0) {
            return false;
        }
        
        char firstChar = trimmed.charAt(0);
        String lowerTrimmed = trimmed.toLowerCase();
        
        if (firstChar == 'д' || firstChar == 'Д' || 
            firstChar == '\u0434' || firstChar == '\u0414') {
            return true;
        }
        
        if (lowerTrimmed.equals("да") || lowerTrimmed.startsWith("да")) {
            return true;
        }
        
        if (firstChar == 'y' || firstChar == 'Y') {
            return true;
        }
        if (lowerTrimmed.equals("yes") || lowerTrimmed.startsWith("yes")) {
            return true;
        }
        
        return false;
    }
    
    private String formatAccount(Account account) {
        return String.format("Счет: %s | Баланс: %s | Валюта: %s", 
            account.getAccountNumber(), 
            formatBalance(account.getBalance(), account.getCurrency()),
            account.getCurrency());
    }
    
    private String formatBalance(double balance, String currency) {
        return currencyFormatter.format(balance) + " " + currency;
    }
    
    private void logout() {
        BankResponse response = client.logout();
        System.out.println(response.getMessage());
        loggedIn = false;
    }
    
    public static void main(String[] args) {
        try {
            System.setOut(new java.io.PrintStream(System.out, true, StandardCharsets.UTF_8.name()));
            System.setErr(new java.io.PrintStream(System.err, true, StandardCharsets.UTF_8.name()));
            
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                try {
                    new ProcessBuilder("cmd", "/c", "chcp", "65001").inheritIO().start().waitFor();
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
            System.err.println("Предупреждение: не удалось установить UTF-8 кодировку");
        }
        
        ClientUI ui = new ClientUI();
        ui.start();
    }
}
