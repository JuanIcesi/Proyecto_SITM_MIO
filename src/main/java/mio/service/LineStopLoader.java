package mio.service;

import mio.model.LineStop;
import mio.Util.CsvUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class LineStopLoader {

    public Map<Integer, Map<Integer, List<LineStop>>> loadLineStops(Path path) throws IOException {
        Map<Integer, Map<Integer, List<LineStop>>> grouped = new HashMap<>();

        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            boolean first = true;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (first) {
                    first = false;
                    if (line.toUpperCase().contains("LINESTOP")) {
                        continue;
                    }
                }

                String[] parts = CsvUtils.splitCsvLine(line);
                if (parts.length < 5) continue;

                try {
                    int sequence = Integer.parseInt(parts[1].trim());
                    int orientation = Integer.parseInt(parts[2].trim());
                    int lineId = Integer.parseInt(parts[3].trim());
                    int stopId = Integer.parseInt(parts[4].trim());

                    LineStop ls = new LineStop(lineId, stopId, sequence, orientation);

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
