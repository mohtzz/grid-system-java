package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Enumeration;
import java.util.Map;
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



    @Process
    public static void saveIfBetter(String currentResultJson) throws IOException {
        // 1. Проверка входных данных
        if (currentResultJson == null || currentResultJson.isEmpty()) {
            throw new IllegalArgumentException("Result JSON string cannot be null or empty");
        }

        // 2. Парсинг JSON строки
        JsonNode currentResult;
        try {
            currentResult = objectMapper.readTree(currentResultJson);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid JSON format", e);
        }

        // 3. Проверка обязательных полей
        if (!currentResult.has("totalCost") || !currentResult.has("route")) {
            throw new IllegalArgumentException("Result must contain 'totalCost' and 'route' fields");
        }

        int currentCost = currentResult.get("totalCost").asInt();

        // 4. Обработка случая, когда файла не существует
        if (!Files.exists(RESULT_FILE)) {
            saveResult(currentResultJson);
            System.out.println("Файл не существовал, сохранен новый результат");
            return;
        }

        // 5. Чтение существующего результата
        try {
            String existingJson = Files.readString(RESULT_FILE);
            JsonNode existingResult = objectMapper.readTree(existingJson);

            if (!existingResult.has("totalCost")) {
                System.out.println("Существующий файл поврежден, будет перезаписан");
                saveResult(currentResultJson);
                return;
            }

            int existingCost = existingResult.get("totalCost").asInt();

            // 6. Сравнение и сохранение
            if (currentCost < existingCost) {
                saveResult(currentResultJson);
                System.out.println("Найден более оптимальный маршрут! Файл обновлен.");
            } else {
                System.out.println("Текущий результат не лучше существующего.");
            }
        } catch (IOException e) {
            System.err.println("Ошибка чтения файла результатов: " + e.getMessage());
            System.out.println("Создаем новый файл результатов");
            saveResult(currentResultJson);
        }
    }

    private static void saveResult(String json) throws IOException {
        try {
            Files.writeString(RESULT_FILE, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Ошибка сохранения результата: " + e.getMessage());
            throw e;
        }
    }

}