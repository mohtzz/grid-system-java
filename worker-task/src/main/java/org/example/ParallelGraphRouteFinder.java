package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ParallelGraphRouteFinder {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final JsonNodeFactory factory = JsonNodeFactory.instance;

    @Solve
    public static ObjectNode findCheapestRoutesParallel(Path archivePath, int startRange, int batchSize)
            throws IOException, ExecutionException, InterruptedException {

        int[][] matrix = loadMatrixFromZip(archivePath);
        validateParameters(matrix, startRange, batchSize);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<Future<RouteResult>> futures = new ArrayList<>();

        int endRange = Math.min(startRange + batchSize, matrix.length);
        for (int startCity = startRange; startCity < endRange; startCity++) {
            final int currentStartCity = startCity;
            futures.add(executor.submit(() -> findBestRoute(matrix, currentStartCity)));
        }

        List<RouteResult> results = new ArrayList<>();
        for (Future<RouteResult> future : futures) {
            RouteResult result = future.get();
            if (result != null) {
                results.add(result);
            }
        }

        executor.shutdown();
        return createJsonResponse(results);
    }

    private static ObjectNode createJsonResponse(List<RouteResult> results) {
        ObjectNode response = factory.objectNode();

        if (results.isEmpty()) {
            response.put("error", "No valid routes found");
            return response;
        }

        RouteResult bestRoute = Collections.min(results, Comparator.comparingInt(r -> r.cost));

        ArrayNode routeArray = factory.arrayNode();
        bestRoute.route.forEach(routeArray::add);
        response.set("route", routeArray);

        response.put("totalCost", bestRoute.cost);

        return response;
    }

    private static int[][] loadMatrixFromZip(Path archivePath) throws IOException {
        try (ZipFile zipFile = new ZipFile(archivePath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(".json")) {
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        return objectMapper.readValue(is, int[][].class);
                    }
                }
            }
        }
        throw new IOException("JSON file with matrix not found in archive at " + archivePath);
    }

    private static void validateParameters(int[][] matrix, int startRange, int batchSize) {
        if (matrix == null || matrix.length == 0) {
            throw new IllegalArgumentException("Invalid adjacency matrix");
        }
        if (startRange < 0 || startRange >= matrix.length) {
            throw new IllegalArgumentException("Start range out of bounds");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be positive");
        }
    }

    private static RouteResult findBestRoute(int[][] matrix, int startCity) {
        boolean[] visited = new boolean[matrix.length];
        List<Integer> currentRoute = new ArrayList<>();
        currentRoute.add(startCity);
        visited[startCity] = true;

        AtomicReference<RouteResult> bestResult = new AtomicReference<>(new RouteResult(null, Integer.MAX_VALUE));
        findRoutes(matrix, startCity, visited, currentRoute, 0, bestResult);

        return bestResult.get().route != null ? bestResult.get() : null;
    }

    private static void findRoutes(int[][] matrix, int currentCity,
                                   boolean[] visited, List<Integer> currentRoute,
                                   int currentCost, AtomicReference<RouteResult> bestResult) {
        if (currentRoute.size() == matrix.length) {
            synchronized (bestResult) {
                if (currentCost < bestResult.get().cost) {
                    bestResult.set(new RouteResult(new ArrayList<>(currentRoute), currentCost));
                }
            }
            return;
        }

        for (int nextCity = 0; nextCity < matrix.length; nextCity++) {
            if (!visited[nextCity] && matrix[currentCity][nextCity] > 0) {
                visited[nextCity] = true;
                currentRoute.add(nextCity);
                int newCost = currentCost + matrix[currentCity][nextCity];

                if (newCost < bestResult.get().cost) {
                    findRoutes(matrix, nextCity, visited, currentRoute, newCost, bestResult);
                }

                currentRoute.remove(currentRoute.size() - 1);
                visited[nextCity] = false;
            }
        }
    }

    static class RouteResult {
        List<Integer> route;
        int cost;

        RouteResult(List<Integer> route, int cost) {
            this.route = route;
            this.cost = cost;
        }
    }

}