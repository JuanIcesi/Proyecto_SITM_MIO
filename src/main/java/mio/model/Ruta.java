package mio.model;

// Representa una ruta del sistema MIO
public class Ruta {
    private final int lineId;
    private final String shortName;
    private final String description;

    public Ruta(int lineId, String shortName, String description) {
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

