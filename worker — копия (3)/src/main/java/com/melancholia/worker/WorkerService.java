package com.melancholia.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;


@Service
public class WorkerService {
    @Autowired
    private RestApiClient restApiClient;
    @Autowired
    private Solver solver;
    @Autowired
    private WorkerHealth workerStateService;
    @Value("${upload-path}")
    private String UPLOAD_PATH;
    @Value("${temp-path}")
    private String TEMP_PATH;
    private static final ObjectMapper mapper = new ObjectMapper();

    public void solve(Task task) {
        try {
            workerStateService.setWorkerStatusEnum(WorkerStatus.WORKING);

            ObjectNode result = (ObjectNode) solver.solve(task);

            ObjectNode finalResult = JsonNodeFactory.instance.objectNode();
            finalResult.set("result", result);
            finalResult.set("task", mapper.valueToTree(task));

            ApiBody response = restApiClient.postRequest(task.getCallbackUrl(), finalResult, ApiBody.class);

            System.out.println("Задача " + task.getStart() + ":" + task.getEnd() +
                    " успешно решена. Ответ: " + response.getMessage());

        } catch (Exception e) {
            System.out.println("Возникла ошибка при выполнении задачи " + task.getStart() + ":" + task.getEnd() +
                    ": " + e.getMessage());
        } finally {
            workerStateService.setWorkerStatusEnum(WorkerStatus.FREE);
        }
    }

    public void reset() {
        workerStateService.setWorkerStatusEnum(WorkerStatus.UNINITIALIZED);
        solver.setZipPath(null);
        solver.setSolveMethod(null);
        try {
            ReflectionUtils.classLoader.close();
            FileUtils.deleteDirectory(new File(UPLOAD_PATH));
            FileUtils.deleteDirectory(new File(TEMP_PATH));
        } catch (IOException e) {
            System.out.println("Возникла ошибка при удалении директории " + e.getMessage());
        }
        System.out.println("Воркер успешно очищен");
    }
}
