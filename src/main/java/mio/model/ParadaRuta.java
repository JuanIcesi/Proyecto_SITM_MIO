package mio.model;

// Relación entre una ruta y una parada (con secuencia y orientación)
public class ParadaRuta {
    private final int lineId;
    private final int stopId;
    private final int sequence;
    private final int orientation;

    public ParadaRuta(int lineId, int stopId, int sequence, int orientation) {
        this.lineId = lineId;
        this.stopId = stopId;
        this.sequence = sequence;
        this.orientation = orientation;
    }

    public int getLineId() {
        return lineId;
    }

    public int getStopId() {
        return stopId;
    }

    public int getSequence() {
        return sequence;
    }

    public int getOrientation() {
        return orientation;
    }
}

