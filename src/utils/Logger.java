package utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private String logFile;
    private String name;
    
    public Logger(String name, String logFile) {
        this.name = name;
        this.logFile = logFile;
    }
    
    public void log(String level, String message) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String logMessage = String.format("[%s] %s [%s] %s", timestamp, level, name, message);
        
        // Вывод в консоль
        System.out.println(logMessage);
        
        // Запись в файл
        try (PrintWriter out = new PrintWriter(new FileWriter(logFile, true))) {
            out.println(logMessage);
        } catch (IOException e) {
            System.err.println("Ошибка записи в лог: " + e.getMessage());
        }
    }
    
    public void info(String message) { log("INFO", message); }
    public void error(String message) { log("ERROR", message); }
    public void warn(String message) { log("WARN", message); }
}