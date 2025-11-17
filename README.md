# TAREA ANÁLISIS – GRAFO DE ARCOS SITM-MIO

## Integrantes

- Daniel Trujillo Marin - A00398810
- Juan Pablo Sinisterra - A00402548
- Juan Esteban Cuellar - A00402333

## Contexto del Problema

El Sistema Integrado de Transporte Masivo del Valle del Cauca (SITM-MIO) cuenta con múltiples rutas de transporte público, cada una compuesta por una secuencia de paradas. Cada ruta tiene dos sentidos de circulación: ida y regreso, donde la secuencia de paradas puede diferir entre ambos sentidos.

Para el análisis y cálculo de velocidades promedio de los arcos en las rutas, es necesario construir los grafos correspondientes donde:
- Los nodos representan las paradas del sistema
- Las aristas (arcos) representan las conexiones entre paradas consecutivas según las rutas definidas

Cada ruta determina dos grafos independientes (uno para ida y otro para regreso), donde los arcos se definen por la secuencia de paradas establecida en cada sentido.

## Datos del Sistema

En este proyecto utilizamos tres archivos CSV que contienen la información del sistema:

- **RUTAS (lines.csv)**: 105 rutas del sistema
- **PARADAS (stops.csv)**: 2.119 paradas disponibles
- **Paradas Por Ruta (linestops.csv)**: 7.368 relaciones ruta-parada

## Objetivos

1. Construir los grafos de paradas y arcos a partir de los archivos CSV
2. Mostrar la lista ordenada de arcos en secuencia (ida y regreso) por ruta en consola
3. Generar visualizaciones gráficas de los grafos usando Java2D y exportarlas como imágenes JPG

## Estructura del Proyecto

```
Proyecto_SITM_MIO/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── mio/
│   │   │       ├── app/
│   │   │       │   └── Main.java
│   │   │       ├── model/
│   │   │       │   ├── Arco.java
│   │   │       │   ├── LineStop.java
│   │   │       │   ├── Route.java
│   │   │       │   └── Stop.java
│   │   │       ├── service/
│   │   │       │   ├── GraphBuilder.java
│   │   │       │   ├── GraphImageExporter.java
│   │   │       │   ├── LineStopLoader.java
│   │   │       │   ├── RouteLoader.java
│   │   │       │   └── StopLoader.java
│   │   │       └── Util/
│   │   │           └── CsvUtils.java
│   │   └── data/
│   │       └── proyecto-mio/
│   │           └── MIO/
│   │               ├── lines-241.csv
│   │               ├── stops-241.csv
│   │               └── linestops-241.csv
├── GrafosRutasIndividuales/
│   └── (181 imágenes JPG de rutas individuales)
├── GrafoRutasCompletas/
│   └── Grafo_Completo_MIO.jpg
└── pom.xml
```

## Requisitos

- Java 11 o superior
- Maven (para gestión de dependencias)

## Compilación y Ejecución

### Compilación

```bash
javac -d target/classes -sourcepath src/main/java src/main/java/mio/app/Main.java src/main/java/mio/service/*.java src/main/java/mio/model/*.java src/main/java/mio/Util/*.java
```

### Ejecución

```bash
java -cp target/classes mio.app.Main
```