package mio.app;

import mio.model.Route;
import mio.model.Stop;
import mio.model.LineStop;
import mio.service.RouteLoader;
import mio.service.StopLoader;
import mio.service.LineStopLoader;
import mio.service.GraphBuilder;
import mio.service.GraphImageExporter;   // 👈 importa el exportador de imágenes

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {

        // Rutas a los CSV (tal como los tienes ahora)
        Path linesCsvPath     = Path.of("src/data/proyecto-mio/MIO/lines-241.csv");
        Path stopsCsvPath     = Path.of("src/data/proyecto-mio/MIO/stops-241.csv");
        Path lineStopsCsvPath = Path.of("src/data/proyecto-mio/MIO/linestops-241.csv");

        RouteLoader routeLoader = new RouteLoader();
        StopLoader stopLoader = new StopLoader();
        LineStopLoader lineStopLoader = new LineStopLoader();
        GraphBuilder graphBuilder = new GraphBuilder();
        GraphImageExporter imageExporter = new GraphImageExporter(); // 👈 nuevo

        try {
            Map<Integer, Route> routesById = routeLoader.loadRoutes(linesCsvPath);
            Map<Integer, Stop> stopsById = stopLoader.loadStops(stopsCsvPath);
            Map<Integer, Map<Integer, List<LineStop>>> lineStopsByRouteAndOrientation =
                    lineStopLoader.loadLineStops(lineStopsCsvPath);

            // Parte A: imprimir los grafos en consola
            graphBuilder.buildAndPrintGraphs(routesById, stopsById, lineStopsByRouteAndOrientation);

            // BONUS: exportar imágenes de los grafos a la carpeta "graphs"
            Path graphsDir = Path.of("graphs");
            imageExporter.exportRouteGraphs(
                    routesById,
                    stopsById,
                    lineStopsByRouteAndOrientation,
                    graphsDir
            );

        } catch (IOException e) {
            System.err.println("Error leyendo archivos CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
