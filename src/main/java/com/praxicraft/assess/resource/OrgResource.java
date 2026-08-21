package com.praxicraft.assess.resource;

import com.praxicraft.assess.Client;

import java.util.Map;

/** Organisation profile and stats. */
public final class OrgResource {
  private final Client client;

  public OrgResource(Client client) {
    this.client = client;
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> retrieve() {
    return (Map<String, Object>) client.get("/org/", null);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> stats(Map<String, ?> params) {
    return (Map<String, Object>) client.get("/org/stats/", params);
  }
}
