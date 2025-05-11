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


    public static class GraphData {
        public int[][] matrix;
        public int city;
    }

    @End
    public static int countPossibleRoutesInZip(String requestJson) throws IOException {
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
                GraphData graphData = objectMapper.readValue(is, GraphData.class);

                if (graphData.matrix == null || graphData.matrix.length == 0) {
                    throw new IOException("Матрица городов пуста или некорректна");
                }

                int cityCount = graphData.matrix.length;

                // Для задачи коммивояжера количество возможных маршрутов:
                // (n-1)! / 2 для симметричной матрицы (учитываем обратные маршруты)
                // или (n-1)! для асимметричной

                boolean isSymmetric = isMatrixSymmetric(graphData.matrix);
                int possibleRoutes = factorial(cityCount - 1);

                if (isSymmetric) {
                    possibleRoutes /= 2;
                }

                return possibleRoutes;
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

    private static boolean isMatrixSymmetric(int[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < i; j++) {
                if (matrix[i][j] != matrix[j][i]) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int factorial(int n) {
        if (n < 0) return 0;
        int result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
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
        if (!currentResult.has("totalCost") || !currentResult.has("optimalPath")) {
            throw new IllegalArgumentException("Result must contain 'totalCost' and 'optimalPath' fields");
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
                System.out.println("Новая стоимость: " + currentCost + " (было: " + existingCost + ")");
                System.out.println("Обработано перестановок: " + currentResult.get("processedPermutations").asInt());
            } else {
                System.out.println("Текущий результат не лучше существующего (текущий: " +
                        currentCost + ", существующий: " + existingCost + ")");
            }
        } catch (IOException e) {
            System.err.println("Ошибка чтения файла результатов: " + e.getMessage());
            System.out.println("Создаем новый файл результатов");
            saveResult(currentResultJson);
        }
    }

    private static void saveResult(String json) throws IOException {
        try {
            Files.writeString(RESULT_FILE, json,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Результат успешно сохранен в " + RESULT_FILE);
        } catch (IOException e) {
            System.err.println("Ошибка сохранения результата: " + e.getMessage());
            throw e;
        }
    }

}