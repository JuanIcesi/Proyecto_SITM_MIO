package mio.util;

public class CsvUtils {

    // Cambia este delimitador si tus CSV usan ";" en vez de ","
    private static final String DELIMITER = ",";

    /**
     * Divide una línea CSV y limpia comillas.
     */
    public static String[] splitCsvLine(String line) {
        String[] raw = line.split(DELIMITER);
        String[] cleaned = new String[raw.length];

        for (int i = 0; i < raw.length; i++) {
            String value = raw[i].trim();
            if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                value = value.substring(1, value.length() - 1);
            }
            cleaned[i] = value;
        }
        return cleaned;
    }
}