package common;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Transaction implements Serializable {
    private static final long serialVersionUID = 4L;
    private String id;
    private String accountNumber;
    private String type; // Тип операции: DEPOSIT, WITHDRAW, TRANSFER_IN, TRANSFER_OUT
    private double amount;
    private String currency;
    private String timestamp; // Сохраняем как строку для Gson
    private String description;
    
    // Конструктор для создания новой транзакции
    public Transaction(String accountNumber, String type, double amount, String currency, String description) {
        this.id = "TXN" + System.currentTimeMillis() + (int)(Math.random() * 1000);
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        this.description = description;
    }
    
    // Конструктор по умолчанию (нужен для Gson)
    public Transaction() {
    }
    
    // Методы доступа (геттеры и сеттеры, нужны для Gson)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    // Метод для получения LocalDateTime из строки
    public LocalDateTime getTimestampAsDateTime() {
        if (timestamp == null) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
    
    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        LocalDateTime dateTime = getTimestampAsDateTime();
        String typeName = switch(type) {
            case "DEPOSIT" -> "Пополнение";
            case "WITHDRAW" -> "Снятие";
            case "TRANSFER_IN" -> "Перевод (входящий)";
            case "TRANSFER_OUT" -> "Перевод (исходящий)";
            default -> type;
        };
        return String.format("[%s] %s: %.2f %s - %s", 
            dateTime.format(formatter), typeName, amount, currency, description);
    }
}

