package com.melancholia.distributor.distributor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.melancholia.distributor.core.ApiBody;
import com.melancholia.distributor.dto.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class DistributorController {

    @Autowired
    private DistributorService distributorService;
    @Autowired
    private DistributorJarService distributorJarService;
    @Autowired
    private TaskManager taskManager;
    @Value("${upload-path}")
    private String UPLOAD_PATH;
    private static final ObjectMapper mapper = new ObjectMapper();
    private long startTime;

    @GetMapping("/start")
    public ResponseEntity<ApiBody> startTask(@RequestParam("zipName") String zipName) {

        if (!isZipFileValid(zipName)) {
            ApiBody response = new ApiBody("No such file exists", HttpStatus.NOT_FOUND.value(), null);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        System.out.println("Получена новая задача. Архив: " + zipName);
        distributorService.startTask(zipName);

        startTime = System.currentTimeMillis();

        ApiBody response = new ApiBody("Задача запущена", HttpStatus.OK.value(), null);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public boolean isZipFileValid(String zipName) {
        try {
            if (zipName == null || zipName.isEmpty()) {
                return false;
            }

            Path filePath = Paths.get(UPLOAD_PATH, zipName);
            return Files.exists(filePath)
                    && Files.isRegularFile(filePath)
                    && zipName.toLowerCase().endsWith(".zip");
        } catch (Exception e) {
            System.err.println("Ошибка при валидации архива " + zipName + e.getMessage());
            return false;
        }
    }

    @PostMapping("/result")
    public ResponseEntity<ApiBody> receiveResult(@RequestBody ObjectNode result) throws JsonProcessingException {
        if (distributorService.getZipName() == null) {
            ApiBody response = new ApiBody("Задачи нет", HttpStatus.OK.value(), null);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        System.out.println(result);
        JsonNode resultNode = result.path("result");

        Task task = mapper.treeToValue(result.path("task"), Task.class);

        taskManager.removeCompetedTask(task);
        distributorJarService.executeProcessResult(resultNode.toString());
        if (taskManager.getFinalEnd().equals(new BigInteger(task.getEnd()))) distributorService.stopGlobalTask();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.println("Время выполнения: " + duration + " миллисекунд");

        ApiBody response = new ApiBody("Результат получен", HttpStatus.OK.value(), null);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
