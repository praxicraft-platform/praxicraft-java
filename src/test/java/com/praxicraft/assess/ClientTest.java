package com.praxicraft.assess;

import com.praxicraft.assess.exception.AuthenticationException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientTest {
  @Test
  void orgRetrieveUsesMockHandler() {
    List<Object[]> calls = new ArrayList<>();
    Client client =
        Client.builder()
            .apiKey("ct_test_x")
            .httpHandler(
                (method, url, headers, body) -> {
                  calls.add(new Object[] {method, url, headers, body});
                  return new HttpExchangeResponse(
                      200,
                      Map.of("content-type", "application/json"),
                      "{\"name\":\"Acme\",\"plan\":\"starter\"}");
                })
            .build();

    Map<String, Object> org = client.org().retrieve();
    assertEquals("Acme", org.get("name"));
    assertEquals("GET", calls.get(0)[0]);
    assertTrue(((String) calls.get(0)[1]).contains("/api/v1/public/org/"));
    @SuppressWarnings("unchecked")
    Map<String, String> headers = (Map<String, String>) calls.get(0)[2];
    assertEquals("Bearer ct_test_x", headers.get("Authorization"));
    assertTrue(headers.get("User-Agent").startsWith("praxicraft-java/"));
  }

  @Test
  void mapsAuthenticationError() {
    Client client =
        Client.builder()
            .apiKey("ct_test_x")
            .maxRetries(0)
            .httpHandler(
                (method, url, headers, body) ->
                    new HttpExchangeResponse(
                        401,
                        Map.of(),
                        "{\"error\":{\"code\":\"INVALID_API_KEY\",\"message\":\"bad\"}}"))
            .build();

    assertThrows(AuthenticationException.class, () -> client.org().retrieve());
  }

  @Test
  void assessmentsCreatePostsJson() {
    AtomicReference<String> capturedBody = new AtomicReference<>();
    AtomicReference<String> capturedUrl = new AtomicReference<>();
    Client client =
        Client.builder()
            .apiKey("ct_test_x")
            .httpHandler(
                (method, url, headers, body) -> {
                  capturedUrl.set(url);
                  capturedBody.set(body);
                  assertEquals("POST", method);
                  return new HttpExchangeResponse(200, Map.of(), "{\"slug\":\"demo\"}");
                })
            .build();

    Map<String, Object> created = client.assessments().create(Map.of("title", "Demo"));
    assertEquals("demo", created.get("slug"));
    assertTrue(capturedUrl.get().contains("/api/v1/public/assessments/create/"));
    assertTrue(capturedBody.get().contains("Demo"));
  }

  @Test
  void assessmentTaskPathsAndBodyKeys() {
    List<Object[]> calls = new ArrayList<>();
    Client client =
        Client.builder()
            .apiKey("ct_test_x")
            .httpHandler(
                (method, url, headers, body) -> {
                  calls.add(new Object[] {method, url, body});
                  if (url.contains("/tasks/attach/")) {
                    return new HttpExchangeResponse(200, Map.of(), "{\"attached\":1}");
                  }
                  if (url.contains("/tasks/remove/")) {
                    return new HttpExchangeResponse(204, Map.of(), "");
                  }
                  return new HttpExchangeResponse(
                      200, Map.of(), "{\"results\":[{\"id\":\"row-1\"}]}");
                })
            .build();

    client
        .assessments()
        .attachTasks(
            "demo",
            Map.of(
                "tasks",
                List.of(Map.of("task_id", "task-1", "source", "platform"))));
    client.assessments().listTasks("demo", null);
    client.assessments().removeTask("demo", "row-1");

    assertEquals("POST", calls.get(0)[0]);
    assertTrue(((String) calls.get(0)[1]).contains("/assessments/demo/tasks/attach/"));
    assertTrue(((String) calls.get(0)[2]).contains("task_id"));

    assertEquals("GET", calls.get(1)[0]);
    assertTrue(((String) calls.get(1)[1]).contains("/assessments/demo/tasks/"));

    assertEquals("DELETE", calls.get(2)[0]);
    assertTrue(((String) calls.get(2)[1]).contains("/assessments/demo/tasks/remove/"));
    assertTrue(((String) calls.get(2)[2]).contains("assessment_task_id"));
  }
}
