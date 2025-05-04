package com.melancholia.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@RestController
public class WorkerController {

    @Autowired
    private WorkerService workerService;
    @Autowired
    private Solver solverService;
    @Autowired WorkerHealth workerStateService;
    @Value("${upload-path}")
    private String UPLOAD_PATH;

    public static final ObjectMapper mapper = new ObjectMapper();

    @PostMapping("/init")
    public ResponseEntity<ApiBody> init(@RequestPart("jarFile") MultipartFile jarFile,
                                        @RequestPart("archiveFile") MultipartFile archiveFile,
                                        @RequestPart("jsonData") MultipartFile jsonData){

        Path uploadDir = Paths.get(UPLOAD_PATH);

        try {
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            Path jarFilePath = uploadDir.resolve(jarFile.getOriginalFilename());
            Files.copy(jarFile.getInputStream(), jarFilePath, StandardCopyOption.REPLACE_EXISTING);

            Path archiveFilePath = uploadDir.resolve(archiveFile.getOriginalFilename());
            Files.copy(archiveFile.getInputStream(), archiveFilePath, StandardCopyOption.REPLACE_EXISTING);

            Path jsonFilePath = uploadDir.resolve(jsonData.getOriginalFilename());
            Files.copy(jsonData.getInputStream(), jsonFilePath, StandardCopyOption.REPLACE_EXISTING);

            File jsonFile = jsonFilePath.toFile();
            Manifest manifest = mapper.readValue(jsonFile, Manifest.class);

            solverService.setSolveMethod(ReflectionUtils.getAnnotatedMethodsByName(
                    jarFilePath,
                    manifest.getClassName(),
                    manifest.getAnnotationName()).get(0));
            solverService.setZipPath(archiveFilePath);
            workerStateService.setWorkerStatusEnum(WorkerStatus.FREE);

            ApiBody response = new ApiBody("Инициализирован", HttpStatus.OK.value(), null);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
            ApiBody response = new ApiBody("Ошибка", HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/solve")
    public ResponseEntity<ApiBody> solve(@RequestBody Task task) {
        System.out.println("Задача получена: " + task.getStart() + ":" + task.getEnd());
        System.out.println(task.getStart() + " " + task.getEnd());
        workerService.solve(task);

        ApiBody response = new ApiBody("Задача получена", HttpStatus.OK.value(), null);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/check-state")
    public ResponseEntity<ApiBody> checkWorkerState() {
        WorkerStatus workerStatus = workerStateService.getWorkerStatusEnum();

        ApiBody response = new ApiBody("Статус воркера", HttpStatus.OK.value(), workerStatus);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/reset")
    public ResponseEntity<ApiBody> reset() {
        workerService.reset();

        ApiBody response = new ApiBody("Обновлено успешно", HttpStatus.OK.value(), null);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
