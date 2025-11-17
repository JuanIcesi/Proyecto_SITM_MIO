package mio.model;

// Representa una parada del sistema MIO con coordenadas geográficas
public class Parada {
    private final int stopId;
    private final String shortName;
    private final String longName;
    private final double lat;
    private final double lon;

    public Parada(int stopId, String shortName, String longName, double lat, double lon) {
        this.stopId = stopId;
        this.shortName = shortName;
        this.longName = longName;
        this.lat = lat;
        this.lon = lon;
    }

    public int getStopId() {
        return stopId;
    }

    public String getShortName() {
        return shortName;
    }

    public String getLongName() {
        return longName;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }
}

