package com.praxicraft.assess.resource;

import com.praxicraft.assess.Client;
import com.praxicraft.assess.exception.ApiException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Hiring pipelines and enrollments. */
public final class PipelinesResource {
  private final Client client;

  public PipelinesResource(Client client) {
    this.client = client;
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> list(Map<String, ?> params) {
    return (Map<String, Object>) client.get("/pipelines/", params);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> retrieve(String pipeline) {
    String key = Client.pathSegment(pipeline, "pipeline");
    return (Map<String, Object>) client.get("/pipelines/" + key + "/", null);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> enroll(String pipeline, Map<String, ?> args) {
    if (args == null || !(args.get("email") instanceof String email) || email.trim().isEmpty()) {
      throw new ApiException("email is required", "INVALID_ARGUMENT");
    }
    String key = Client.pathSegment(pipeline, "pipeline");
    return (Map<String, Object>) client.post("/pipelines/" + key + "/enroll/", args);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> bulkEnroll(
      String pipeline, List<Map<String, Object>> candidates, Map<String, ?> args) {
    String key = Client.pathSegment(pipeline, "pipeline");
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("candidates", candidates);
    if (args != null) {
      body.putAll(args);
    }
    return (Map<String, Object>) client.post("/pipelines/" + key + "/enroll/bulk/", body);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> listEnrollments(String pipeline, Map<String, ?> params) {
    String key = Client.pathSegment(pipeline, "pipeline");
    return (Map<String, Object>) client.get("/pipelines/" + key + "/enrollments/", params);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> getEnrollment(String enrollmentId) {
    String key = Client.pathSegment(enrollmentId, "enrollmentId");
    return (Map<String, Object>) client.get("/pipelines/enrollments/" + key + "/", null);
  }
}
