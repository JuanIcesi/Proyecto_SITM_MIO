package mio.service;

import mio.model.Ruta;
import mio.Util.UtilidadesCsv;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

// Carga rutas desde archivo CSV
public class CargadorRutas {

    public Map<Integer, Ruta> loadRoutes(Path path) throws IOException {
        Map<Integer, Ruta> routesById = new HashMap<>();

        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            boolean first = true;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (first) {
                    first = false;
                    if (line.toUpperCase().contains("LINEID")) {
                        continue;
                    }
                }

                String[] parts = UtilidadesCsv.splitCsvLine(line);
                if (parts.length < 4) continue;

                try {
                    int lineId = Integer.parseInt(parts[0].trim());
                    String shortName = parts[2].trim();
                    String description = parts[3].trim();

                    Ruta route = new Ruta(lineId, shortName, description);
                    routesById.put(lineId, route);
                } catch (NumberFormatException e) {
                }
            }
        }

        return routesById;
    }
}

