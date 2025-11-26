package common;

import java.io.Serializable;
import java.util.List;

public class BankResponse implements Serializable {
    private static final long serialVersionUID = 3L;
    private boolean success;
    private String message;
    private List<Account> accounts;
    private Double balance;
    private List<Transaction> transactions;
    
    public BankResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    // Методы доступа (геттеры и сеттеры)
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public List<Account> getAccounts() { return accounts; }
    public void setAccounts(List<Account> accounts) { this.accounts = accounts; }
    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }
    public List<Transaction> getTransactions() { return transactions; }
    public void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }
}