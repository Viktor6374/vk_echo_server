package com.example.vk_echo_server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Сервис для обработки событий лонг поллинга от VK API. Реагирует только на входящие сообщения. В ответ цитирует отправителя
 */
@Service
public class LongPollingService {
    /**
     * Базовый URL для запросов к VK API.
     */
    @Value("${vk.api.url}")
    private String VK_API_URL;
    /**
     * Токен доступа для взаимодействия с VK API.
     */
    @Value("${vk.api.token}")
    private String ACCESS_TOKEN;
    /**
     * Версия VK API, которая будет использоваться для всех запросов.
     */
    @Value("${vk.api.version}")
    private String API_VERSION;
    /**
     * Идентификатор группы в которой находится бот.
     */
    @Value("${vk.api.group.id}")
    private String GROUP_ID;

    /**
     * Адрес сервера, получаемый от VK API.
     */
    private String server;
    /**
     * Ключ для подключения к Long Poll серверу, получаемый от VK API.
     */
    private String key;
    /**
     * Текущий номер события, для синхронизации с Long Poll сервером.
     */
    private Integer ts;

    /**
     * Объект для выполнения HTTP-запросов к VK API.
     */
    @Autowired
    private RestTemplate restTemplate;

    /**
     * Инициализирует процесс лонг-поллинга при старте сервиса.
     */
    public void startPolling() {
        getLongPollServer();
        while (true) {
            pollForUpdates();
        }
    }


    /**
     * Получает информацию о Long Poll сервере VK.
     * Отправляет запрос к VK API для получения необходимых данных для работы с Long Poll сервером,
     * включая адрес сервера, ключ и текущий номер события.
     */
    private void getLongPollServer() {
        String url = VK_API_URL + "messages.getLongPollServer";
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("access_token", ACCESS_TOKEN)
                .queryParam("group_id", GROUP_ID)
                .queryParam("v", API_VERSION);

        String finalUrl = uriBuilder.toUriString();

        Map<String, Object> response = restTemplate.getForObject(finalUrl, Map.class);
        Map<String, Object> responseBody = (Map<String, Object>) response.get("response");

        server = (String) responseBody.get("server");
        key = (String) responseBody.get("key");
        ts = (Integer) responseBody.get("ts");
    }

    /**
     * Выполняет опрос Long Poll сервера VK для получения новых событий.
     * Этот метод регулярно опрашивает сервер и передает обработанные события методу handleUpdate().
     */
    private void pollForUpdates() {
        String url = String.format("https://%s?act=a_check&key=%s&ts=%d&wait=25", server, key, ts);

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        List<ArrayList<Object>> updates = (List<ArrayList<Object>>) response.get("updates");

        if (updates != null && !updates.isEmpty()) {
            for (List<Object> update : updates) {
                handleUpdate(update);
            }
        }

        ts = (Integer) response.get("ts");
    }

    /**
     * Обрабатывает каждое полученное событие Long Poll.
     * В данном примере обрабатываются только события представляющие собой новое входящее сообщение.
     *
     * @param update Объект, содержащий информацию о событии
     */
    private void handleUpdate(List<Object> update) {
        Integer updateType = (Integer) update.get(0);

        if (updateType == 4) {
            boolean isOutgoing = (((Integer) update.get(2)) & 2) == 0; // Проверка, что сообщение именно входящее, а не исходящее
            if (isOutgoing) {
                Integer userId = (Integer) update.get(3);
                String messageText = (String) update.get(6);

                System.out.println("Получено сообщение: " + messageText);

                String responseMessage = "Вы сказали: " + messageText;
                sendMessage(userId, responseMessage);
            }
        }
    }

    /**
     * Отправляет сообщение пользователю через VK API.
     *
     * @param userId Идентификатор пользователя, которому будет отправлено сообщение
     * @param message Текст сообщения
     */
    private void sendMessage(Integer userId, String message) {
        String url = VK_API_URL + "messages.send";
        MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
        params.add("user_id", userId.toString());
        params.add("random_id", "0");
        params.add("message", message);
        params.add("access_token", ACCESS_TOKEN);
        params.add("v", API_VERSION);



        restTemplate.postForObject(url, params, String.class);
    }
}
