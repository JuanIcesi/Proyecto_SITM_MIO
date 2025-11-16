package mio.service;

import mio.model.Route;
import mio.Util.CsvUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class RouteLoader {

    /**
     * Carga lines.csv en un mapa: lineId -> Route
     */
    public Map<Integer, Route> loadRoutes(Path path) throws IOException {
        Map<Integer, Route> routesById = new HashMap<>();

        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            boolean first = true;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Encabezado
                if (first) {
                    first = false;
                    if (line.toUpperCase().contains("LINEID")) {
                        continue;
                    }
                }

                String[] parts = CsvUtils.splitCsvLine(line);
                if (parts.length < 4) continue;

                try {
                    int lineId = Integer.parseInt(parts[0].trim());
                    String shortName = parts[2].trim();
                    String description = parts[3].trim();

                    Route route = new Route(lineId, shortName, description);
                    routesById.put(lineId, route);
                } catch (NumberFormatException e) {
                    // ignorar fila inválida
                }
            }
        }

        return routesById;
    }
}
