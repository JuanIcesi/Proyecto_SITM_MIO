package mio.service;

import mio.model.Arco;
import mio.model.LineStop;
import mio.model.Route;
import mio.model.Stop;

import java.util.*;

public class GraphBuilder {

    public void buildAndPrintGraphs(
            Map<Integer, Route> routesById,
            Map<Integer, Stop> stopsById,
            Map<Integer, Map<Integer, List<LineStop>>> lineStopsByRouteAndOrientation
    ) {
        List<Integer> lineIds = new ArrayList<>(lineStopsByRouteAndOrientation.keySet());
        Collections.sort(lineIds);

        int totalArcos = 0;
        int totalRutas = 0;

        System.out.println("=================================================================");
        System.out.println("GRAFOS DE RUTAS DEL SITM-MIO - LISTA DE ARCOS POR RUTA");
        System.out.println("=================================================================");
        System.out.println();

        for (int lineId : lineIds) {
            Route route = routesById.get(lineId);
            Map<Integer, List<LineStop>> byOrientation = lineStopsByRouteAndOrientation.get(lineId);

            List<Integer> orientations = new ArrayList<>(byOrientation.keySet());
            Collections.sort(orientations);

            String routeName = (route != null ? route.getShortName() : ("LINEID " + lineId));
            String routeDesc = (route != null ? route.getDescription() : "");

            System.out.println("=================================================================");
            System.out.println("RUTA: " + routeName + " (ID: " + lineId + ")");
            if (!routeDesc.isEmpty()) {
                System.out.println("Descripcion: " + routeDesc);
            }
            System.out.println("=================================================================");

            for (int orientation : orientations) {
                List<LineStop> stopsSeq = new ArrayList<>(byOrientation.get(orientation));
                stopsSeq.sort(Comparator.comparingInt(LineStop::getSequence));

                String orientationLabel = orientationLabel(orientation);
                
                List<Arco> arcos = buildArcos(stopsSeq);
                totalArcos += arcos.size();

                System.out.println();
                System.out.println("--- " + orientationLabel + " ---");
                System.out.println("Paradas: " + stopsSeq.size() + " | Arcos: " + arcos.size());
                System.out.println("Secuencia de arcos:");

                for (int i = 0; i < arcos.size(); i++) {
                    Arco arco = arcos.get(i);
                    Stop stopFrom = stopsById.get(arco.getOrigenStopId());
                    Stop stopTo = stopsById.get(arco.getDestinoStopId());

                    String fromName = (stopFrom != null ? stopFrom.getShortName() : "N/A");
                    String toName = (stopTo != null ? stopTo.getShortName() : "N/A");

                    System.out.printf("  %3d. [%d -> %d] %s -> %s%n",
                            i + 1,
                            arco.getOrigenStopId(),
                            arco.getDestinoStopId(),
                            fromName,
                            toName);
                }
            }

            totalRutas++;
            System.out.println();
        }

        System.out.println("=================================================================");
        System.out.println("RESUMEN");
        System.out.println("=================================================================");
        System.out.println("Total de rutas procesadas: " + totalRutas);
        System.out.println("Total de arcos generados: " + totalArcos);
        System.out.println("=================================================================");
    }

    private List<Arco> buildArcos(List<LineStop> stopsSeq) {
        List<Arco> arcos = new ArrayList<>();
        
        for (int i = 0; i < stopsSeq.size() - 1; i++) {
            LineStop from = stopsSeq.get(i);
            LineStop to = stopsSeq.get(i + 1);
            Arco arco = new Arco(from.getStopId(), to.getStopId());
            arcos.add(arco);
        }
        
        return arcos;
    }

    private String orientationLabel(int orientation) {
        if (orientation == 0) {
            return "IDA";
        } else if (orientation == 1) {
            return "REGRESO";
        } else {
            return "ORIENTACION " + orientation;
        }
    }
}
