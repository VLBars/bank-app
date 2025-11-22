package server;

import utils.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BankServer {
    private int port;
    private BankService bankService;
    private Logger logger;
    private ExecutorService threadPool;
    private boolean running;
    
    public BankServer(int port) {
        this.port = port;
        this.logger = new Logger("BankServer", Config.SERVER_LOG_FILE);
        this.bankService = new BankService(logger);
        this.threadPool = Executors.newCachedThreadPool();
    }
    
    public void start() {
        running = true;
        logger.info("Сервер банка запускается на порту " + port);
        
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("Сервер банка успешно запущен");
            
            while (running) {
                Socket clientSocket = serverSocket.accept();
                logger.info("Новое подключение: " + clientSocket.getInetAddress());
                
                ClientHandler clientHandler = new ClientHandler(clientSocket, bankService, logger);
                threadPool.execute(clientHandler);
            }
        } catch (IOException e) {
            logger.error("Ошибка сервера: " + e.getMessage());
        } finally {
            threadPool.shutdown();
            logger.info("Сервер банка остановлен");
        }
    }
    
    public void stop() {
        running = false;
    }
    
    public static void main(String[] args) {
        BankServer server = new BankServer(Config.SERVER_PORT);
        server.start();
    }
}