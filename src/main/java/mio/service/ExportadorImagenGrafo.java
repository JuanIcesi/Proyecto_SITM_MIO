package mio.service;

import mio.model.ParadaRuta;
import mio.model.Ruta;
import mio.model.Parada;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

// Genera imágenes visuales de los grafos de rutas
public class ExportadorImagenGrafo {

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

    // genera una imagen JPG por cada ruta y orientación (ida/regreso)
    public void exportRouteGraphs(
            Map<Integer, Ruta> routesById,
            Map<Integer, Parada> stopsById,
            Map<Integer, Map<Integer, List<ParadaRuta>>> lineStopsByRouteAndOrientation,
            Path outputDir
    ) throws IOException {

        // creo la carpeta si no existe
        if (Files.notExists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        // agarro todas las rutas y las ordeno
        List<Integer> lineIds = new ArrayList<>(lineStopsByRouteAndOrientation.keySet());
        Collections.sort(lineIds);

        // voy ruta por ruta
        for (int lineId : lineIds) {
            Ruta route = routesById.get(lineId);
            // las paradas vienen agrupadas por orientación
            Map<Integer, List<ParadaRuta>> byOrientation = lineStopsByRouteAndOrientation.get(lineId);

            // veo qué orientaciones tiene (ida/regreso)
            List<Integer> orientations = new ArrayList<>(byOrientation.keySet());
            Collections.sort(orientations);

            // genero una imagen por cada orientación
            for (int orientation : orientations) {
                // ordeno las paradas por secuencia
                List<ParadaRuta> seq = new ArrayList<>(byOrientation.get(orientation));
                seq.sort(Comparator.comparingInt(ParadaRuta::getSequence));

                // genero la imagen
                exportSingleGraph(route, lineId, orientation, seq, stopsById, outputDir);
            }
        }

        System.out.println("Grafos individuales generados");
    }

    // genera una sola imagen con todas las rutas juntas
    public void exportFullGraph(
            Map<Integer, Ruta> routesById,
            Map<Integer, Parada> stopsById,
            Map<Integer, Map<Integer, List<ParadaRuta>>> lineStopsByRouteAndOrientation,
            Path outputDir
    ) throws IOException {

        if (Files.notExists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║ Generando imágen del grafo de las rutas completas... ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");

        // junto todas las paradas de todas las rutas
        List<ParadaRuta> allStopsGlobal = new ArrayList<>();

        for (var entry : lineStopsByRouteAndOrientation.entrySet()) {
            for (var orientEntry : entry.getValue().entrySet()) {
                List<ParadaRuta> seq = new ArrayList<>(orientEntry.getValue());
                seq.sort(Comparator.comparingInt(ParadaRuta::getSequence));
                allStopsGlobal.addAll(seq);
            }
        }

        // calculo el área que contiene todas las paradas (para saber qué mostrar)
        BoundingBox bbox = calculateBoundingBox(allStopsGlobal, stopsById);
        if (bbox == null) {
            System.out.println("  ⚠ No se pudo calcular el bounding box. Abortando.");
            return;
        }

        // tamaño de la imagen
        int width = 2400;
        int height = 1800;
        int marginX = 80;
        int marginY = 100;
        int headerHeight = 80;

        // creo la imagen
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // pinto el fondo
        g.setColor(COLOR_BACKGROUND);
        g.fillRect(0, 0, width, height);

        // activo calidad alta
        setupHighQualityRendering(g);

        // escribo el título
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

        // calculo el espacio disponible para dibujar
        int usableWidth = width - 2 * marginX;
        int usableHeight = height - marginY - headerHeight;

        // configuro cómo dibujar las líneas
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0, 90, 200, 50)); // azul transparente

        // dibujo todas las líneas de todas las rutas
        int totalArcs = 0;
        for (var entry : lineStopsByRouteAndOrientation.entrySet()) {
            for (var orientEntry : entry.getValue().entrySet()) {
                List<ParadaRuta> seq = new ArrayList<>(orientEntry.getValue());
                seq.sort(Comparator.comparingInt(ParadaRuta::getSequence));

                // dibujo línea de parada i a parada i+1
                for (int i = 0; i < seq.size() - 1; i++) {
                    Parada a = stopsById.get(seq.get(i).getStopId());
                    Parada b = stopsById.get(seq.get(i + 1).getStopId());
                    if (a == null || b == null) continue;

                    // convierto lat/lon a píxeles
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

        // dibujo los círculos (paradas)
        Set<Integer> drawnStops = new HashSet<>(); // para no dibujar la misma parada dos veces
        g.setColor(new Color(200, 0, 0, 180)); // rojo transparente
        int nodeRadius = 3;

        for (ParadaRuta ls : allStopsGlobal) {
            int stopId = ls.getStopId();
            if (drawnStops.contains(stopId)) continue; // ya la dibujé
            
            Parada s = stopsById.get(stopId);
            if (s == null) continue;

            // convierto lat/lon a píxeles
            Point2D p = project(
                    s.getLon(), s.getLat(),
                    bbox, marginX, marginY,
                    usableWidth, usableHeight
            );

            // dibujo el círculo
            g.fill(new Ellipse2D.Double(p.getX() - nodeRadius, p.getY() - nodeRadius, 
                    nodeRadius * 2, nodeRadius * 2));
            drawnStops.add(stopId);
        }

        // dibujo el pie de página
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

        // guardo la imagen como JPG
        Path file = outputDir.resolve("Grafo_Completo_MIO.jpg");
        ImageIO.write(image, "jpg", file.toFile());

        System.out.println("Grafo completo generado");
        System.out.println("\nUbicación: " + file.toAbsolutePath());
        System.out.println("\n    - Rutas: " + totalRoutes);
        System.out.println("    - Paradas únicas: " + drawnStops.size());
        System.out.println("    - Arcos totales: " + totalArcs);
    }

    // genera la imagen de una ruta específica
    private boolean exportSingleGraph(
            Ruta route,
            int lineId,
            int orientation,
            List<ParadaRuta> seq,
            Map<Integer, Parada> stopsById,
            Path outputDir
    ) throws IOException {

        // necesito al menos 2 paradas para hacer un arco
        if (seq.size() < 2) return false;

        // calculo el área que contiene las paradas de esta ruta
        BoundingBox bbox = calculateBoundingBox(seq, stopsById);
        if (bbox == null) return false;

        // creo la imagen
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // activo calidad alta y pinto el fondo
        setupHighQualityRendering(g);
        drawBackground(g);

        // calculo el espacio disponible (quitando márgenes)
        int usableW = WIDTH - 2 * MARGIN;
        int usableH = HEIGHT - HEADER_HEIGHT - FOOTER_HEIGHT - MARGIN;

        // dibujo todo en orden: título, líneas, círculos, etiquetas, leyenda, pie
        drawHeader(g, route, lineId, seq.size(), orientation);
        drawRouteArcs(g, seq, stopsById, bbox, MARGIN, HEADER_HEIGHT, usableW, usableH);
        drawStops(g, seq, stopsById, bbox, MARGIN, HEADER_HEIGHT, usableW, usableH);
        drawStopLabels(g, seq, stopsById, bbox, MARGIN, HEADER_HEIGHT, usableW, usableH);

        drawLegend(g, WIDTH - MARGIN - 250, HEADER_HEIGHT + 40);
        drawFooter(g, seq.size());

        g.dispose();

        // armo el nombre del archivo
        String name = route != null ? route.getShortName() : ("LINE_" + lineId);
        name = name.replaceAll("[^a-zA-Z0-9_-]", "_"); // quito caracteres raros

        String f = String.format("%s_%s_%d.jpg",
                name,
                getOrientationLabel(orientation).toLowerCase(),
                lineId
        );

        // guardo como JPG
        Path file = outputDir.resolve(f);
        ImageIO.write(image, "jpg", file.toFile());

        return true;
    }

    // activo opciones para que se vea mejor (suavizado, etc)
    private void setupHighQualityRendering(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
    }

    // pinto el fondo
    private void drawBackground(Graphics2D g) {
        g.setColor(COLOR_BACKGROUND);
        g.fillRect(0, 0, WIDTH, HEIGHT);
    }

    // dibujo el título arriba
    private void drawHeader(Graphics2D g, Ruta route, int lineId, int stops, int orientation) {
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

    // dibujo las líneas que conectan las paradas
    private void drawRouteArcs(Graphics2D g,
                               List<ParadaRuta> seq,
                               Map<Integer, Parada> stopsById,
                               BoundingBox bbox,
                               int offX, int offY, int w, int h) {

        g.setColor(COLOR_ROUTE_LINE);
        g.setStroke(new BasicStroke(ROUTE_STROKE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // dibujo línea de parada i a parada i+1
        for (int i = 0; i < seq.size() - 1; i++) {
            Parada a = stopsById.get(seq.get(i).getStopId());
            Parada b = stopsById.get(seq.get(i + 1).getStopId());
            if (a == null || b == null) continue;

            // convierto lat/lon a píxeles
            Point2D p1 = project(a.getLon(), a.getLat(), bbox, offX, offY, w, h);
            Point2D p2 = project(b.getLon(), b.getLat(), bbox, offX, offY, w, h);

            g.draw(new Line2D.Double(p1, p2));

            // dibujo flecha cada dos arcos para mostrar dirección
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

    // dibujo los círculos (paradas) con colores diferentes
    private void drawStops(Graphics2D g,
                           List<ParadaRuta> seq,
                           Map<Integer, Parada> stopsById,
                           BoundingBox bbox,
                           int offX, int offY, int w, int h) {

        // identifico primera y última para darles color especial
        int firstStopId = seq.get(0).getStopId();
        int lastStopId = seq.get(seq.size() - 1).getStopId();

        for (ParadaRuta ls : seq) {
            Parada s = stopsById.get(ls.getStopId());
            if (s == null) continue;

            // convierto lat/lon a píxeles
            Point2D p = project(s.getLon(), s.getLat(), bbox, offX, offY, w, h);

            Color c;
            int r;

            // primera = verde, última = naranja, otras = rojo
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

            // dibujo el círculo
            g.setColor(c);
            g.fill(new Ellipse2D.Double(p.getX() - r, p.getY() - r, r * 2, r * 2));
        }
    }

    private void drawStopLabels(Graphics2D g,
                                List<ParadaRuta> seq,
                                Map<Integer, Parada> stopsById,
                                BoundingBox bbox,
                                int offX, int offY, int w, int h) {

        g.setFont(new Font("SansSerif", Font.PLAIN, 13));

        int firstStopId = seq.get(0).getStopId();
        int lastStopId = seq.get(seq.size() - 1).getStopId();

        Parada first = stopsById.get(firstStopId);
        Parada last = stopsById.get(lastStopId);

        if (first != null) {
            drawLabel(g, first.getShortName(), first, bbox, offX, offY, w, h, true);
        }

        if (last != null && firstStopId != lastStopId) {
            drawLabel(g, last.getShortName(), last, bbox, offX, offY, w, h, true);
        }

        int step = Math.max(1, seq.size() / 6);
        for (int i = step; i < seq.size() - step; i += step) {
            int stopId = seq.get(i).getStopId();
            Parada s = stopsById.get(stopId);
            if (s != null && stopId != firstStopId && stopId != lastStopId) {
                drawLabel(g, s.getShortName(), s, bbox, offX, offY, w, h, false);
            }
        }
    }

    private void drawLabel(Graphics2D g, String text, Parada s,
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

    // calculo el área que contiene todas las paradas (mín/máx de lat y lon)
    private BoundingBox calculateBoundingBox(List<ParadaRuta> seq, Map<Integer, Parada> stops) {
        double minLat = Double.POSITIVE_INFINITY, maxLat = Double.NEGATIVE_INFINITY;
        double minLon = Double.POSITIVE_INFINITY, maxLon = Double.NEGATIVE_INFINITY;

        // busco los límites
        int validStops = 0;
        for (ParadaRuta ls : seq) {
            Parada s = stops.get(ls.getStopId());
            if (s == null) continue;

            validStops++;
            if (s.getLat() < minLat) minLat = s.getLat();
            if (s.getLat() > maxLat) maxLat = s.getLat();
            if (s.getLon() < minLon) minLon = s.getLon();
            if (s.getLon() > maxLon) maxLon = s.getLon();
        }

        if (validStops == 0) return null;

        // si todas las paradas están en el mismo punto, agrego un poquito para evitar división por cero
        if (minLat == maxLat) {
            maxLat = minLat + 0.0001;
        }
        if (minLon == maxLon) {
            maxLon = minLon + 0.0001;
        }

        // agrego un 5% de margen para que no quede pegado a los bordes
        double padLat = (maxLat - minLat) * 0.05;
        double padLon = (maxLon - minLon) * 0.05;

        return new BoundingBox(minLon - padLon, maxLon + padLon, minLat - padLat, maxLat + padLat);
    }

    // convierto lat/lon a píxeles en la imagen
    private Point2D project(double lon, double lat, BoundingBox b, int ox, int oy, int w, int h) {
        // normalizo al rango 0-1
        double x = (lon - b.minLon) / (b.maxLon - b.minLon);
        double y = (lat - b.minLat) / (b.maxLat - b.minLat);

        // convierto a píxeles (invierto Y porque en imágenes Y va hacia abajo)
        return new Point2D.Double(
                ox + x * w,
                oy + (1 - y) * h
        );
    }

    private String getOrientationLabel(int o) {
        return (o == 0) ? "IDA" : "REGRESO";
    }

    // Área geográfica rectangular que contiene las paradas
    private static class BoundingBox {
        final double minLon;
        final double maxLon;
        final double minLat;
        final double maxLat;

        BoundingBox(double minLon, double maxLon, double minLat, double maxLat) {
            this.minLon = minLon;
            this.maxLon = maxLon;
            this.minLat = minLat;
            this.maxLat = maxLat;
        }
    }
}

