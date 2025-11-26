package common;

import java.io.Serializable;

public class Account implements Serializable {
    private static final long serialVersionUID = 1L;
    private String accountNumber;
    private String owner;
    private double balance;
    private String currency;
    
    public Account(String accountNumber, String owner, double balance, String currency) {
        this.accountNumber = accountNumber;
        this.owner = owner;
        this.balance = balance;
        this.currency = currency;
    }
    
    // Методы доступа (геттеры и сеттеры)
    public String getAccountNumber() { return accountNumber; }
    public String getOwner() { return owner; }
    public double getBalance() { return balance; }
    public String getCurrency() { return currency; }
    
    public void deposit(double amount) { balance += amount; }
    public void withdraw(double amount) { balance -= amount; }
    
    @Override
    public String toString() {
        return String.format("Счет: %s, Баланс: %.2f %s", accountNumber, balance, currency);
    }
}