package mio.service;

import mio.model.LineStop;
import mio.model.Route;
import mio.model.Stop;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public class GraphImageExporter {

    private static final int WIDTH = 1600;
    private static final int HEIGHT = 1600;

    private static final int MARGIN = 120;
    private static final int HEADER_HEIGHT = 120;
    private static final int FOOTER_HEIGHT = 80;

    private static final Color COLOR_BACKGROUND = new Color(252, 252, 252);
    private static final Color COLOR_ROUTE_LINE = new Color(0, 90, 200, 190);
    private static final Color COLOR_STOP_NODE = new Color(220, 20, 60);

    private static final Color COLOR_FIRST_STOP = new Color(0, 160, 60);
    private static final Color COLOR_LAST_STOP = new Color(240, 140, 0);

    private static final Color COLOR_TEXT = new Color(25, 25, 25);
    private static final Color LEGEND_BG = new Color(255, 255, 255, 240);

    private static final int NODE_RADIUS = 7;
    private static final int NODE_RADIUS_BIG = 10;

    private static final float ROUTE_STROKE_WIDTH = 4.0f;
    private static final float ARROW_SIZE = 12.0f;

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

        System.out.println("Grafos individuales generados");
    }

    public void exportFullGraph(
            Map<Integer, Route> routesById,
            Map<Integer, Stop> stopsById,
            Map<Integer, Map<Integer, List<LineStop>>> lineStopsByRouteAndOrientation,
            Path outputDir
    ) throws IOException {

        if (Files.notExists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║ Generando imágen del grafo de las rutas completas... ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");

        List<LineStop> allStopsGlobal = new ArrayList<>();

        for (var entry : lineStopsByRouteAndOrientation.entrySet()) {
            for (var orientEntry : entry.getValue().entrySet()) {
                List<LineStop> seq = new ArrayList<>(orientEntry.getValue());
                seq.sort(Comparator.comparingInt(LineStop::getSequence));
                allStopsGlobal.addAll(seq);
            }
        }

        BoundingBox bbox = calculateBoundingBox(allStopsGlobal, stopsById);
        if (bbox == null) {
            System.out.println("  ⚠ No se pudo calcular el bounding box. Abortando.");
            return;
        }

        int width = 2400;
        int height = 1800;
        int marginX = 80;
        int marginY = 100;
        int headerHeight = 80;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setColor(COLOR_BACKGROUND);
        g.fillRect(0, 0, width, height);

        setupHighQualityRendering(g);

        g.setColor(COLOR_TEXT);
        g.setFont(new Font("SansSerif", Font.BOLD, 32));
        g.drawString("Grafo Completo - Todas las Rutas SITM-MIO", marginX, 50);

        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g.setColor(new Color(70, 70, 70));
        int totalRoutes = routesById.size();
        int totalStops = allStopsGlobal.size();
        g.drawString(
                "Total de rutas: " + totalRoutes + " | Total de paradas en el grafo: " + totalStops,
                marginX, 75
        );

        int usableWidth = width - 2 * marginX;
        int usableHeight = height - marginY - headerHeight;

        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0, 90, 200, 50));

        int totalArcs = 0;
        for (var entry : lineStopsByRouteAndOrientation.entrySet()) {
            for (var orientEntry : entry.getValue().entrySet()) {
                List<LineStop> seq = new ArrayList<>(orientEntry.getValue());
                seq.sort(Comparator.comparingInt(LineStop::getSequence));

                for (int i = 0; i < seq.size() - 1; i++) {
                    Stop a = stopsById.get(seq.get(i).getStopId());
                    Stop b = stopsById.get(seq.get(i + 1).getStopId());
                    if (a == null || b == null) continue;

                    Point2D p1 = project(
                            a.getLon(), a.getLat(),
                            bbox, marginX, marginY,
                            usableWidth, usableHeight
                    );
                    Point2D p2 = project(
                            b.getLon(), b.getLat(),
                            bbox, marginX, marginY,
                            usableWidth, usableHeight
                    );

                    g.draw(new Line2D.Double(p1, p2));
                    totalArcs++;
                }
            }
        }

        Set<Integer> drawnStops = new HashSet<>();
        g.setColor(new Color(200, 0, 0, 180));
        int nodeRadius = 3;

        for (LineStop ls : allStopsGlobal) {
            int stopId = ls.getStopId();
            if (drawnStops.contains(stopId)) continue;
            
            Stop s = stopsById.get(stopId);
            if (s == null) continue;

            Point2D p = project(
                    s.getLon(), s.getLat(),
                    bbox, marginX, marginY,
                    usableWidth, usableHeight
            );

            g.fill(new Ellipse2D.Double(p.getX() - nodeRadius, p.getY() - nodeRadius, 
                    nodeRadius * 2, nodeRadius * 2));
            drawnStops.add(stopId);
        }

        g.setColor(new Color(240, 240, 240));
        g.fillRect(0, height - 60, width, 60);
        
        g.setColor(new Color(90, 90, 90));
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.drawString(
                String.format("SITM-MIO - Grafo Completo | Rutas: %d | Paradas únicas: %d | Arcos: %d",
                        totalRoutes, drawnStops.size(), totalArcs),
                marginX, height - 25
        );

        g.dispose();

        Path file = outputDir.resolve("Grafo_Completo_MIO.jpg");
        ImageIO.write(image, "jpg", file.toFile());

        System.out.println("Grafo completo generado");
        System.out.println("\nUbicación: " + file.toAbsolutePath());
        System.out.println("\n    - Rutas: " + totalRoutes);
        System.out.println("    - Paradas únicas: " + drawnStops.size());
        System.out.println("    - Arcos totales: " + totalArcs);
    }

    private boolean exportSingleGraph(
            Route route,
            int lineId,
            int orientation,
            List<LineStop> seq,
            Map<Integer, Stop> stopsById,
            Path outputDir
    ) throws IOException {

        if (seq.size() < 2) return false;

        BoundingBox bbox = calculateBoundingBox(seq, stopsById);
        if (bbox == null) return false;

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        setupHighQualityRendering(g);
        drawBackground(g);

        int usableW = WIDTH - 2 * MARGIN;
        int usableH = HEIGHT - HEADER_HEIGHT - FOOTER_HEIGHT - MARGIN;

        drawHeader(g, route, lineId, seq.size(), orientation);
        drawRouteArcs(g, seq, stopsById, bbox, MARGIN, HEADER_HEIGHT, usableW, usableH);
        drawStops(g, seq, stopsById, bbox, MARGIN, HEADER_HEIGHT, usableW, usableH);
        drawStopLabels(g, seq, stopsById, bbox, MARGIN, HEADER_HEIGHT, usableW, usableH);

        drawLegend(g, WIDTH - MARGIN - 250, HEADER_HEIGHT + 40);
        drawFooter(g, seq.size());

        g.dispose();

        String name = route != null ? route.getShortName() : ("LINE_" + lineId);
        name = name.replaceAll("[^a-zA-Z0-9_-]", "_");

        String f = String.format("%s_%s_%d.jpg",
                name,
                getOrientationLabel(orientation).toLowerCase(),
                lineId
        );

        Path file = outputDir.resolve(f);
        ImageIO.write(image, "jpg", file.toFile());

        return true;
    }

    private void setupHighQualityRendering(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
    }

    private void drawBackground(Graphics2D g) {
        g.setColor(COLOR_BACKGROUND);
        g.fillRect(0, 0, WIDTH, HEIGHT);
    }

    private void drawHeader(Graphics2D g, Route route, int lineId, int stops, int orientation) {
        g.setColor(COLOR_TEXT);

        g.setFont(new Font("SansSerif", Font.BOLD, 32));
        g.drawString("Ruta " +
                        (route != null ? route.getShortName() : lineId)
                        + " - " + getOrientationLabel(orientation),
                MARGIN, 45);

        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        if (route != null && route.getDescription() != null) {
            g.drawString(route.getDescription(), MARGIN, 70);
        }

        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(70, 70, 70));
        g.drawString(
                "ID: " + lineId + "  |  Paradas: " + stops + "  |  Arcos: " + (stops - 1),
                MARGIN, 95
        );
    }

    private void drawFooter(Graphics2D g, int nStops) {
        g.setColor(new Color(240, 240, 240));
        g.fillRect(0, HEIGHT - FOOTER_HEIGHT, WIDTH, FOOTER_HEIGHT);

        g.setColor(new Color(90, 90, 90));
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.drawString(
                "SITM-MIO — Grafo de ruta | Paradas: " + nStops,
                MARGIN, HEIGHT - 30
        );
    }

    private void drawRouteArcs(Graphics2D g,
                               List<LineStop> seq,
                               Map<Integer, Stop> stopsById,
                               BoundingBox bbox,
                               int offX, int offY, int w, int h) {

        g.setColor(COLOR_ROUTE_LINE);
        g.setStroke(new BasicStroke(ROUTE_STROKE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        for (int i = 0; i < seq.size() - 1; i++) {
            Stop a = stopsById.get(seq.get(i).getStopId());
            Stop b = stopsById.get(seq.get(i + 1).getStopId());
            if (a == null || b == null) continue;

            Point2D p1 = project(a.getLon(), a.getLat(), bbox, offX, offY, w, h);
            Point2D p2 = project(b.getLon(), b.getLat(), bbox, offX, offY, w, h);

            g.draw(new Line2D.Double(p1, p2));

            if (i % 2 == 0) drawArrow(g, p1, p2);
        }
    }

    private void drawArrow(Graphics2D g, Point2D from, Point2D to) {
        double ang = Math.atan2(to.getY() - from.getY(), to.getX() - from.getX());
        double arrowAngle = Math.PI / 6;

        double mx = from.getX() + 0.8 * (to.getX() - from.getX());
        double my = from.getY() + 0.8 * (to.getY() - from.getY());

        double x1 = mx - ARROW_SIZE * Math.cos(ang - arrowAngle);
        double y1 = my - ARROW_SIZE * Math.sin(ang - arrowAngle);
        double x2 = mx - ARROW_SIZE * Math.cos(ang + arrowAngle);
        double y2 = my - ARROW_SIZE * Math.sin(ang + arrowAngle);

        Path2D arrow = new Path2D.Double();
        arrow.moveTo(mx, my);
        arrow.lineTo(x1, y1);
        arrow.lineTo(x2, y2);
        arrow.closePath();

        g.fill(arrow);
    }

    private void drawStops(Graphics2D g,
                           List<LineStop> seq,
                           Map<Integer, Stop> stopsById,
                           BoundingBox bbox,
                           int offX, int offY, int w, int h) {

        int firstStopId = seq.get(0).getStopId();
        int lastStopId = seq.get(seq.size() - 1).getStopId();

        for (LineStop ls : seq) {
            Stop s = stopsById.get(ls.getStopId());
            if (s == null) continue;

            Point2D p = project(s.getLon(), s.getLat(), bbox, offX, offY, w, h);

            Color c;
            int r;

            if (ls.getStopId() == firstStopId) {
                c = COLOR_FIRST_STOP;
                r = NODE_RADIUS_BIG;
            } else if (ls.getStopId() == lastStopId) {
                c = COLOR_LAST_STOP;
                r = NODE_RADIUS_BIG;
            } else {
                c = COLOR_STOP_NODE;
                r = NODE_RADIUS;
            }

            g.setColor(c);
            g.fill(new Ellipse2D.Double(p.getX() - r, p.getY() - r, r * 2, r * 2));
        }
    }

    private void drawStopLabels(Graphics2D g,
                                List<LineStop> seq,
                                Map<Integer, Stop> stopsById,
                                BoundingBox bbox,
                                int offX, int offY, int w, int h) {

        g.setFont(new Font("SansSerif", Font.PLAIN, 13));

        int firstStopId = seq.get(0).getStopId();
        int lastStopId = seq.get(seq.size() - 1).getStopId();

        Stop first = stopsById.get(firstStopId);
        Stop last = stopsById.get(lastStopId);

        if (first != null) {
            drawLabel(g, first.getShortName(), first, bbox, offX, offY, w, h, true);
        }

        if (last != null && firstStopId != lastStopId) {
            drawLabel(g, last.getShortName(), last, bbox, offX, offY, w, h, true);
        }

        int step = Math.max(1, seq.size() / 6);
        for (int i = step; i < seq.size() - step; i += step) {
            int stopId = seq.get(i).getStopId();
            Stop s = stopsById.get(stopId);
            if (s != null && stopId != firstStopId && stopId != lastStopId) {
                drawLabel(g, s.getShortName(), s, bbox, offX, offY, w, h, false);
            }
        }
    }

    private void drawLabel(Graphics2D g, String text, Stop s,
                           BoundingBox bbox,
                           int offX, int offY, int w, int h, boolean bold) {

        if (s == null || text == null) return;

        Point2D p = project(s.getLon(), s.getLat(), bbox, offX, offY, w, h);

        Font f = bold ?
                new Font("SansSerif", Font.BOLD, 14) :
                new Font("SansSerif", Font.PLAIN, 12);

        g.setFont(f);

        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(text);
        int th = fm.getHeight();

        int x = (int) p.getX() + 10;
        int y = (int) p.getY() - 10;

        g.setColor(new Color(255, 255, 255, 240));
        g.fillRoundRect(x - 4, y - th, tw + 8, th + 6, 6, 6);
        
        g.setColor(new Color(180, 180, 180));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(x - 4, y - th, tw + 8, th + 6, 6, 6);

        g.setColor(new Color(20, 20, 20));
        g.drawString(text, x, y);
    }

    private void drawLegend(Graphics2D g, int x, int y) {

        g.setColor(LEGEND_BG);
        g.fillRoundRect(x, y, 240, 150, 12, 12);

        g.setColor(Color.GRAY);
        g.drawRoundRect(x, y, 240, 150, 12, 12);

        g.setColor(COLOR_TEXT);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("Leyenda", x + 12, y + 22);

        int py = y + 45;

        g.setFont(new Font("SansSerif", Font.PLAIN, 12));

        g.setColor(COLOR_FIRST_STOP);
        g.fillOval(x + 12, py - 6, 12, 12);
        g.setColor(COLOR_TEXT);
        g.drawString("Primera parada", x + 32, py);

        py += 22;
        g.setColor(COLOR_LAST_STOP);
        g.fillOval(x + 12, py - 6, 12, 12);
        g.setColor(COLOR_TEXT);
        g.drawString("Última parada", x + 32, py);

        py += 22;
        g.setColor(COLOR_STOP_NODE);
        g.fillOval(x + 12, py - 6, 10, 10);
        g.setColor(COLOR_TEXT);
        g.drawString("Paradas intermedias", x + 32, py);

        py += 24;
        g.setColor(COLOR_ROUTE_LINE);
        g.setStroke(new BasicStroke(4f));
        g.drawLine(x + 12, py, x + 40, py);
        g.setColor(COLOR_TEXT);
        g.setStroke(new BasicStroke(1.5f));
        g.drawString("Ruta / dirección", x + 55, py + 4);
    }

    private BoundingBox calculateBoundingBox(List<LineStop> seq, Map<Integer, Stop> stops) {
        double minLat = Double.POSITIVE_INFINITY, maxLat = Double.NEGATIVE_INFINITY;
        double minLon = Double.POSITIVE_INFINITY, maxLon = Double.NEGATIVE_INFINITY;

        int validStops = 0;
        for (LineStop ls : seq) {
            Stop s = stops.get(ls.getStopId());
            if (s == null) continue;

            validStops++;
            if (s.getLat() < minLat) minLat = s.getLat();
            if (s.getLat() > maxLat) maxLat = s.getLat();
            if (s.getLon() < minLon) minLon = s.getLon();
            if (s.getLon() > maxLon) maxLon = s.getLon();
        }

        if (validStops == 0) return null;

        if (minLat == maxLat) {
            maxLat = minLat + 0.0001;
        }
        if (minLon == maxLon) {
            maxLon = minLon + 0.0001;
        }

        double padLat = (maxLat - minLat) * 0.05;
        double padLon = (maxLon - minLon) * 0.05;

        return new BoundingBox(minLon - padLon, maxLon + padLon, minLat - padLat, maxLat + padLat);
    }

    private Point2D project(double lon, double lat, BoundingBox b, int ox, int oy, int w, int h) {
        double x = (lon - b.minLon) / (b.maxLon - b.minLon);
        double y = (lat - b.minLat) / (b.maxLat - b.minLat);

        return new Point2D.Double(
                ox + x * w,
                oy + (1 - y) * h
        );
    }

    private String getOrientationLabel(int o) {
        return (o == 0) ? "IDA" : "REGRESO";
    }

    private static class BoundingBox {
        final double minLon, maxLon, minLat, maxLat;

        BoundingBox(double minLon, double maxLon, double minLat, double maxLat) {
            this.minLon = minLon;
            this.maxLon = maxLon;
            this.minLat = minLat;
            this.maxLat = maxLat;
        }
    }
}

