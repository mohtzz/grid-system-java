package com.melancholia.distributor.distributor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melancholia.distributor.core.ApiBody;
import com.melancholia.distributor.dto.Task;
import com.melancholia.distributor.dto.Worker;
import com.melancholia.distributor.enums.WorkerStatusEnum;
import com.melancholia.distributor.utils.HttpSenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.List;

@Service
public class DistributorService {

    @Autowired
    private HttpSenderService httpSenderService;
    @Autowired
    private TaskManager taskManager;
    @Value("${worker-init-files.jar-file-path}")
    private String JAR_FILE_PATH;
    @Value("${worker-init-files.manifest-path}")
    private String MANIFEST_PATH;
    @Value("${manager.get-workers-endpoint}")
    private String MANAGER_GET_WORKERS_ENDPOINT;
    @Value("${upload-path}")
    private String UPLOAD_PATH;
    private String zipName = null;

    private static final long PROCESS_INTERVAL_MS = 10_000;
    private static final ObjectMapper mapper = new ObjectMapper();

    public String getZipName() {
        return zipName;
    }

    public void startTask(String zipName) {
        this.zipName = zipName;
    }

    @Scheduled(fixedRate = PROCESS_INTERVAL_MS)
    public void processTask() {
        if (zipName == null) return;

        try {
            System.out.println("Запуск обработки задач...");

            ApiBody workersResponse = httpSenderService.sendGetRequest(MANAGER_GET_WORKERS_ENDPOINT, ApiBody.class);
            List<Worker> workers = parseWorkers(workersResponse.getData());

            System.out.println("Получено воркеров: " + workers.size());

            for (Worker worker : workers) {
                try {

                    if (worker.getWorkerStatus().equals(WorkerStatusEnum.UNINITIALIZED)) {
                        System.out.println("Инициализация воркера: " + worker.fullAddress());
                        initWorker(worker, zipName);
                    } else if (worker.getWorkerStatus().equals(WorkerStatusEnum.FREE)) {
                        System.out.println("Отправка задачи воркеру: " + worker.fullAddress());
                        sendTask(worker);
                    }
                } catch (Exception e) {
                    System.out.println("Проблема с воркером " + worker.fullAddress() + ": " + e.getMessage());
                }
            }

            taskManager.taskRedistribution(workers);
            System.out.println("Завершена обработка всех воркеров");

        } catch (Exception e) {
            System.out.println("Ошибка в процессе обработки: " + e.getMessage());
        }
    }

    private List<Worker> parseWorkers(Object data) {
        try {
            if (data == null) {
                return Collections.emptyList();
            }
            String jsonString = mapper.writeValueAsString(data);
            return mapper.readValue(jsonString, new TypeReference<List<Worker>>() {});
        } catch (Exception e) {
            System.out.println("Не удалось разобрать список воркеров: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private void initWorker(Worker worker, String zipName) {
        try {
            MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
            multipartBody.add("jarFile", new FileSystemResource(JAR_FILE_PATH));
            multipartBody.add("jsonData", new FileSystemResource(MANIFEST_PATH));
            multipartBody.add("archiveFile", new FileSystemResource(UPLOAD_PATH + "/" + zipName));

            ApiBody response = httpSenderService.sendMultipartPostRequest(
                    worker.initAddress(),
                    multipartBody,
                    ApiBody.class
            );

            System.out.println("Воркер " + worker.fullAddress() + " успешно инициализирован: " + response.getMessage());

        } catch (Exception e) {
            System.out.println("Ошибка инициализации воркера " + worker.fullAddress() + ": " + e.getMessage());
            throw new RuntimeException("Ошибка инициализации воркера", e);
        }
    }

    private void sendTask(Worker worker) {
        try {
            Task task = taskManager.getTask();
            if (task == null) {
                System.out.println("Нет задач для отправки воркеру " + worker.fullAddress());
                return;
            }

            task.setWorker(worker.fullAddress());
            ApiBody response = httpSenderService.sendPostRequest(
                    worker.solveAddress(),
                    task,
                    ApiBody.class
            );

            System.out.println("Задача успешно отправлена воркеру " + worker.fullAddress() + ": " + response.getMessage());

        } catch (Exception e) {
            System.out.println("Не удалось отправить задачу воркеру " + worker.fullAddress() + ": " + e.getMessage());
            throw new RuntimeException("Ошибка отправки задачи", e);
        }
    }

    public void resetAllWorkers() {
        try {
            System.out.println("Начало сброса всех воркеров...");

            ApiBody workersResponse = httpSenderService.sendGetRequest(MANAGER_GET_WORKERS_ENDPOINT, ApiBody.class);
            List<Worker> workers = parseWorkers(workersResponse.getData());

            for (Worker worker : workers) {
                try {
                    httpSenderService.sendGetRequest(worker.resetAddress(), Void.class);
                    System.out.println("Воркер " + worker.fullAddress() + " успешно сброшен");
                } catch (Exception e) {
                    System.out.println("Не удалось сбросить воркер " + worker.fullAddress() + ": " + e.getMessage());
                }
            }

            System.out.println("Все воркеры успешно сброшены");

        } catch (Exception e) {
            System.out.println("При сбросе воркеров: " + e.getMessage());
            throw new RuntimeException("Ошибка сброса воркеров", e);
        }
    }

    public void stopGlobalTask() {
        try {
            zipName = null;
            taskManager.reset();
            resetAllWorkers();

            System.out.println("Глобальная задача успешно остановлена!");

        } catch (Exception e) {
            System.out.println("Ошибка при остановке глобальной задачи: " + e.getMessage());
        }
    }
}
