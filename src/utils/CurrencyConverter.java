package utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Класс для конвертации валют
 * Использует фиксированные курсы обмена
 */
public class CurrencyConverter {
    // Курсы обмена к базовой валюте (RUB)
    // Например: 1 USD = 100 RUB, 1 EUR = 110 RUB
    private static final Map<String, Double> exchangeRates = new HashMap<>();
    
    static {
        // Инициализация курсов валют
        // Базовая валюта - RUB
        exchangeRates.put("RUB", 1.0);
        exchangeRates.put("USD", 100.0);  // 1 USD = 100 RUB
        exchangeRates.put("EUR", 110.0);  // 1 EUR = 110 RUB
    }
    
    /**
     * Конвертирует сумму из одной валюты в другую
     * @param amount сумма для конвертации
     * @param fromCurrency исходная валюта
     * @param toCurrency целевая валюта
     * @return конвертированная сумма
     */
    public static double convert(double amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }
        
        // Конвертируем в базовую валюту (RUB), затем в целевую
        double amountInRUB = amount * exchangeRates.getOrDefault(fromCurrency, 1.0);
        double rateToTarget = exchangeRates.getOrDefault(toCurrency, 1.0);
        
        return amountInRUB / rateToTarget;
    }
    
    /**
     * Получает курс обмена между двумя валютами
     * @param fromCurrency исходная валюта
     * @param toCurrency целевая валюта
     * @return курс обмена (сколько единиц целевой валюты за 1 единицу исходной)
     */
    public static double getExchangeRate(String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return 1.0;
        }
        
        double rateFrom = exchangeRates.getOrDefault(fromCurrency, 1.0);
        double rateTo = exchangeRates.getOrDefault(toCurrency, 1.0);
        
        return rateFrom / rateTo;
    }
    
    /**
     * Проверяет, поддерживается ли валюта
     */
    public static boolean isCurrencySupported(String currency) {
        return exchangeRates.containsKey(currency);
    }
    
    /**
     * Получает форматированную строку курса обмена
     */
    public static String getExchangeRateString(String fromCurrency, String toCurrency) {
        double rate = getExchangeRate(fromCurrency, toCurrency);
        return String.format("1 %s = %.4f %s", fromCurrency, rate, toCurrency);
    }
}

