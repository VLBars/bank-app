package utils;

public class Config {
    public static final int SERVER_PORT = 12345;
    public static final String SERVER_HOST = "localhost";
    public static final String USER_DATA_FILE = "data/users.json";
    public static final String ACCOUNT_DATA_FILE = "data/accounts.json";
    public static final String TRANSACTION_DATA_FILE = "data/transactions.json";
    public static final String SERVER_LOG_FILE = "logs/server.log";
    public static final String CLIENT_LOG_FILE = "logs/client.log";
    public static final int CONNECTION_TIMEOUT = 30000; // 30 секунд
    public static final int OPERATION_TIMEOUT = 10000; // 10 секунд
}