package mio.service;

import mio.model.LineStop;
import mio.model.Route;
import mio.model.Stop;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class GraphImageExporter {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 800;
    private static final int MARGIN = 50;

    /**
     * Exporta un JPG por cada ruta y orientación.
     */
    public void exportRouteGraphs(
            Map<Integer, Route> routesById,
            Map<Integer, Stop> stopsById,
            Map<Integer, Map<Integer, List<LineStop>>> lineStopsByRouteAndOrientation,
            Path outputDir
    ) throws IOException {

        if (Files.notExists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        List<Integer> lineIds = new ArrayList<>(lineStopsByRouteAndOrientation.keySet());
        Collections.sort(lineIds);

        for (int lineId : lineIds) {
            Route route = routesById.get(lineId);
            Map<Integer, List<LineStop>> byOrientation = lineStopsByRouteAndOrientation.get(lineId);

            List<Integer> orientations = new ArrayList<>(byOrientation.keySet());
            Collections.sort(orientations);

            for (int orientation : orientations) {
                List<LineStop> seq = new ArrayList<>(byOrientation.get(orientation));
                seq.sort(Comparator.comparingInt(LineStop::getSequence));

                exportSingleGraph(route, lineId, orientation, seq, stopsById, outputDir);
            }
        }
    }

    /**
     * Dibuja un solo grafo (ruta + orientación) y lo guarda como JPG.
     */
    private void exportSingleGraph(
            Route route,
            int lineId,
            int orientation,
            List<LineStop> sequence,
            Map<Integer, Stop> stopsById,
            Path outputDir
    ) throws IOException {

        if (sequence.size() < 2) {
            return; // no hay suficientes paradas para dibujar
        }

        // 1) Calcular min/max lat/lon de las paradas de esta ruta/orientación
        double minLat = Double.POSITIVE_INFINITY;
        double maxLat = Double.NEGATIVE_INFINITY;
        double minLon = Double.POSITIVE_INFINITY;
        double maxLon = Double.NEGATIVE_INFINITY;

        List<Stop> usedStops = new ArrayList<>();
        for (LineStop ls : sequence) {
            Stop s = stopsById.get(ls.getStopId());
            if (s == null) {
                continue;
            }
            usedStops.add(s);

            double lat = s.getLat();
            double lon = s.getLon();

            if (lat < minLat) minLat = lat;
            if (lat > maxLat) maxLat = lat;
            if (lon < minLon) minLon = lon;
            if (lon > maxLon) maxLon = lon;
        }

        if (usedStops.isEmpty()) {
            return;
        }

        // Evitar división por cero si todas las paradas tienen la misma coord
        if (minLat == maxLat) {
            maxLat = minLat + 0.0001;
        }
        if (minLon == maxLon) {
            maxLon = minLon + 0.0001;
        }

        // 2) Crear imagen y Graphics2D
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // Fondo blanco
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Antialiasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 3) Título
        String routeName = (route != null ? route.getShortName() : ("LINEID " + lineId));
        String orientationLabel = switch (orientation) {
            case 0 -> "IDA";
            case 1 -> "REGRESO";
            default -> "ORI " + orientation;
        };

        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        String title = "Ruta " + routeName + " (" + orientationLabel + ")";
        g.drawString(title, MARGIN, MARGIN - 15);

        int usableWidth = WIDTH - 2 * MARGIN;
        int usableHeight = HEIGHT - 2 * MARGIN;

        // 4) Dibujar arcos (líneas entre paradas consecutivas)
        g.setColor(new Color(0, 102, 204)); // azul
        g.setStroke(new BasicStroke(2f));

        for (int i = 0; i < sequence.size() - 1; i++) {
            Stop from = stopsById.get(sequence.get(i).getStopId());
            Stop to = stopsById.get(sequence.get(i + 1).getStopId());
            if (from == null || to == null) continue;

            int[] p1 = project(from.getLon(), from.getLat(), minLon, maxLon, minLat, maxLat, usableWidth, usableHeight);
            int[] p2 = project(to.getLon(), to.getLat(), minLon, maxLon, minLat, maxLat, usableWidth, usableHeight);

            g.drawLine(p1[0], p1[1], p2[0], p2[1]);
        }

        // 5) Dibujar nodos (paradas)
        g.setColor(new Color(220, 20, 60)); // rojo
        int nodeRadius = 5;

        for (Stop s : usedStops) {
            int[] p = project(s.getLon(), s.getLat(), minLon, maxLon, minLat, maxLat, usableWidth, usableHeight);
            int x = p[0] - nodeRadius;
            int y = p[1] - nodeRadius;
            g.fillOval(x, y, nodeRadius * 2, nodeRadius * 2);
        }

        // 6) Etiquetar primera y última parada
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));

        Stop first = stopsById.get(sequence.get(0).getStopId());
        Stop last = stopsById.get(sequence.get(sequence.size() - 1).getStopId());

        if (first != null) {
            int[] p = project(first.getLon(), first.getLat(), minLon, maxLon, minLat, maxLat, usableWidth, usableHeight);
            g.drawString(first.getShortName(), p[0] + 5, p[1] - 5);
        }
        if (last != null) {
            int[] p = project(last.getLon(), last.getLat(), minLon, maxLon, minLat, maxLat, usableWidth, usableHeight);
            g.drawString(last.getShortName(), p[0] + 5, p[1] - 5);
        }

        g.dispose();

        // 7) Guardar JPG
        String safeRouteName = routeName.replaceAll("[^a-zA-Z0-9_-]", "_");
        String fileName = safeRouteName + "_" + orientationLabel.toLowerCase() + ".jpg";
        Path outputFile = outputDir.resolve(fileName);

        ImageIO.write(image, "jpg", outputFile.toFile());
        System.out.println("Imagen generada: " + outputFile.toAbsolutePath());
    }

    /**
     * Proyecta (lon, lat) a coordenadas de la imagen.
     */
    private int[] project(double lon,
                          double lat,
                          double minLon,
                          double maxLon,
                          double minLat,
                          double maxLat,
                          int usableWidth,
                          int usableHeight) {

        double xNorm = (lon - minLon) / (maxLon - minLon); // [0,1]
        double yNorm = (lat - minLat) / (maxLat - minLat); // [0,1]

        int x = MARGIN + (int) Math.round(xNorm * usableWidth);
        // invertimos Y para que el norte quede arriba
        int y = HEIGHT - MARGIN - (int) Math.round(yNorm * usableHeight);

        return new int[]{x, y};
    }
}
