package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class BruteForceTSP {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final JsonNodeFactory factory = JsonNodeFactory.instance;

    public static class GraphData {
        public int[][] matrix;
        public int city;
    }

    public static class TSPSolution {
        public List<Integer> bestPath;
        public int minCost = Integer.MAX_VALUE;
        public int processedPermutations;
        public int startPermutation;
        public int endPermutation;
    }

    @Solve
    public static ObjectNode findOptimalRoute(Path archivePath, int startPermutation, int batchSize)
            throws IOException {

        GraphData graphData = loadGraphDataFromZip(archivePath);
        int[][] matrix = graphData.matrix;
        int startCity = graphData.city;

        if (startPermutation < 0) {
            throw new IllegalArgumentException("Start permutation must be positive");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be positive");
        }

        TSPSolution solution = new TSPSolution();
        solution.startPermutation = startPermutation;
        solution.endPermutation = startPermutation + batchSize - 1;

        bruteForceTSP(matrix, startCity, startPermutation, batchSize, solution);

        return createJsonResponse(solution, startCity);
    }

    private static void bruteForceTSP(int[][] matrix, int startCity,
                                      int startPermutation, int batchSize,
                                      TSPSolution solution) {
        int n = matrix.length;
        if (n <= 1) {
            solution.bestPath = List.of(startCity);
            solution.minCost = 0;
            return;
        }

        List<Integer> otherCities = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (i != startCity) {
                otherCities.add(i);
            }
        }

        int totalPermutations = factorial(n - 1);
        if (startPermutation >= totalPermutations) {
            return;
        }

        int endPermutation = Math.min(startPermutation + batchSize, totalPermutations);
        solution.processedPermutations = endPermutation - startPermutation;

        // Генерация перестановок с учетом диапазона
        PermutationGenerator generator = new PermutationGenerator(otherCities);
        generator.skip(startPermutation);

        for (int i = startPermutation; i < endPermutation; i++) {
            if (!generator.hasNext()) break;

            List<Integer> permutation = generator.next();
            List<Integer> path = new ArrayList<>();
            path.add(startCity);
            path.addAll(permutation);
            path.add(startCity);

            int currentCost = calculatePathCost(matrix, path);
            if (currentCost < solution.minCost) {
                solution.minCost = currentCost;
                solution.bestPath = new ArrayList<>(path);
            }
        }
    }

    private static int factorial(int n) {
        int result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    private static int calculatePathCost(int[][] matrix, List<Integer> path) {
        int cost = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            int from = path.get(i);
            int to = path.get(i + 1);
            cost += matrix[from][to];
        }
        return cost;
    }

    static class PermutationGenerator implements Iterator<List<Integer>> {
        private List<Integer> elements;
        private int[] indices;
        private boolean hasNext = true;

        public PermutationGenerator(List<Integer> elements) {
            this.elements = new ArrayList<>(elements);
            this.indices = new int[elements.size()];
            for (int i = 0; i < indices.length; i++) {
                indices[i] = i;
            }
        }

        public void skip(int count) {
            for (int i = 0; i < count; i++) {
                if (!hasNext()) break;
                generateNext();
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
            for (int index : indices) {
                result.add(elements.get(index));
            }

            generateNext();
            return result;
        }

        private void generateNext() {
            hasNext = false;
            for (int i = indices.length - 2; i >= 0; i--) {
                if (indices[i] < indices[i + 1]) {
                    int pivot = findPivot(i);
                    swap(i, pivot);
                    reverse(i + 1);
                    hasNext = true;
                    break;
                }
            }
        }

        private int findPivot(int i) {
            int pivot = i + 1;
            for (int j = i + 2; j < indices.length; j++) {
                if (indices[i] < indices[j] && indices[j] < indices[pivot]) {
                    pivot = j;
                }
            }
            return pivot;
        }

        private void swap(int i, int j) {
            int temp = indices[i];
            indices[i] = indices[j];
            indices[j] = temp;
        }

        private void reverse(int from) {
            int i = from;
            int j = indices.length - 1;
            while (i < j) {
                swap(i, j);
                i++;
                j--;
            }
        }
    }

    private static GraphData loadGraphDataFromZip(Path archivePath) throws IOException {
        try (ZipFile zipFile = new ZipFile(archivePath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(".json")) {
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        return objectMapper.readValue(is, GraphData.class);
                    }
                }
            }
        }
        throw new IOException("JSON file not found in archive");
    }

    private static ObjectNode createJsonResponse(TSPSolution solution, int startCity) {
        ObjectNode response = factory.objectNode();

        if (solution.bestPath == null) {
            response.put("error", "No valid route found");
            return response;
        }

        ArrayNode pathArray = factory.arrayNode();
        solution.bestPath.forEach(pathArray::add);
        response.set("optimalPath", pathArray);

        response.put("totalCost", solution.minCost);
        response.put("processedPermutations", solution.processedPermutations);
        response.put("startPermutation", solution.startPermutation);
        response.put("endPermutation", solution.endPermutation);
        response.put("startCity", startCity);

        return response;
    }

}