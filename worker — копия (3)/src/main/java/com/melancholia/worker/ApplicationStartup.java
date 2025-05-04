package com.melancholia.worker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;


@Component
public class ApplicationStartup {

    @Autowired
    private RestApiClient restApiClient;
    @Value("${server.port}")
    private int port;
    private static final String managerAddress = "http://localhost:8000";


    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationStartEvent(ApplicationReadyEvent event) {
        String url = String.format("%s/%s", managerAddress, "worker-register");

        Worker worker;
        try {
            worker = new Worker(InetAddress.getLocalHost().getHostAddress(), port);
        } catch (UnknownHostException e) {
            System.out.println("Не удалось определить локальный IP-адрес: " + e.getMessage());
            throw new RuntimeException("Не удалось разрешить локальный адрес", e);
        }

        System.out.println("Пытаюсь зарегистрироваться в менеджере по адресу: " + url);

        int attempt = 0;
        while (true) {
            try {
                ApiBody response = restApiClient.postRequest(url, worker, ApiBody.class);
                System.out.println("Успешная регистрация в менеджере: " + response.getMessage());
                break;
            } catch (Exception e) {
                attempt++;
                long delay = (long) Math.min(30, Math.pow(2, attempt));
                System.out.println(String.format(
                        "Попытка %d не удалась. Повтор через %d сек. Ошибка: %s",
                        attempt, delay, e.getMessage()));

                try {
                    Thread.sleep(delay * 1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Процесс регистрации прерван", ie);
                }
            }
        }
    }

    @EventListener
    public void onApplicationEndEvent(ContextClosedEvent event) {
        String url = String.format("%s/%s", managerAddress, "worker-leave");
        System.out.println("Пытаюсь отрегистрироваться в менеджере по адресу: " + url);

        Worker worker;
        try {
            worker = new Worker(InetAddress.getLocalHost().getHostAddress(), port);
        } catch (UnknownHostException e) {
            System.out.println("Не удалось определить локальный IP-адрес: " + e.getMessage());
            throw new RuntimeException("Не удалось разрешить локальный адрес", e);
        }

        try {
            ApiBody response = restApiClient.postRequest(url, worker, ApiBody.class);
            System.out.println("Успешная отмена регистрации: " + response.getMessage());
        } catch (Exception e) {
            System.out.println("Не удалось отрегистрироваться: " + e.getMessage());
        }

        System.out.println("Процесс отмены регистрации завершен");
    }

}
