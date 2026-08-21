package com.praxicraft.assess.resource;

import com.praxicraft.assess.Client;
import com.praxicraft.assess.exception.ApiException;

import java.util.List;
import java.util.Map;

/** Outbound webhook endpoint management. */
public final class WebhooksResource {
  private final Client client;

  public WebhooksResource(Client client) {
    this.client = client;
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> list(Map<String, ?> params) {
    return (Map<String, Object>) client.get("/webhooks/", params);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> create(Map<String, ?> args) {
    if (args == null || !(args.get("url") instanceof String url) || url.trim().isEmpty()) {
      throw new ApiException("url is required", "INVALID_ARGUMENT");
    }
    Object events = args.get("events");
    if (!(events instanceof List<?> list) || list.isEmpty()) {
      throw new ApiException("events must be a non-empty list", "INVALID_ARGUMENT");
    }
    return (Map<String, Object>) client.post("/webhooks/create/", args);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> retrieve(String webhookId) {
    String key = Client.pathSegment(webhookId, "webhookId");
    return (Map<String, Object>) client.get("/webhooks/" + key + "/", null);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> update(String webhookId, Map<String, ?> fields) {
    if (fields == null || fields.isEmpty()) {
      throw new ApiException("update() requires at least one field to change", "INVALID_ARGUMENT");
    }
    String key = Client.pathSegment(webhookId, "webhookId");
    return (Map<String, Object>) client.patch("/webhooks/" + key + "/", fields);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> delete(String webhookId) {
    String key = Client.pathSegment(webhookId, "webhookId");
    return (Map<String, Object>) client.delete("/webhooks/" + key + "/", null);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> deliveries(String webhookId) {
    String key = Client.pathSegment(webhookId, "webhookId");
    return (Map<String, Object>) client.get("/webhooks/" + key + "/deliveries/", null);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> test(String webhookId) {
    String key = Client.pathSegment(webhookId, "webhookId");
    return (Map<String, Object>) client.post("/webhooks/" + key + "/test/", null);
  }
}
