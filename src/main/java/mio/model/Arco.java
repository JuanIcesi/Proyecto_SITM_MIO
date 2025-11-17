package mio.model;

// Representa un arco (conexión) entre dos paradas consecutivas
public class Arco {
    private final int origenStopId;
    private final int destinoStopId;

    public Arco(int origenStopId, int destinoStopId) {
        this.origenStopId = origenStopId;
        this.destinoStopId = destinoStopId;
    }

    public int getOrigenStopId() {
        return origenStopId;
    }

    public int getDestinoStopId() {
        return destinoStopId;
    }

    @Override
    public String toString() {
        return origenStopId + " -> " + destinoStopId;
    }
}
