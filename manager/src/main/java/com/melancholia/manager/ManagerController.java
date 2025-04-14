package com.melancholia.manager;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class ManagerController {

    @Autowired
    private ManagerService managerService;

    @PostMapping("/worker-register")
    public ResponseEntity<ApiBody> workerRegister(@RequestBody Worker request) {
        managerService.registerWorker(request);
        ApiBody response = new ApiBody("Воркер успешно зарегистрирован", HttpStatus.OK.value(), null);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/worker-leave")
    public ResponseEntity<ApiBody> workerLeave(@RequestBody Worker request) {
        managerService.leaveWorker(request);
        ApiBody response = new ApiBody("Воркер успешно удален", HttpStatus.OK.value(), null);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/get-workers")
    public ResponseEntity<ApiBody> getWorkers() {
        ApiBody response = new ApiBody("Активные воркеры", HttpStatus.OK.value(), managerService.getWorkers());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
