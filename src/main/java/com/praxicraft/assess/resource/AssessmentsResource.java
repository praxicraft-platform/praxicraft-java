package com.praxicraft.assess.resource;

import com.praxicraft.assess.Client;
import com.praxicraft.assess.exception.ApiException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Assessment CRUD and task attachment. */
public final class AssessmentsResource {
  private final Client client;

  public AssessmentsResource(Client client) {
    this.client = client;
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> list(Map<String, ?> params) {
    return (Map<String, Object>) client.get("/assessments/", params);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> retrieve(String assessment) {
    String key = Client.pathSegment(assessment, "assessment");
    return (Map<String, Object>) client.get("/assessments/" + key + "/", null);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> create(Map<String, ?> fields) {
    return (Map<String, Object>) client.post("/assessments/create/", fields);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> update(String assessment, Map<String, ?> fields) {
    if (fields == null || fields.isEmpty()) {
      throw new ApiException("update() requires at least one field to change", "INVALID_ARGUMENT");
    }
    String key = Client.pathSegment(assessment, "assessment");
    return (Map<String, Object>) client.patch("/assessments/" + key + "/update/", fields);
  }

  public Map<String, Object> activate(String assessment) {
    return update(assessment, Map.of("status", "active"));
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> listTasks(String assessment, Map<String, ?> params) {
    String key = Client.pathSegment(assessment, "assessment");
    return (Map<String, Object>) client.get("/assessments/" + key + "/tasks/", params);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> attachTasks(String assessment, Map<String, ?> args) {
    if (args == null || args.isEmpty()) {
      throw new ApiException("attachTasks() requires tasks or task_id", "INVALID_ARGUMENT");
    }
    String key = Client.pathSegment(assessment, "assessment");
    return (Map<String, Object>) client.post("/assessments/" + key + "/tasks/attach/", args);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> replaceTasks(
      String assessment, List<Map<String, Object>> tasks, Map<String, ?> extra) {
    String key = Client.pathSegment(assessment, "assessment");
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("tasks", tasks);
    if (extra != null) {
      body.putAll(extra);
    }
    return (Map<String, Object>) client.put("/assessments/" + key + "/tasks/replace/", body);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> removeTask(String assessment, String assessmentTaskId) {
    String key = Client.pathSegment(assessment, "assessment");
    if (assessmentTaskId == null || assessmentTaskId.trim().isEmpty()) {
      throw new ApiException("assessmentTaskId must be a non-empty string", "INVALID_ARGUMENT");
    }
    return (Map<String, Object>)
        client.delete(
            "/assessments/" + key + "/tasks/remove/",
            Map.of("assessment_task_id", assessmentTaskId.trim()));
  }
}
