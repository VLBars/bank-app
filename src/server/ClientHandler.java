package server;

import common.*;
import utils.Logger;
import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private BankService bankService;
    private Logger logger;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private String currentUser;
    
    public ClientHandler(Socket socket, BankService bankService, Logger logger) {
        this.clientSocket = socket;
        this.bankService = bankService;
        this.logger = logger;
    }
    
    @Override
    public void run() {
        try {
            output = new ObjectOutputStream(clientSocket.getOutputStream());
            input = new ObjectInputStream(clientSocket.getInputStream());
            
            while (true) {
                BankOperation operation = (BankOperation) input.readObject();
                BankResponse response = processOperation(operation);
                output.writeObject(response);
                output.flush();
                
                if (operation == BankOperation.LOGOUT) {
                    break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            logger.error("Ошибка обработки клиента: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                logger.info("Клиент отключен: " + currentUser);
            } catch (IOException e) {
                logger.error("Ошибка закрытия соединения: " + e.getMessage());
            }
        }
    }
    
    private BankResponse processOperation(BankOperation operation) throws IOException, ClassNotFoundException {
        // Операции, не требующие авторизации
        if (operation == BankOperation.REGISTER || operation == BankOperation.LOGIN) {
            switch (operation) {
                case REGISTER:
                    User newUser = (User) input.readObject();
                    return bankService.register(newUser.getLogin(), newUser.getPassword());
                    
                case LOGIN:
                    User user = (User) input.readObject();
                    BankResponse loginResponse = bankService.authenticate(user.getLogin(), user.getPassword());
                    if (loginResponse.isSuccess()) {
                        currentUser = user.getLogin();
                    }
                    return loginResponse;
                    
                default:
                    return new BankResponse(false, "Неизвестная операция");
            }
        }
        
        // Проверка авторизации для всех остальных операций
        if (currentUser == null) {
            return new BankResponse(false, "Требуется авторизация");
        }
        
        switch (operation) {
            case CREATE_ACCOUNT:
                String currency = (String) input.readObject();
                return bankService.createAccount(currentUser, currency);
                
            case DELETE_ACCOUNT:
                String accountToDelete = (String) input.readObject();
                return bankService.deleteAccount(currentUser, accountToDelete);
                
            case GET_BALANCE:
                String accountForBalance = (String) input.readObject();
                return bankService.getBalance(currentUser, accountForBalance);
                
            case DEPOSIT:
                Object[] depositData = (Object[]) input.readObject();
                return bankService.deposit(currentUser, (String) depositData[0], (Double) depositData[1]);
                
            case WITHDRAW:
                Object[] withdrawData = (Object[]) input.readObject();
                return bankService.withdraw(currentUser, (String) withdrawData[0], (Double) withdrawData[1]);
                
            case TRANSFER:
                Object[] transferData = (Object[]) input.readObject();
                if (transferData.length == 5) {
                    // Перевод с конвертацией валют
                    return bankService.transfer(currentUser, (String) transferData[0], 
                                              (String) transferData[1], (Double) transferData[2],
                                              (String) transferData[3], (String) transferData[4]);
                } else {
                    // Обычный перевод (обратная совместимость)
                    return bankService.transfer(currentUser, (String) transferData[0], 
                                              (String) transferData[1], (Double) transferData[2]);
                }
                
            case GET_ACCOUNTS:
                return bankService.getAccounts(currentUser);
                
            case GET_TRANSACTIONS:
                String accountForTransactions = (String) input.readObject();
                return bankService.getTransactions(currentUser, accountForTransactions);
                
            case LOGOUT:
                currentUser = null;
                return new BankResponse(true, "Выход выполнен");
                
            default:
                return new BankResponse(false, "Неизвестная операция");
        }
    }
}
