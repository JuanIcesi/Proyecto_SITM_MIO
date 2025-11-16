package mio.model;

public class LineStop {
    private final int lineId;
    private final int stopId;
    private final int sequence;
    private final int orientation;

    public LineStop(int lineId, int stopId, int sequence, int orientation) {
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
