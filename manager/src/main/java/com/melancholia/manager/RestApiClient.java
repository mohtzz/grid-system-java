package com.melancholia.manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Service
public class RestApiClient {

    @Autowired
    private RestTemplate restTemplate;

    public <T> T postRequest(String url, Object requestData, Class<T> responseType) {
        try {
            System.out.println("Отправка POST запроса: " + url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> request = new HttpEntity<>(requestData, headers);

            ResponseEntity<T> response = restTemplate.postForEntity(url, request, responseType);
            System.out.println("POST запрос успешно доставлен: " + url);

            return response.getBody();
        } catch (Exception e) {
            System.out.println("POST запрос провалился " + url + ". Ошибка: " + e.getMessage());
            throw e;
        }
    }

    public <T> T getRequest(String url, Class<T> responseType) {
        try {
            System.out.println("Отправка GET запроса: " + url);

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<?> request = new HttpEntity<>(headers);

            ResponseEntity<T> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    responseType
            );
            System.out.println("GET запрос успешно доставлен: " + url);

            return response.getBody();
        } catch (Exception e) {
            System.out.println("GET запрос провалился " + url + ". Ошибка: " + e.getMessage());
            throw e;
        }
    }
}
