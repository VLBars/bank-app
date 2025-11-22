package client;

import common.*;
import utils.Config;
import utils.Logger;
import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class BankClient {
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private Logger logger;
    private String currentUser;
    private String host;
    private int port;
    private boolean connected;
    
    public BankClient(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        this.logger = new Logger("BankClient", Config.CLIENT_LOG_FILE);
        connect();
    }
    
    private void connect() throws IOException {
        try {
            socket = new Socket(host, port);
            socket.setSoTimeout(Config.OPERATION_TIMEOUT);
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());
            connected = true;
            logger.info("Подключение к серверу установлено");
        } catch (IOException e) {
            connected = false;
            throw e;
        }
    }
    
    private boolean ensureConnected() {
        if (!connected || socket == null || socket.isClosed()) {
            try {
                connect();
                return true;
            } catch (IOException e) {
                logger.error("Не удалось переподключиться: " + e.getMessage());
                return false;
            }
        }
        return true;
    }
    
    public BankResponse register(String login, String password) {
        return executeOperation(BankOperation.REGISTER, new User(login, password), false);
    }
    
    public BankResponse login(String login, String password) {
        BankResponse response = executeOperation(BankOperation.LOGIN, new User(login, password), false);
        if (response.isSuccess()) {
            currentUser = login;
            logger.info("Успешный вход пользователя: " + login);
        } else {
            logger.warn("Неудачный вход пользователя: " + login);
        }
        return response;
    }
    
    public BankResponse createAccount(String currency) {
        return executeOperation(BankOperation.CREATE_ACCOUNT, currency, true);
    }
    
    public BankResponse deleteAccount(String accountNumber) {
        return executeOperation(BankOperation.DELETE_ACCOUNT, accountNumber, true);
    }
    
    public BankResponse getBalance(String accountNumber) {
        return executeOperation(BankOperation.GET_BALANCE, accountNumber, true);
    }
    
    public BankResponse deposit(String accountNumber, double amount) {
        return executeOperation(BankOperation.DEPOSIT, new Object[]{accountNumber, amount}, true);
    }
    
    public BankResponse withdraw(String accountNumber, double amount) {
        return executeOperation(BankOperation.WITHDRAW, new Object[]{accountNumber, amount}, true);
    }
    
    public BankResponse transfer(String fromAccount, String toAccount, double amount) {
        return executeOperation(BankOperation.TRANSFER, new Object[]{fromAccount, toAccount, amount}, true);
    }
    
    public BankResponse transfer(String fromAccount, String toAccount, double amount, 
                                 String fromCurrency, String toCurrency) {
        return executeOperation(BankOperation.TRANSFER, 
            new Object[]{fromAccount, toAccount, amount, fromCurrency, toCurrency}, true);
    }
    
    public BankResponse getAccountInfo(String accountNumber) {
        // Получаем информацию о счете (для определения валюты)
        // Используем getBalance, но нам нужна валюта
        // Временно используем обходной путь - получаем все счета
        return executeOperation(BankOperation.GET_ACCOUNTS, null, true);
    }
    
    public BankResponse getAccounts() {
        return executeOperation(BankOperation.GET_ACCOUNTS, null, true);
    }
    
    public BankResponse getTransactions(String accountNumber) {
        return executeOperation(BankOperation.GET_TRANSACTIONS, accountNumber, true);
    }
    
    public BankResponse logout() {
        BankResponse response = executeOperation(BankOperation.LOGOUT, null, true);
        if (currentUser != null) {
            logger.info("Пользователь " + currentUser + " вышел из системы");
            currentUser = null;
        }
        disconnect();
        return response;
    }
    
    private BankResponse executeOperation(BankOperation operation, Object data, boolean requiresAuth) {
        if (requiresAuth && !ensureConnected()) {
            return new BankResponse(false, "Нет соединения с сервером");
        }
        
        int retries = 2;
        while (retries >= 0) {
            try {
                if (!ensureConnected()) {
                    return new BankResponse(false, "Не удалось подключиться к серверу");
                }
                
                output.writeObject(operation);
                if (data != null) {
                    output.writeObject(data);
                }
                output.flush();
                
                BankResponse response = (BankResponse) input.readObject();
                String userInfo = currentUser != null ? " (пользователь: " + currentUser + ")" : "";
                logger.info("Операция " + operation + userInfo + ": " + response.getMessage());
                return response;
                
            } catch (SocketTimeoutException e) {
                logger.warn("Таймаут операции " + operation);
                if (retries > 0) {
                    connected = false;
                    retries--;
                    continue;
                }
                return new BankResponse(false, "Таймаут операции. Попробуйте позже");
                
            } catch (IOException | ClassNotFoundException e) {
                logger.error("Ошибка операции " + operation + ": " + e.getMessage());
                connected = false;
                
                if (retries > 0) {
                    try {
                        Thread.sleep(1000); // Пауза перед переподключением
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    retries--;
                    continue;
                }
                
                return new BankResponse(false, "Ошибка соединения: " + e.getMessage());
            }
        }
        
        return new BankResponse(false, "Не удалось выполнить операцию после нескольких попыток");
    }
    
    private void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            connected = false;
            logger.info("Отключение от сервера");
        } catch (IOException e) {
            logger.error("Ошибка отключения: " + e.getMessage());
        }
    }
    
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
}
