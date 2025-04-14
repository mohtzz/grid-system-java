package com.melancholia.distributor.utils;

import com.melancholia.distributor.distributor.DistributorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Service
public class HttpSenderService {

    @Autowired
    private RestTemplate restTemplate;

    public <T> T sendPostRequest(String url, Object data, Class<T> responseType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> requestEntity = new HttpEntity<>(data, headers);

            ResponseEntity<T> response = restTemplate.postForEntity(url, requestEntity, responseType);
            return response.getBody();
        } catch (RestClientException e) {
            System.out.println("Ошибка при отправке POST-запроса на " +  url + e.getMessage());
            throw e;
        }
    }

    public <T> T sendGetRequest(String url, Class<T> responseType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<T> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    responseType
            );
            return response.getBody();
        } catch (RestClientException e) {
            System.out.println("Ошибка при отправке GET-запроса на " + url + e.getMessage());
            throw e;
        }
    }

    public <T> T sendMultipartPostRequest(String url, MultiValueMap<String, Object> multipartBody, Class<T> responseType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(multipartBody, headers);

            ResponseEntity<T> response = restTemplate.postForEntity(url, requestEntity, responseType);
            return response.getBody();
        } catch (RestClientException e) {
            System.out.println("Ошибка при отправке multipart POST-запроса на " +  url + e.getMessage());
            throw e;
        }
    }
}
