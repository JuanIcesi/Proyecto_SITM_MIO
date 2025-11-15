package mio.app.app;

import mio.model.Route;
import mio.model.Stop;
import mio.model.LineStop;
import mio.service.RouteLoader;
import mio.service.StopLoader;
import mio.service.LineStopLoader;
import mio.service.GraphBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        // 🔧 Ajusta estas rutas a donde tengas los CSV
        Path linesCsvPath = Path.of("data/lines.csv");
        Path stopsCsvPath = Path.of("data/stops.csv");
        Path lineStopsCsvPath = Path.of("data/linestops.csv");

        RouteLoader routeLoader = new RouteLoader();
        StopLoader stopLoader = new StopLoader();
        LineStopLoader lineStopLoader = new LineStopLoader();
        GraphBuilder graphBuilder = new GraphBuilder();

        try {
            Map<Integer, Route> routesById = routeLoader.loadRoutes(linesCsvPath);
            Map<Integer, Stop> stopsById = stopLoader.loadStops(stopsCsvPath);
            Map<Integer, Map<Integer, List<LineStop>>> lineStopsByRouteAndOrientation =
                    lineStopLoader.loadLineStops(lineStopsCsvPath);

            graphBuilder.buildAndPrintGraphs(routesById, stopsById, lineStopsByRouteAndOrientation);

        } catch (IOException e) {
            System.err.println("Error leyendo archivos CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
