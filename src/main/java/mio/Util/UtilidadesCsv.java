package mio.Util;

// Utilidades para procesar archivos CSV
public class UtilidadesCsv {

    private static final String DELIMITER = ",";

    // Divide línea CSV en campos y limpia valores (elimina espacios y comillas)
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

