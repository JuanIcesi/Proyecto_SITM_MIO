package mio.service;

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

        for (int lineId : lineIds) {
            Route route = routesById.get(lineId);
            Map<Integer, List<LineStop>> byOrientation = lineStopsByRouteAndOrientation.get(lineId);

            List<Integer> orientations = new ArrayList<>(byOrientation.keySet());
            Collections.sort(orientations);

            for (int orientation : orientations) {
                List<LineStop> stopsSeq = byOrientation.get(orientation);
                stopsSeq.sort(Comparator.comparingInt(LineStop::getSequence));

                String routeName = (route != null ? route.getShortName() : ("LINEID " + lineId));
                String routeDesc = (route != null ? route.getDescription() : "");
                String orientationLabel = orientationLabel(orientation);

                System.out.println("=====================================================");
                System.out.println("Ruta " + routeName + " (ID: " + lineId + ") - " + orientationLabel);
                if (!routeDesc.isEmpty()) {
                    System.out.println(routeDesc);
                }
                System.out.println("Arcos (paradas consecutivas):");
                System.out.println("-----------------------------------------------------");

                for (int i = 0; i < stopsSeq.size() - 1; i++) {
                    LineStop from = stopsSeq.get(i);
                    LineStop to = stopsSeq.get(i + 1);

                    Stop stopFrom = stopsById.get(from.getStopId());
                    Stop stopTo = stopsById.get(to.getStopId());

                    String fromName = (stopFrom != null ? stopFrom.getShortName() : "");
                    String toName = (stopTo != null ? stopTo.getShortName() : "");

                    System.out.printf(
                            "%d (%s) -> %d (%s)%n",
                            from.getStopId(), fromName,
                            to.getStopId(), toName
                    );
                }
                System.out.println();
            }
        }
    }

    private String orientationLabel(int orientation) {
        return switch (orientation) {
            case 0 -> "IDA";
            case 1 -> "REGRESO";
            default -> "ORIENTACION " + orientation;
        };
    }
}
