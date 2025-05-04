package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ResultSaver {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Path RESULT_FILE = Paths.get("best_routes.json");

    @End
    public static int countCitiesInZip(String requestJson) throws IOException {
        JsonNode jsonNode = objectMapper.readTree(requestJson);
        String zipPath = jsonNode.path("dataZip").asText();
        if (zipPath == null || zipPath.isEmpty()) {
            throw new IOException("Не указан путь к ZIP-архиву в поле 'dataZip'");
        }

        try (ZipFile zipFile = new ZipFile(zipPath)) {
            ZipEntry jsonEntry = findFirstJsonEntry(zipFile);
            if (jsonEntry == null) {
                throw new IOException("JSON файл не найден в архиве");
            }

            try (InputStream is = zipFile.getInputStream(jsonEntry)) {
                int[][] matrix = objectMapper.readValue(is, int[][].class);
                if (matrix == null || matrix.length == 0) {
                    throw new IOException("Матрица городов пуста или некорректна");
                }
                return matrix.length;
            }
        }
    }

    @Process
    public static void saveIfBetter(String currentResultJson) throws IOException {
        if (currentResultJson == null || currentResultJson.isEmpty()) {
            throw new IllegalArgumentException("Result JSON string cannot be null or empty");
        }

        JsonNode currentResult = objectMapper.readTree(currentResultJson);
        if (!currentResult.has("totalCost") || !currentResult.has("route")) {
            throw new IllegalArgumentException("Result must contain 'totalCost' and 'route' fields");
        }

        if (!Files.exists(RESULT_FILE)) {
            saveResult(currentResultJson);
            return;
        }

        try {
            String existingJson = Files.readString(RESULT_FILE);
            JsonNode existingResult = objectMapper.readTree(existingJson);
            if (existingResult.has("totalCost") &&
                    currentResult.get("totalCost").asInt() < existingResult.get("totalCost").asInt()) {
                saveResult(currentResultJson);
            }
        } catch (IOException e) {
            saveResult(currentResultJson);
        }
    }

    private static void saveResult(String json) throws IOException {
        Files.writeString(RESULT_FILE, json,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static ZipEntry findFirstJsonEntry(ZipFile zipFile) {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".json")) {
                return entry;
            }
        }
        return null;
    }
}