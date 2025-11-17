package mio.service;

import mio.model.Arco;
import mio.model.ParadaRuta;
import mio.model.Ruta;
import mio.model.Parada;

import java.util.*;

// Construye grafos a partir de rutas y paradas
public class ConstructorGrafo {

    // Construye y muestra grafos de todas las rutas
    public void buildAndPrintGraphs(
            Map<Integer, Ruta> routesById,
            Map<Integer, Parada> stopsById,
            Map<Integer, Map<Integer, List<ParadaRuta>>> lineStopsByRouteAndOrientation
    ) {
        // agarro todas las rutas y las ordeno
        List<Integer> lineIds = new ArrayList<>(lineStopsByRouteAndOrientation.keySet());
        Collections.sort(lineIds);

        int totalArcos = 0;
        int totalRutas = 0;

        // imprimo el título
        System.out.println("=================================================================");
        System.out.println("GRAFOS DE RUTAS DEL SITM-MIO - LISTA DE ARCOS POR RUTA");
        System.out.println("=================================================================");
        System.out.println();

        // voy ruta por ruta
        for (int lineId : lineIds) {
            Ruta route = routesById.get(lineId);
            // las paradas vienen agrupadas por orientación (ida o regreso)
            Map<Integer, List<ParadaRuta>> byOrientation = lineStopsByRouteAndOrientation.get(lineId);

            // veo qué orientaciones tiene esta ruta (0=ida, 1=regreso)
            List<Integer> orientations = new ArrayList<>(byOrientation.keySet());
            Collections.sort(orientations);

            String routeName = (route != null ? route.getShortName() : ("LINEID " + lineId));
            String routeDesc = (route != null ? route.getDescription() : "");

            // muestro info de la ruta
            System.out.println("=================================================================");
            System.out.println("RUTA: " + routeName + " (ID: " + lineId + ")");
            if (!routeDesc.isEmpty()) {
                System.out.println("Descripcion: " + routeDesc);
            }
            System.out.println("=================================================================");

            // ahora proceso ida y regreso por separado
            for (int orientation : orientations) {
                // ordeno las paradas por secuencia (1, 2, 3...)
                List<ParadaRuta> stopsSeq = new ArrayList<>(byOrientation.get(orientation));
                stopsSeq.sort(Comparator.comparingInt(ParadaRuta::getSequence));

                String orientationLabel = orientationLabel(orientation);
                
                // creo los arcos conectando parada 1->2, 2->3, etc
                List<Arco> arcos = buildArcos(stopsSeq);
                totalArcos += arcos.size();

                // muestro info de esta orientación
                System.out.println();
                System.out.println("--- " + orientationLabel + " ---");
                System.out.println("Paradas: " + stopsSeq.size() + " | Arcos: " + arcos.size());
                System.out.println("Secuencia de arcos:");

                // imprimo cada arco con sus paradas
                for (int i = 0; i < arcos.size(); i++) {
                    Arco arco = arcos.get(i);
                    Parada stopFrom = stopsById.get(arco.getOrigenStopId());
                    Parada stopTo = stopsById.get(arco.getDestinoStopId());

                    String fromName = (stopFrom != null ? stopFrom.getShortName() : "N/A");
                    String toName = (stopTo != null ? stopTo.getShortName() : "N/A");

                    // formato: número, IDs, nombres
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

        System.out.println("╔═════════════════════════════════╗");
        System.out.println("║            RESUMEN              ║");
        System.out.println("╚═════════════════════════════════╝");
        System.out.println("- Total de rutas procesadas: " + totalRutas);
        System.out.println("- Total de arcos generados: " + totalArcos);
    }

    // creo los arcos: parada 1->2, 2->3, 3->4, etc
    private List<Arco> buildArcos(List<ParadaRuta> stopsSeq) {
        List<Arco> arcos = new ArrayList<>();
        
        // voy hasta size-1 porque la última no tiene siguiente
        for (int i = 0; i < stopsSeq.size() - 1; i++) {
            ParadaRuta from = stopsSeq.get(i);
            ParadaRuta to = stopsSeq.get(i + 1);
            // creo el arco de esta parada a la siguiente
            Arco arco = new Arco(from.getStopId(), to.getStopId());
            arcos.add(arco);
        }
        
        return arcos;
    }

    // convierto 0 a "IDA" y 1 a "REGRESO"
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

