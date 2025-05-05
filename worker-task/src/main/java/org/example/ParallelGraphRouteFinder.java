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
import java.util.concurrent.atomic.AtomicInteger;
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

        if (startRange < 0 || startRange >= matrix.length) {
            throw new IllegalArgumentException("Start range out of matrix bounds");
        }


        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<Future<RouteResult>> futures = new ArrayList<>();

        int endRange = Math.min(startRange + batchSize, matrix.length);
        for (int startCity = startRange; startCity < endRange; startCity++) {
            final int currentStartCity = startCity;
            futures.add(executor.submit(() -> solveGenetic(matrix, currentStartCity)));
        }

        List<RouteResult> results = new ArrayList<>();
        for (Future<RouteResult> future : futures) {
            RouteResult result = future.get();
            if (result != null) {
                results.add(result);
            }
        }

        executor.shutdown();
        return createJsonResponse(results, startRange, endRange);
    }

    private static RouteResult solveGenetic(int[][] matrix, int startCity) {
        int populationSize = 100;
        int generations = 200;
        double mutationRate = 0.1;
        int n = matrix.length;

        List<List<Integer>> population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            List<Integer> route = new ArrayList<>();
            for (int j = 0; j < n; j++) if (j != startCity) route.add(j);
            Collections.shuffle(route);
            route.add(0, startCity);
            population.add(route);
        }

        RouteResult best = new RouteResult(null, Integer.MAX_VALUE);

        for (int gen = 0; gen < generations; gen++) {
            for (List<Integer> route : population) {
                if (route.get(0) != startCity) continue;
                int cost = 0;
                for (int i = 0; i < route.size(); i++) {
                    int from = route.get(i);
                    int to = route.get((i + 1) % route.size());
                    cost += matrix[from][to];
                }

                if (cost < best.cost) {
                    best = new RouteResult(new ArrayList<>(route), cost);
                }
            }

            List<List<Integer>> newPopulation = new ArrayList<>();
            while (newPopulation.size() < populationSize) {
                List<Integer> parent1 = selectParent(population, matrix);
                List<Integer> parent2 = selectParent(population, matrix);
                List<Integer> child = crossover(parent1, parent2, startCity);
                mutate(child, mutationRate, startCity);
                newPopulation.add(child);
            }
            population = newPopulation;
        }

        if (best.route != null) {
            if (best.route.get(0) != startCity) {
                best.route.remove(Integer.valueOf(startCity));
                best.route.add(0, startCity);
            }
            best.route.add(startCity); // Замыкаем цикл
            best.cost = calculateRouteCost(matrix, best.route);

            if (best.route.get(0) != startCity) {
                return null;
            }
        }

        return best.route != null ? best : null;
    }

    private static int calculateRouteCost(int[][] matrix, List<Integer> route) {
        int cost = 0;
        for (int i = 0; i < route.size() - 1; i++) {
            cost += matrix[route.get(i)][route.get(i+1)];
        }
        cost += matrix[route.get(route.size()-1)][route.get(0)];
        return cost;
    }

    private static List<Integer> selectParent(List<List<Integer>> population, int[][] matrix) {
        Collections.shuffle(population);
        return population.stream()
                .limit(3)
                .min(Comparator.comparingInt(r -> calculateRouteCost(matrix, r)))
                .orElse(population.get(0));
    }

    private static List<Integer> crossover(List<Integer> parent1, List<Integer> parent2, int startCity) {
        int n = parent1.size();
        List<Integer> child = new ArrayList<>(Collections.nCopies(n, -1));
        child.set(0, startCity);
        int a = 1 + (int)(Math.random() * (n/2));
        int b = a + (int)(Math.random() * (n/2));

        for (int i = a; i <= b && i < parent1.size(); i++) {
            int city = parent1.get(i);
            if (city != startCity) {
                child.set(i, city);
            }
        }

        int currentPos = 1;
        for (int city : parent2) {
            if (city != startCity && !child.contains(city)) {
                while (currentPos < n && child.get(currentPos) != -1) {
                    currentPos++;
                }
                if (currentPos < n) {
                    child.set(currentPos, city);
                }
            }
        }

        return child;
    }

    private static void mutate(List<Integer> route, double mutationRate, int startCity) {
        if (Math.random() < mutationRate) {
            int i = 1 + (int)(Math.random() * (route.size()-2));
            int j = 1 + (int)(Math.random() * (route.size()-2));
            Collections.swap(route, i, j);
        }
    }

    private static ObjectNode createJsonResponse(List<RouteResult> results, int startRange, int endRange) {
        ObjectNode response = factory.objectNode();
        if (results.isEmpty()) {
            response.put("error", "No valid routes found");
            return response;
        }

        List<RouteResult> validResults = results.stream()
                .filter(r -> r.route != null
                        && !r.route.isEmpty()
                        && r.route.get(0) == startRange)
                .toList();

        if (validResults.isEmpty()) {
            response.put("error", "No routes found within specified range");
            return response;
        }

        RouteResult bestRoute = Collections.min(validResults, Comparator.comparingInt(r -> r.cost));
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

    static class RouteResult {
        List<Integer> route;
        int cost;

        RouteResult(List<Integer> route, int cost) {
            this.route = route;
            this.cost = cost;
        }
    }
}