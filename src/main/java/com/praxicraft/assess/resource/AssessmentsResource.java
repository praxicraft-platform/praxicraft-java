package com.praxicraft.assess.resource;

import com.praxicraft.assess.Client;
import com.praxicraft.assess.exception.ApiException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Assessment CRUD and case attachment. */
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
  public Map<String, Object> listCases(String assessment, Map<String, ?> params) {
    String key = Client.pathSegment(assessment, "assessment");
    return (Map<String, Object>) client.get("/assessments/" + key + "/cases/", params);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> attachCases(String assessment, Map<String, ?> args) {
    if (args == null || args.isEmpty()) {
      throw new ApiException("attachCases() requires cases or case_id", "INVALID_ARGUMENT");
    }
    String key = Client.pathSegment(assessment, "assessment");
    return (Map<String, Object>) client.post("/assessments/" + key + "/cases/attach/", args);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> replaceCases(
      String assessment, List<Map<String, Object>> cases, Map<String, ?> extra) {
    String key = Client.pathSegment(assessment, "assessment");
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("cases", cases);
    if (extra != null) {
      body.putAll(extra);
    }
    return (Map<String, Object>) client.put("/assessments/" + key + "/cases/replace/", body);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> removeCase(String assessment, String assessmentCaseId) {
    String key = Client.pathSegment(assessment, "assessment");
    if (assessmentCaseId == null || assessmentCaseId.trim().isEmpty()) {
      throw new ApiException("assessmentCaseId must be a non-empty string", "INVALID_ARGUMENT");
    }
    return (Map<String, Object>)
        client.delete(
            "/assessments/" + key + "/cases/remove/",
            Map.of("assessment_case_id", assessmentCaseId.trim()));
  }
}
