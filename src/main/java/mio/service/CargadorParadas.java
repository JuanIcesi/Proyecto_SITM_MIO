package mio.service;

import mio.model.Parada;
import mio.Util.UtilidadesCsv;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

// Carga paradas desde archivo CSV
public class CargadorParadas {

    public Map<Integer, Parada> loadStops(Path path) throws IOException {
        Map<Integer, Parada> stopsById = new HashMap<>();

        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            boolean first = true;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (first) {
                    first = false;
                    if (line.toUpperCase().contains("STOPID")) {
                        continue;
                    }
                }

                String[] parts = UtilidadesCsv.splitCsvLine(line);
                if (parts.length < 8) continue;

                try {
                    int stopId = Integer.parseInt(parts[0].trim());
                    String shortName = parts[2].trim();
                    String longName = parts[3].trim();
                    double lon = Double.parseDouble(parts[6].trim());
                    double lat = Double.parseDouble(parts[7].trim());

                    Parada stop = new Parada(stopId, shortName, longName, lat, lon);
                    stopsById.put(stopId, stop);
                } catch (NumberFormatException e) {
                }
            }
        }

        return stopsById;
    }
}

