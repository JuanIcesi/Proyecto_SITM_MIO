package mio.model;

public class Route {
    private final int lineId;
    private final String shortName;
    private final String description;

    public Route(int lineId, String shortName, String description) {
        this.lineId = lineId;
        this.shortName = shortName;
        this.description = description;
    }

    public int getLineId() {
        return lineId;
    }

    public String getShortName() {
        return shortName;
    }

    public String getDescription() {
        return description;
    }
}
