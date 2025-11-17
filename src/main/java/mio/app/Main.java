package mio.app;

import mio.model.Ruta;
import mio.model.Parada;
import mio.model.ParadaRuta;
import mio.service.CargadorRutas;
import mio.service.CargadorParadas;
import mio.service.CargadorParadasRutas;
import mio.service.ConstructorGrafo;
import mio.service.ExportadorImagenGrafo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

//Procesa datos del MIO y genera grafos
public class Main {

    public static void main(String[] args) {

        // Archivos CSV con los datos
        Path linesCsvPath     = Path.of("src/data/proyecto-mio/MIO/lines-241.csv");
        Path stopsCsvPath     = Path.of("src/data/proyecto-mio/MIO/stops-241.csv");
        Path lineStopsCsvPath = Path.of("src/data/proyecto-mio/MIO/linestops-241.csv");

        // Servicios
        CargadorRutas routeLoader = new CargadorRutas();
        CargadorParadas stopLoader = new CargadorParadas();
        CargadorParadasRutas lineStopLoader = new CargadorParadasRutas();
        ConstructorGrafo graphBuilder = new ConstructorGrafo();
        ExportadorImagenGrafo imageExporter = new ExportadorImagenGrafo();

        try {
            // Carga datos desde CSV
            System.out.println("Cargando datos de los archivos CSV...");
            Map<Integer, Ruta> routesById = routeLoader.loadRoutes(linesCsvPath);
            Map<Integer, Parada> stopsById = stopLoader.loadStops(stopsCsvPath);
            Map<Integer, Map<Integer, List<ParadaRuta>>> lineStopsByRouteAndOrientation =
                    lineStopLoader.loadLineStops(lineStopsCsvPath);
            
            System.out.println("Rutas cargadas: " + routesById.size());
            System.out.println("Paradas cargadas: " + stopsById.size());
            System.out.println("Relaciones ruta-parada cargadas");
            System.out.println();

            // Construye grafos
            System.out.println("Construyendo grafos y generando lista de arcos...");
            System.out.println();
            graphBuilder.buildAndPrintGraphs(routesById, stopsById, lineStopsByRouteAndOrientation);

            // Genera imágenes de grafos individuales
            System.out.println();
            System.out.println("╔═══════════════════════════════════════════════════════════════╗");
            System.out.println("║ Generando imágenes de los grafos de las rutas individuales... ║");
            System.out.println("╚═══════════════════════════════════════════════════════════════╝");
            Path graphsDir = Path.of("GrafosRutasIndividuales");
            imageExporter.exportRouteGraphs(
                    routesById,
                    stopsById,
                    lineStopsByRouteAndOrientation,
                    graphsDir
            );
            
            System.out.println("\nUbicación: " + graphsDir.toAbsolutePath());
            System.out.println();
            
            // Genera grafo completo
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
