package com.example.vk_echo_server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.mockito.Mockito.*;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
class VkEchoServerApplicationTests {
    @Autowired
    private LongPollingService vkLongPollingService;

    @MockBean
    private RestTemplate restTemplate;
    @Test
    void testHandleMultipleEvents() {
        Map<String, Object> mockResponseBody = new HashMap<>();
        mockResponseBody.put("server", "test");
        mockResponseBody.put("key", "test");
        mockResponseBody.put("ts", 12345);

        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("response", mockResponseBody);

        when(restTemplate.getForObject(contains("messages.getLongPollServer"), eq(Map.class)))
                .thenReturn(mockResponse);

        List<Object> update = Arrays.asList(4, 123, 17, 123, 0, 0, "Привет");
        List<Object> updateOutgoing = Arrays.asList(4, 124, 3, 123, 0, 0, "Привет2");
        List<Object> updateEditMessage = Arrays.asList(80, 789);


        Map<String, Object> longPollResponse = new HashMap<>();
        longPollResponse.put("ts", 12346);
        longPollResponse.put("updates", List.of(update, updateOutgoing, updateEditMessage));

        when(restTemplate.getForObject(contains("a_check"), eq(Map.class)))
                .thenReturn(longPollResponse).thenThrow(new RuntimeException());

        when(restTemplate.postForObject(contains("messages.send"), any(Map.class), eq(String.class))).thenReturn("");

        try {
            vkLongPollingService.startPolling();
        } catch (RuntimeException ignored){

        }
        finally {
            verify(restTemplate, times(1)).postForObject(
                    contains("messages.send"),
                    argThat(params ->
                            params instanceof Map &&
                                    checkSendObjectField(params, "123", "user_id") &&
                                    checkSendObjectField(params, "Вы сказали: Привет", "message")
                    ),
                    eq(String.class)
            );

            verify(restTemplate, times(1)).postForObject(anyString(), any(Map.class), eq(String.class));
        }
    }

    private boolean checkSendObjectField(Object sendObj, String expected, String field){
        try {
            List<String> list = (List<String>) ((Map<?, ?>) sendObj).get(field);
            return  list.get(0).equals(expected);
        } catch (Throwable t){
            return false;
        }
    }
}
