package com.praxicraft.assess.resource;

import com.praxicraft.assess.Client;
import com.praxicraft.assess.exception.ApiException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Candidate invites for assessments. */
public final class InvitesResource {
  private final Client client;

  public InvitesResource(Client client) {
    this.client = client;
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> list(Map<String, ?> params) {
    return (Map<String, Object>) client.get("/invites/", params);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> retrieve(String inviteToken) {
    String token = Client.pathSegment(inviteToken, "inviteToken");
    return (Map<String, Object>) client.get("/invites/" + token + "/", null);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> create(String assessment, Map<String, ?> args) {
    if (args == null || !(args.get("email") instanceof String email) || email.trim().isEmpty()) {
      throw new ApiException("email is required", "INVALID_ARGUMENT");
    }
    String key = Client.pathSegment(assessment, "assessment");
    return (Map<String, Object>) client.post("/assessments/" + key + "/invites/", args);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> bulkCreate(
      String assessment, List<Map<String, Object>> candidates, Map<String, ?> args) {
    String key = Client.pathSegment(assessment, "assessment");
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("candidates", candidates);
    if (args != null) {
      body.putAll(args);
    }
    return (Map<String, Object>) client.post("/assessments/" + key + "/invites/bulk/", body);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> remind(String inviteToken) {
    String token = Client.pathSegment(inviteToken, "inviteToken");
    return (Map<String, Object>) client.post("/invites/" + token + "/remind/", null);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> cancel(String inviteToken) {
    String token = Client.pathSegment(inviteToken, "inviteToken");
    return (Map<String, Object>) client.delete("/invites/" + token + "/", null);
  }
}
