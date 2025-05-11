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

    public static ObjectNode findCheapestRoutesParallel(Path archivePath, int startRange, int batchSize)
            throws IOException, ExecutionException, InterruptedException {

        // Загружаем матрицу и получаем стартовый город
        MatrixData matrixData = loadMatrixFromZip(archivePath);
        int[][] matrix = matrixData.array;
        int startCity = matrixData.city;

        validateParameters(matrix, startRange, batchSize);

        // Вычисляем общее количество перестановок и проверяем границы
        long totalPermutations = factorial(matrix.length - 1);
        if (startRange < 0 || startRange >= totalPermutations) {
            throw new IllegalArgumentException("Start range out of bounds");
        }
        long endRange = Math.min(startRange + batchSize, totalPermutations);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        AtomicReference<RouteResult> bestResult = new AtomicReference<>(new RouteResult(null, Integer.MAX_VALUE));

        // Создаем задачу для обработки части перестановок
        Future<?> future = executor.submit(() -> {
            findRoutesInRange(matrix, startCity, startRange, endRange, bestResult);
        });

        future.get(); // Ждем завершения задачи
        executor.shutdown();

        return createJsonResponse(Collections.singletonList(bestResult.get()));
    }

    private static void findRoutesInRange(int[][] matrix, int startCity, long startRange, long endRange,
                                          AtomicReference<RouteResult> bestResult) {
        int n = matrix.length;
        List<Integer> cities = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (i != startCity) {
                cities.add(i);
            }
        }

        // Используем итератор перестановок с пропуском первых startRange перестановок
        PermutationIterator permIterator = new PermutationIterator(cities, startRange);
        long count = 0;

        while (permIterator.hasNext() && count < (endRange - startRange)) {
            List<Integer> permutation = permIterator.next();
            count++;

            // Строим полный маршрут (стартовый город + перестановка)
            List<Integer> route = new ArrayList<>();
            route.add(startCity);
            route.addAll(permutation);

            // Вычисляем стоимость маршрута
            int cost = calculateRouteCost(matrix, route);

            // Обновляем лучший результат, если нашли лучше
            synchronized (bestResult) {
                if (cost < bestResult.get().cost) {
                    bestResult.set(new RouteResult(new ArrayList<>(route), cost));
                }
            }
        }
    }

    private static int calculateRouteCost(int[][] matrix, List<Integer> route) {
        int cost = 0;
        for (int i = 0; i < route.size() - 1; i++) {
            int from = route.get(i);
            int to = route.get(i + 1);
            if (matrix[from][to] == 0) {
                return Integer.MAX_VALUE; // Нет пути, маршрут невалиден
            }
            cost += matrix[from][to];
        }
        return cost;
    }

    private static MatrixData loadMatrixFromZip(Path archivePath) throws IOException {
        try (ZipFile zipFile = new ZipFile(archivePath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(".json")) {
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        return objectMapper.readValue(is, MatrixData.class);
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
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be positive");
        }
    }

    private static ObjectNode createJsonResponse(List<RouteResult> results) {
        ObjectNode response = factory.objectNode();

        if (results.isEmpty() || results.get(0).route == null) {
            response.put("error", "No valid routes found");
            return response;
        }

        RouteResult bestRoute = results.get(0);

        ArrayNode routeArray = factory.arrayNode();
        bestRoute.route.forEach(routeArray::add);
        response.set("route", routeArray);

        response.put("totalCost", bestRoute.cost);

        return response;
    }

    private static long factorial(int n) {
        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    static class RouteResult {
        List<Integer> route;
        int cost;

        RouteResult(List<Integer> route, int cost) {
            this.route = route;
            this.cost = cost;
        }
    }

    static class MatrixData {
        public int[][] array;
        public int city;
    }

    // Итератор перестановок с возможностью пропустить первые N перестановок
    static class PermutationIterator implements Iterator<List<Integer>> {
        private List<Integer> elements;
        private int[] indices;
        private long skipped;
        private boolean hasNext;

        public PermutationIterator(List<Integer> elements, long skip) {
            this.elements = new ArrayList<>(elements);
            this.indices = new int[elements.size()];
            this.skipped = 0;
            this.hasNext = true;

            // Пропускаем первые skip перестановок
            while (skipped < skip && hasNext) {
                hasNext = nextPermutation();
                skipped++;
            }
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public List<Integer> next() {
            if (!hasNext) {
                throw new NoSuchElementException();
            }

            List<Integer> result = new ArrayList<>();
            for (int i = 0; i < indices.length; i++) {
                result.add(elements.get(indices[i]));
            }

            hasNext = nextPermutation();
            return result;
        }

        private boolean nextPermutation() {
            int i = indices.length - 2;
            while (i >= 0 && indices[i] >= indices[i + 1]) {
                i--;
            }

            if (i < 0) {
                return false;
            }

            int j = indices.length - 1;
            while (indices[j] <= indices[i]) {
                j--;
            }

            swap(indices, i, j);
            reverse(indices, i + 1, indices.length - 1);
            return true;
        }

        private void swap(int[] arr, int i, int j) {
            int temp = arr[i];
            arr[i] = arr[j];
            arr[j] = temp;
        }

        private void reverse(int[] arr, int i, int j) {
            while (i < j) {
                swap(arr, i++, j--);
            }
        }
    }
}
