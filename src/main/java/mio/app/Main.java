package mio.app;

import mio.model.Route;
import mio.model.Stop;
import mio.model.LineStop;
import mio.service.RouteLoader;
import mio.service.StopLoader;
import mio.service.LineStopLoader;
import mio.service.GraphBuilder;
import mio.service.GraphImageExporter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {

        Path linesCsvPath     = Path.of("src/data/proyecto-mio/MIO/lines-241.csv");
        Path stopsCsvPath     = Path.of("src/data/proyecto-mio/MIO/stops-241.csv");
        Path lineStopsCsvPath = Path.of("src/data/proyecto-mio/MIO/linestops-241.csv");

        RouteLoader routeLoader = new RouteLoader();
        StopLoader stopLoader = new StopLoader();
        LineStopLoader lineStopLoader = new LineStopLoader();
        GraphBuilder graphBuilder = new GraphBuilder();
        GraphImageExporter imageExporter = new GraphImageExporter();

        try {
            System.out.println("Cargando datos de los archivos CSV...");
            Map<Integer, Route> routesById = routeLoader.loadRoutes(linesCsvPath);
            Map<Integer, Stop> stopsById = stopLoader.loadStops(stopsCsvPath);
            Map<Integer, Map<Integer, List<LineStop>>> lineStopsByRouteAndOrientation =
                    lineStopLoader.loadLineStops(lineStopsCsvPath);
            
            System.out.println("Rutas cargadas: " + routesById.size());
            System.out.println("Paradas cargadas: " + stopsById.size());
            System.out.println("Relaciones ruta-parada cargadas");
            System.out.println();

            System.out.println("Construyendo grafos y generando lista de arcos...");
            System.out.println();
            graphBuilder.buildAndPrintGraphs(routesById, stopsById, lineStopsByRouteAndOrientation);

            System.out.println();
            System.out.println("Generando imágenes de los grafos con Java2D...");
            Path graphsDir = Path.of("GrafosRutasIndividuales");
            imageExporter.exportRouteGraphs(
                    routesById,
                    stopsById,
                    lineStopsByRouteAndOrientation,
                    graphsDir
            );
            System.out.println("Imágenes JPG generadas en la carpeta: " + graphsDir.toAbsolutePath());
            
            System.out.println();
            Path fullGraphDir = Path.of("GrafoRutasCompletas");
            imageExporter.exportFullGraph(
                    routesById,
                    stopsById,
                    lineStopsByRouteAndOrientation,
                    fullGraphDir
            );

        } catch (IOException e) {
            System.err.println("Error leyendo archivos CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
