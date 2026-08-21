package com.praxicraft.assess.resource;

import com.praxicraft.assess.Client;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/** Assessment results listing and per-invite retrieval. */
public final class ResultsResource {
  private static final int MAX_RESULT_PAGES = 10000;

  private final Client client;

  public ResultsResource(Client client) {
    this.client = client;
  }

  /**
   * List results for an assessment.
   *
   * @param args optional keys: {@code cursor}, {@code page_size}, {@code params}
   */
  @SuppressWarnings("unchecked")
  public Map<String, Object> list(String assessment, Map<String, ?> args) {
    Map<String, Object> query = new LinkedHashMap<>();
    if (args != null) {
      Object nested = args.get("params");
      if (nested instanceof Map<?, ?> nestedMap) {
        query.putAll((Map<String, Object>) nestedMap);
      }
      if (args.containsKey("cursor")) {
        query.put("cursor", args.get("cursor"));
      }
      if (args.containsKey("page_size")) {
        query.put("page_size", args.get("page_size"));
      }
    }
    String key = Client.pathSegment(assessment, "assessment");
    return (Map<String, Object>) client.get("/assessments/" + key + "/results/", query);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> retrieve(String inviteToken) {
    String token = Client.pathSegment(inviteToken, "inviteToken");
    return (Map<String, Object>) client.get("/invites/" + token + "/result/", null);
  }

  /**
   * Iterate all result rows across pages.
   *
   * @param args optional keys: {@code page_size}, {@code params}
   */
  public Iterable<Object> iterAll(String assessment, Map<String, ?> args) {
    Map<String, Object> baseArgs = args == null ? Map.of() : new LinkedHashMap<>(args);
    return () ->
        new Iterator<>() {
          private String cursor = null;
          private final Set<String> seen = new HashSet<>();
          private final List<Object> buffer = new ArrayList<>();
          private int page = 0;
          private boolean done = false;

          @Override
          public boolean hasNext() {
            fill();
            return !buffer.isEmpty();
          }

          @Override
          public Object next() {
            fill();
            if (buffer.isEmpty()) {
              throw new NoSuchElementException();
            }
            return buffer.remove(0);
          }

          @SuppressWarnings("unchecked")
          private void fill() {
            while (buffer.isEmpty() && !done && page < MAX_RESULT_PAGES) {
              Map<String, Object> pageArgs = new LinkedHashMap<>(baseArgs);
              if (cursor != null) {
                pageArgs.put("cursor", cursor);
              }
              Map<String, Object> pageBody = list(assessment, pageArgs);
              page++;
              if (pageBody == null) {
                done = true;
                return;
              }
              Object results = pageBody.get("results");
              if (results instanceof List<?> list) {
                buffer.addAll(list);
              }
              String next = nextCursor(pageBody);
              if (next == null || seen.contains(next)) {
                done = true;
                return;
              }
              seen.add(next);
              cursor = next;
            }
            if (page >= MAX_RESULT_PAGES) {
              done = true;
            }
          }
        };
  }

  private static String nextCursor(Map<String, Object> page) {
    Object nextCursor = page.get("next_cursor");
    if (nextCursor instanceof String s && !s.isEmpty()) {
      return s;
    }
    Object nextLink = page.get("next");
    if (!(nextLink instanceof String link) || link.isEmpty()) {
      return null;
    }
    try {
      URI uri = URI.create(link);
      String query = uri.getRawQuery();
      if (query == null) {
        return null;
      }
      for (String part : query.split("&")) {
        int eq = part.indexOf('=');
        if (eq <= 0) {
          continue;
        }
        String name = URLDecoder.decode(part.substring(0, eq), StandardCharsets.UTF_8);
        if ("cursor".equals(name)) {
          String value = URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8);
          return value.isEmpty() ? null : value;
        }
      }
    } catch (Exception ignored) {
      return null;
    }
    return null;
  }
}
