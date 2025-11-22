package server;

import common.*;
import utils.Config;
import utils.Logger;
import utils.PasswordHasher;
import utils.CurrencyConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class BankService {
    private Map<String, User> users;
    private Map<String, List<Account>> accounts;
    private Map<String, List<Transaction>> transactions;
    private Logger logger;
    private Gson gson;
    private static final AtomicLong accountCounter = new AtomicLong(System.currentTimeMillis());
    
    public BankService(Logger logger) {
        this.logger = logger;
        this.gson = new Gson();
        this.users = new ConcurrentHashMap<>();
        this.accounts = new ConcurrentHashMap<>();
        this.transactions = new ConcurrentHashMap<>();
        ensureDataDirectories();
        loadData();
        migratePasswordsToHashed();
    }
    
    private void ensureDataDirectories() {
        try {
            Files.createDirectories(Paths.get(Config.USER_DATA_FILE).getParent());
            Files.createDirectories(Paths.get(Config.ACCOUNT_DATA_FILE).getParent());
            Files.createDirectories(Paths.get(Config.TRANSACTION_DATA_FILE).getParent());
        } catch (IOException e) {
            logger.warn("Не удалось создать директории для данных: " + e.getMessage());
        }
    }
    
    private void loadData() {
        // Загрузка пользователей
        try (Reader reader = Files.newBufferedReader(Paths.get(Config.USER_DATA_FILE), StandardCharsets.UTF_8)) {
            Type userType = new TypeToken<Map<String, User>>(){}.getType();
            Map<String, User> loadedUsers = gson.fromJson(reader, userType);
            if (loadedUsers != null) {
                users.putAll(loadedUsers);
            }
        } catch (IOException e) {
            logger.warn("Не удалось загрузить пользователей: " + e.getMessage());
        }
        
        // Загрузка счетов
        try (Reader reader = Files.newBufferedReader(Paths.get(Config.ACCOUNT_DATA_FILE), StandardCharsets.UTF_8)) {
            Type accountType = new TypeToken<Map<String, List<Account>>>(){}.getType();
            Map<String, List<Account>> loadedAccounts = gson.fromJson(reader, accountType);
            if (loadedAccounts != null) {
                accounts.putAll(loadedAccounts);
            }
        } catch (IOException e) {
            logger.warn("Не удалось загрузить счета: " + e.getMessage());
        }
        
        // Загрузка транзакций
        try (Reader reader = Files.newBufferedReader(Paths.get(Config.TRANSACTION_DATA_FILE), StandardCharsets.UTF_8)) {
            Type transactionType = new TypeToken<Map<String, List<Transaction>>>(){}.getType();
            Map<String, List<Transaction>> loadedTransactions = gson.fromJson(reader, transactionType);
            if (loadedTransactions != null) {
                transactions.putAll(loadedTransactions);
            }
        } catch (IOException e) {
            logger.warn("Не удалось загрузить транзакции: " + e.getMessage());
        }
    }
    
    private void saveData() {
        // Сохранение пользователей
        try (Writer writer = Files.newBufferedWriter(Paths.get(Config.USER_DATA_FILE), StandardCharsets.UTF_8)) {
            gson.toJson(users, writer);
        } catch (IOException e) {
            logger.error("Ошибка сохранения пользователей: " + e.getMessage());
        }
        
        // Сохранение счетов
        try (Writer writer = Files.newBufferedWriter(Paths.get(Config.ACCOUNT_DATA_FILE), StandardCharsets.UTF_8)) {
            gson.toJson(accounts, writer);
        } catch (IOException e) {
            logger.error("Ошибка сохранения счетов: " + e.getMessage());
        }
        
        // Сохранение транзакций
        try (Writer writer = Files.newBufferedWriter(Paths.get(Config.TRANSACTION_DATA_FILE), StandardCharsets.UTF_8)) {
            gson.toJson(transactions, writer);
        } catch (IOException e) {
            logger.error("Ошибка сохранения транзакций: " + e.getMessage());
        }
    }
    
    // Миграция паролей в хешированные (для существующих пользователей)
    private void migratePasswordsToHashed() {
        boolean needsSave = false;
        for (User user : users.values()) {
            String password = user.getPassword();
            // Если пароль не хеширован (не начинается с цифр/букв хеша SHA-256)
            if (password.length() < 64 || !password.matches("[0-9a-f]{64}")) {
                String hashedPassword = PasswordHasher.hash(password);
                users.put(user.getLogin(), new User(user.getLogin(), hashedPassword));
                needsSave = true;
            }
        }
        if (needsSave) {
            saveData();
        }
    }
    
    public BankResponse register(String login, String password) {
        if (login == null || login.trim().isEmpty()) {
            return new BankResponse(false, "Логин не может быть пустым");
        }
        
        if (password == null || password.trim().isEmpty()) {
            return new BankResponse(false, "Пароль не может быть пустым");
        }
        
        if (users.containsKey(login)) {
            return new BankResponse(false, "Пользователь с таким логином уже существует");
        }
        
        String hashedPassword = PasswordHasher.hash(password);
        users.put(login, new User(login, hashedPassword));
        accounts.put(login, new ArrayList<>());
        transactions.put(login, new ArrayList<>());
        
        saveData();
        logger.info("Зарегистрирован новый пользователь: " + login);
        return new BankResponse(true, "Регистрация успешна");
    }
    
    public BankResponse authenticate(String login, String password) {
        User user = users.get(login);
        if (user != null) {
            // Проверяем хешированный пароль
            if (PasswordHasher.verify(password, user.getPassword())) {
                logger.info("Пользователь " + login + " успешно авторизован");
                return new BankResponse(true, "Авторизация успешна");
            }
        }
        logger.warn("Неудачная попытка авторизации для пользователя: " + login);
        return new BankResponse(false, "Неверный логин или пароль");
    }
    
    public BankResponse createAccount(String login, String currency) {
        if (currency == null || currency.trim().isEmpty()) {
            return new BankResponse(false, "Валюта не может быть пустой");
        }
        
        String currencyUpper = currency.trim().toUpperCase();
        if (!currencyUpper.equals("RUB") && !currencyUpper.equals("USD") && !currencyUpper.equals("EUR")) {
            return new BankResponse(false, "Неверная валюта. Допустимые значения: RUB, USD, EUR");
        }
        
        if (!accounts.containsKey(login)) {
            accounts.put(login, new ArrayList<>());
        }
        
        String accountNumber = generateAccountNumber();
        Account newAccount = new Account(accountNumber, login, 0.0, currencyUpper);
        accounts.get(login).add(newAccount);
        
        saveData();
        logger.info("Создан новый счет " + accountNumber + " для пользователя " + login);
        
        return new BankResponse(true, "Счет успешно создан: " + accountNumber);
    }
    
    public BankResponse deleteAccount(String login, String accountNumber) {
        List<Account> userAccounts = accounts.get(login);
        if (userAccounts != null) {
            Account account = userAccounts.stream()
                    .filter(acc -> acc.getAccountNumber().equals(accountNumber))
                    .findFirst()
                    .orElse(null);
            
            if (account != null) {
                if (account.getBalance() > 0) {
                    return new BankResponse(false, 
                        String.format("Невозможно удалить счет. На счете осталось средств: %.2f %s", 
                            account.getBalance(), account.getCurrency()));
                }
                
                userAccounts.remove(account);
                saveData();
                logger.info("Счет " + accountNumber + " удален для пользователя " + login);
                return new BankResponse(true, "Счет успешно удален");
            }
        }
        return new BankResponse(false, "Счет не найден");
    }
    
    public BankResponse getBalance(String login, String accountNumber) {
        Account account = findAccount(login, accountNumber);
        if (account != null) {
            BankResponse response = new BankResponse(true, "Баланс получен");
            response.setBalance(account.getBalance());
            return response;
        }
        return new BankResponse(false, "Счет не найден");
    }
    
    public BankResponse deposit(String login, String accountNumber, double amount) {
        Account account = findAccount(login, accountNumber);
        if (account != null && amount > 0) {
            account.deposit(amount);
            addTransaction(accountNumber, "DEPOSIT", amount, account.getCurrency(), 
                "Пополнение счета");
            saveData();
            logger.info("Пополнение счета " + accountNumber + " на сумму " + amount);
            return new BankResponse(true, "Счет успешно пополнен");
        }
        return new BankResponse(false, "Ошибка пополнения счета");
    }
    
    public BankResponse withdraw(String login, String accountNumber, double amount) {
        Account account = findAccount(login, accountNumber);
        if (account != null && amount > 0 && account.getBalance() >= amount) {
            account.withdraw(amount);
            addTransaction(accountNumber, "WITHDRAW", amount, account.getCurrency(), 
                "Снятие средств");
            saveData();
            logger.info("Снятие со счета " + accountNumber + " суммы " + amount);
            return new BankResponse(true, "Средства успешно сняты");
        }
        return new BankResponse(false, "Недостаточно средств или счет не найден");
    }
    
    public BankResponse transfer(String login, String fromAccount, String toAccount, double amount) {
        Account source = findAccount(login, fromAccount);
        Account target = findAccountByNumber(toAccount);
        
        if (source == null) {
            return new BankResponse(false, "Исходный счет не найден");
        }
        
        if (target == null) {
            return new BankResponse(false, "Счет получателя не найден");
        }
        
        if (amount <= 0) {
            return new BankResponse(false, "Сумма должна быть положительной");
        }
        
        if (source.getBalance() < amount) {
            return new BankResponse(false, "Недостаточно средств на счете");
        }
        
        // Проверяем, нужна ли конвертация валют
        boolean needsConversion = !source.getCurrency().equals(target.getCurrency());
        double convertedAmount = amount;
        
        if (needsConversion) {
            // Конвертация валют
            if (!CurrencyConverter.isCurrencySupported(source.getCurrency()) || 
                !CurrencyConverter.isCurrencySupported(target.getCurrency())) {
                return new BankResponse(false, 
                    String.format("Одна из валют не поддерживается для конвертации. Исходный счет: %s, Счет получателя: %s", 
                        source.getCurrency(), target.getCurrency()));
            }
            convertedAmount = CurrencyConverter.convert(amount, source.getCurrency(), target.getCurrency());
        }
        
        // Выполняем перевод
        source.withdraw(amount);
        target.deposit(convertedAmount);
        
        // Добавляем транзакции
        String transferDescription = needsConversion
            ? String.format("Перевод на счет %s (конвертация: %.2f %s -> %.2f %s)", 
                toAccount, amount, source.getCurrency(), convertedAmount, target.getCurrency())
            : "Перевод на счет " + toAccount;
        
        addTransaction(fromAccount, "TRANSFER_OUT", amount, source.getCurrency(), transferDescription);
        addTransaction(toAccount, "TRANSFER_IN", convertedAmount, target.getCurrency(), 
            needsConversion
                ? String.format("Перевод со счета %s (конвертация: %.2f %s -> %.2f %s)", 
                    fromAccount, amount, source.getCurrency(), convertedAmount, target.getCurrency())
                : "Перевод со счета " + fromAccount);
        
        saveData();
        if (needsConversion) {
            logger.info(String.format("Перевод с конвертацией: %.2f %s -> %.2f %s с %s на %s", 
                amount, source.getCurrency(), convertedAmount, target.getCurrency(), fromAccount, toAccount));
            return new BankResponse(true, 
                String.format("Перевод выполнен успешно. Конвертировано: %.2f %s -> %.2f %s", 
                    amount, source.getCurrency(), convertedAmount, target.getCurrency()));
        } else {
            logger.info("Перевод " + amount + " " + source.getCurrency() + " с " + fromAccount + " на " + toAccount);
            return new BankResponse(true, "Перевод выполнен успешно");
        }
    }
    
    // Перегруженный метод transfer с поддержкой конвертации валют
    public BankResponse transfer(String login, String fromAccount, String toAccount, double amount,
                                String fromCurrency, String toCurrency) {
        Account source = findAccount(login, fromAccount);
        Account target = findAccountByNumber(toAccount);
        
        if (source == null) {
            return new BankResponse(false, "Исходный счет не найден");
        }
        
        if (target == null) {
            return new BankResponse(false, "Счет получателя не найден");
        }
        
        // Проверяем, что переданные валюты соответствуют валютам счетов
        if (!source.getCurrency().equals(fromCurrency)) {
            return new BankResponse(false, "Валюта исходного счета не совпадает");
        }
        
        if (!target.getCurrency().equals(toCurrency)) {
            return new BankResponse(false, "Валюта счета получателя не совпадает");
        }
        
        if (amount <= 0) {
            return new BankResponse(false, "Сумма должна быть положительной");
        }
        
        if (source.getBalance() < amount) {
            return new BankResponse(false, "Недостаточно средств на счете");
        }
        
        // Конвертация валют, если необходимо
        double convertedAmount = amount;
        if (!fromCurrency.equals(toCurrency)) {
            if (!CurrencyConverter.isCurrencySupported(fromCurrency) || 
                !CurrencyConverter.isCurrencySupported(toCurrency)) {
                return new BankResponse(false, "Одна из валют не поддерживается для конвертации");
            }
            convertedAmount = CurrencyConverter.convert(amount, fromCurrency, toCurrency);
        }
        
        // Выполняем перевод
        source.withdraw(amount);
        target.deposit(convertedAmount);
        
        // Добавляем транзакции
        String transferDescription = fromCurrency.equals(toCurrency) 
            ? "Перевод на счет " + toAccount
            : String.format("Перевод на счет %s (конвертация: %.2f %s -> %.2f %s)", 
                toAccount, amount, fromCurrency, convertedAmount, toCurrency);
        
        addTransaction(fromAccount, "TRANSFER_OUT", amount, source.getCurrency(), transferDescription);
        addTransaction(toAccount, "TRANSFER_IN", convertedAmount, target.getCurrency(), 
            String.format("Перевод со счета %s (конвертация: %.2f %s -> %.2f %s)", 
                fromAccount, amount, fromCurrency, convertedAmount, toCurrency));
        
        saveData();
        if (fromCurrency.equals(toCurrency)) {
            logger.info("Перевод " + amount + " " + source.getCurrency() + " с " + fromAccount + " на " + toAccount);
        } else {
            logger.info(String.format("Перевод с конвертацией: %.2f %s -> %.2f %s с %s на %s", 
                amount, fromCurrency, convertedAmount, toCurrency, fromAccount, toAccount));
        }
        return new BankResponse(true, "Перевод выполнен успешно");
    }
    
    public BankResponse getAccounts(String login) {
        // Получаем актуальный список счетов из памяти
        List<Account> userAccounts = accounts.get(login);
        BankResponse response = new BankResponse(true, "Счета получены");
        // Создаем новый список, чтобы гарантировать актуальность данных
        if (userAccounts != null) {
            response.setAccounts(new ArrayList<>(userAccounts));
        } else {
            response.setAccounts(new ArrayList<>());
        }
        return response;
    }
    
    public String getAccountCurrency(String accountNumber) {
        Account account = findAccountByNumber(accountNumber);
        return account != null ? account.getCurrency() : null;
    }
    
    public BankResponse getTransactions(String login, String accountNumber) {
        List<Transaction> accountTransactions = transactions.getOrDefault(login, new ArrayList<>())
                .stream()
                .filter(t -> t.getAccountNumber().equals(accountNumber))
                .sorted((t1, t2) -> t2.getTimestampAsDateTime().compareTo(t1.getTimestampAsDateTime()))
                .collect(Collectors.toList());
        
        BankResponse response = new BankResponse(true, "История транзакций получена");
        response.setTransactions(accountTransactions);
        return response;
    }
    
    private void addTransaction(String accountNumber, String type, double amount, String currency, String description) {
        String owner = findOwnerByAccountNumber(accountNumber);
        if (owner != null) {
            if (!transactions.containsKey(owner)) {
                transactions.put(owner, new ArrayList<>());
            }
            Transaction transaction = new Transaction(accountNumber, type, amount, currency, description);
            transactions.get(owner).add(transaction);
        }
    }
    
    private String findOwnerByAccountNumber(String accountNumber) {
        for (Map.Entry<String, List<Account>> entry : accounts.entrySet()) {
            for (Account account : entry.getValue()) {
                if (account.getAccountNumber().equals(accountNumber)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }
    
    private Account findAccount(String login, String accountNumber) {
        List<Account> userAccounts = accounts.get(login);
        if (userAccounts != null) {
            return userAccounts.stream()
                    .filter(acc -> acc.getAccountNumber().equals(accountNumber))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
    
    private Account findAccountByNumber(String accountNumber) {
        for (List<Account> accountList : accounts.values()) {
            for (Account account : accountList) {
                if (account.getAccountNumber().equals(accountNumber)) {
                    return account;
                }
            }
        }
        return null;
    }
    
    private String generateAccountNumber() {
        return "ACC" + accountCounter.incrementAndGet();
    }
}
