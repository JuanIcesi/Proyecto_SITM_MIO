package mio.service;

import mio.model.ParadaRuta;
import mio.Util.UtilidadesCsv;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

// Carga relaciones ruta-parada desde archivo CSV
public class CargadorParadasRutas {

    // Retorna: Map<lineId, Map<orientation, List<ParadaRuta>>>
    public Map<Integer, Map<Integer, List<ParadaRuta>>> loadLineStops(Path path) throws IOException {
        Map<Integer, Map<Integer, List<ParadaRuta>>> grouped = new HashMap<>();

        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            boolean first = true;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Omite encabezado
                if (first) {
                    first = false;
                    if (line.toUpperCase().contains("LINESTOP")) {
                        continue;
                    }
                }

                String[] parts = UtilidadesCsv.splitCsvLine(line);
                if (parts.length < 5) continue;

                try {
                    int sequence = Integer.parseInt(parts[1].trim());
                    int orientation = Integer.parseInt(parts[2].trim());
                    int lineId = Integer.parseInt(parts[3].trim());
                    int stopId = Integer.parseInt(parts[4].trim());

                    ParadaRuta ls = new ParadaRuta(lineId, stopId, sequence, orientation);

                    // Agrupa por ruta y orientación
                    grouped
                        .computeIfAbsent(lineId, k -> new HashMap<>())
                        .computeIfAbsent(orientation, k -> new ArrayList<>())
                        .add(ls);

                } catch (NumberFormatException e) {
                }
            }
        }

        return grouped;
    }
}

