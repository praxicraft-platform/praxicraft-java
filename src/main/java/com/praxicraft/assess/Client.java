package com.praxicraft.assess;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.praxicraft.assess.exception.ApiConnectionException;
import com.praxicraft.assess.exception.ApiException;
import com.praxicraft.assess.exception.ApiStatusException;
import com.praxicraft.assess.exception.AuthenticationException;
import com.praxicraft.assess.exception.InsufficientScopeException;
import com.praxicraft.assess.exception.NotFoundException;
import com.praxicraft.assess.exception.RateLimitException;
import com.praxicraft.assess.exception.ValidationException;
import com.praxicraft.assess.resource.AssessmentsResource;
import com.praxicraft.assess.resource.InvitesResource;
import com.praxicraft.assess.resource.OrgResource;
import com.praxicraft.assess.resource.PipelinesResource;
import com.praxicraft.assess.resource.ResultsResource;
import com.praxicraft.assess.resource.WebhooksResource;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Official client for the Praxicraft Assess Public API.
 *
 * <p>Authenticate with a Bearer API key via {@code apiKey} or {@code PRAXICRAFT_API_KEY}.
 */
public final class Client {
  public static final String DEFAULT_BASE_URL = "https://assess.praxicraft.com";
  public static final String DEFAULT_API_PREFIX = "/api/v1/public";
  public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
  public static final int DEFAULT_MAX_RETRIES = 2;

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final TypeReference<Map<String, Object>> MAP_TYPE =
      new TypeReference<Map<String, Object>>() {};

  private final String apiKey;
  private final String baseUrl;
  private final String apiPrefix;
  private final Duration timeout;
  private final int maxRetries;
  private final HttpHandler httpHandler;
  private final HttpClient httpClient;

  private final OrgResource org;
  private final AssessmentsResource assessments;
  private final InvitesResource invites;
  private final ResultsResource results;
  private final WebhooksResource webhooks;
  private final PipelinesResource pipelines;

  /** Build a client reading {@code PRAXICRAFT_API_KEY} (and optional {@code PRAXICRAFT_API_BASE_URL}). */
  public Client() {
    this(new Builder());
  }

  private Client(Builder builder) {
    String key = firstNonBlank(builder.apiKey, System.getenv("PRAXICRAFT_API_KEY"));
    if (key == null || key.isBlank()) {
      throw new ApiException(
          "No API key provided. Pass apiKey or set PRAXICRAFT_API_KEY.", "MISSING_API_KEY");
    }
    String base =
        firstNonBlank(builder.baseUrl, System.getenv("PRAXICRAFT_API_BASE_URL"), DEFAULT_BASE_URL);
    base = stripTrailingSlash(Objects.requireNonNull(base).trim());
    if (base.isEmpty()) {
      throw new ApiException("baseUrl must be a non-empty URL.", "INVALID_BASE_URL");
    }

    this.apiKey = key.trim();
    this.baseUrl = base;
    this.apiPrefix = DEFAULT_API_PREFIX;
    this.timeout = builder.timeout != null ? builder.timeout : DEFAULT_TIMEOUT;
    this.maxRetries = Math.max(0, builder.maxRetries != null ? builder.maxRetries : DEFAULT_MAX_RETRIES);
    this.httpHandler = builder.httpHandler;
    this.httpClient =
        builder.httpClient != null
            ? builder.httpClient
            : HttpClient.newBuilder().connectTimeout(this.timeout).build();

    this.org = new OrgResource(this);
    this.assessments = new AssessmentsResource(this);
    this.invites = new InvitesResource(this);
    this.results = new ResultsResource(this);
    this.webhooks = new WebhooksResource(this);
    this.pipelines = new PipelinesResource(this);
  }

  public static Builder builder() {
    return new Builder();
  }

  public String apiKey() {
    return apiKey;
  }

  public String baseUrl() {
    return baseUrl;
  }

  public String apiPrefix() {
    return apiPrefix;
  }

  public Duration timeout() {
    return timeout;
  }

  public int maxRetries() {
    return maxRetries;
  }

  public OrgResource org() {
    return org;
  }

  public AssessmentsResource assessments() {
    return assessments;
  }

  public InvitesResource invites() {
    return invites;
  }

  public ResultsResource results() {
    return results;
  }

  public WebhooksResource webhooks() {
    return webhooks;
  }

  public PipelinesResource pipelines() {
    return pipelines;
  }

  public Object get(String path, Map<String, ?> params) {
    return request("GET", path, params, null);
  }

  public Object post(String path, Map<String, ?> json) {
    return request("POST", path, null, json);
  }

  public Object put(String path, Map<String, ?> json) {
    return request("PUT", path, null, json);
  }

  public Object patch(String path, Map<String, ?> json) {
    return request("PATCH", path, null, json);
  }

  public Object delete(String path, Map<String, ?> json) {
    return request("DELETE", path, null, json);
  }

  public static String pathSegment(String value, String label) {
    if (value == null || value.trim().isEmpty()) {
      throw new ApiException(label + " must be a non-empty string", "INVALID_PATH");
    }
    return URLEncoder.encode(value.trim(), StandardCharsets.UTF_8).replace("+", "%20");
  }

  public Object request(String method, String path, Map<String, ?> params, Map<String, ?> json) {
    int attempts = maxRetries + 1;
    RuntimeException lastError = null;

    for (int attempt = 0; attempt < attempts; attempt++) {
      if (attempt > 0) {
        String retryAfter = null;
        if (lastError instanceof ApiStatusException statusEx) {
          retryAfter = statusEx.getHeaders().get("retry-after");
        }
        sleep(retryDelayMs(attempt - 1, retryAfter));
      }

      try {
        return requestOnce(method, path, params, json);
      } catch (ApiConnectionException e) {
        lastError = e;
        if (attempt < attempts - 1) {
          continue;
        }
        throw e;
      } catch (ApiStatusException e) {
        lastError = e;
        if (shouldRetryStatus(e.getStatusCode()) && attempt < attempts - 1) {
          continue;
        }
        throw e;
      }
    }

    throw lastError != null ? lastError : new ApiConnectionException();
  }

  private Object requestOnce(String method, String path, Map<String, ?> params, Map<String, ?> json) {
    StringBuilder url = new StringBuilder(baseUrl).append(apiPrefix).append(path);
    if (params != null && !params.isEmpty()) {
      String query = buildQuery(params);
      if (!query.isEmpty()) {
        url.append(url.indexOf("?") >= 0 ? '&' : '?').append(query);
      }
    }

    Map<String, String> headers = new LinkedHashMap<>();
    headers.put("Accept", "application/json");
    headers.put("Authorization", "Bearer " + apiKey);
    headers.put("User-Agent", "praxicraft-java/" + Version.STRING);

    String body = null;
    if (json != null) {
      try {
        body = MAPPER.writeValueAsString(json);
      } catch (IOException e) {
        throw new ApiException("Failed to encode JSON body", "JSON_ENCODE_ERROR", e);
      }
      headers.put("Content-Type", "application/json");
    }

    HttpExchangeResponse response;
    try {
      if (httpHandler != null) {
        response = httpHandler.handle(method, url.toString(), headers, body);
      } else {
        response = defaultHttp(method, url.toString(), headers, body);
      }
    } catch (ApiConnectionException e) {
      throw e;
    } catch (Exception e) {
      throw new ApiConnectionException(e.getMessage() != null ? e.getMessage() : "HTTP request failed", e);
    }

    int status = response.status();
    Map<String, String> respHeaders = response.headers();
    String raw = response.body();

    Object decoded = null;
    if (raw != null && !raw.isEmpty()) {
      try {
        decoded = MAPPER.readValue(raw, Object.class);
      } catch (IOException e) {
        if (status >= 200 && status < 300) {
          throw new ApiException(
              "Invalid JSON response (HTTP " + status + ").", "INVALID_JSON");
        }
        decoded = raw;
      }
    }

    if (status >= 200 && status < 300) {
      return decoded;
    }

    raiseForStatus(status, decoded, respHeaders, raw);
    throw new ApiStatusException("unreachable", status, null, null, decoded, respHeaders, null, null);
  }

  private HttpExchangeResponse defaultHttp(
      String method, String url, Map<String, String> headers, String body) throws Exception {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(timeout);

    for (Map.Entry<String, String> e : headers.entrySet()) {
      builder.header(e.getKey(), e.getValue());
    }

    HttpRequest.BodyPublisher publisher =
        body == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);
    builder.method(method.toUpperCase(), publisher);

    try {
      HttpResponse<String> resp =
          httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      Map<String, String> respHeaders = new LinkedHashMap<>();
      resp.headers()
          .map()
          .forEach(
              (k, values) -> {
                if (k != null && values != null && !values.isEmpty()) {
                  respHeaders.put(k.toLowerCase(), values.get(0));
                }
              });
      return new HttpExchangeResponse(resp.statusCode(), respHeaders, resp.body());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiConnectionException("Request interrupted", e);
    } catch (IOException e) {
      throw new ApiConnectionException(e.getMessage() != null ? e.getMessage() : "HTTP I/O failed", e);
    }
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> asMap(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Map<?, ?> map) {
      return (Map<String, Object>) map;
    }
    return MAPPER.convertValue(value, MAP_TYPE);
  }

  private static String buildQuery(Map<String, ?> params) {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, ?> e : params.entrySet()) {
      Object v = e.getValue();
      if (v == null) {
        continue;
      }
      String value;
      if (v instanceof Boolean b) {
        value = b ? "true" : "false";
      } else if (v instanceof Number || v instanceof String) {
        value = String.valueOf(v);
      } else {
        continue;
      }
      if (sb.length() > 0) {
        sb.append('&');
      }
      sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8));
      sb.append('=');
      sb.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }
    return sb.toString();
  }

  private static final Set<Integer> RETRYABLE_STATUS_CODES =
      Set.of(429, 500, 502, 503, 504);
  private static final long RETRY_BASE_MS = 500L;
  private static final long RETRY_CAP_MS = 8_000L;

  private static boolean shouldRetryStatus(int status) {
    return RETRYABLE_STATUS_CODES.contains(status);
  }

  private static Double parseRetryAfterSeconds(String retryAfter) {
    if (retryAfter == null || retryAfter.isBlank()) {
      return null;
    }
    String text = retryAfter.trim();
    try {
      return Math.max(0.0, Double.parseDouble(text));
    } catch (NumberFormatException ignored) {
      try {
        Instant when =
            ZonedDateTime.parse(text, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        return Math.max(0.0, (when.toEpochMilli() - System.currentTimeMillis()) / 1000.0);
      } catch (Exception ignored2) {
        return null;
      }
    }
  }

  private static long retryDelayMs(int retryIndex, String retryAfter) {
    Double parsed = parseRetryAfterSeconds(retryAfter);
    if (parsed != null) {
      return Math.min((long) (parsed * 1000.0), RETRY_CAP_MS);
    }
    long ceiling = Math.min(RETRY_CAP_MS, RETRY_BASE_MS * (1L << retryIndex));
    if (ceiling <= 0) {
      return 0L;
    }
    return (long) (Math.random() * ceiling);
  }

  private static void sleep(long ms) {
    if (ms <= 0) {
      return;
    }
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiConnectionException("Retry sleep interrupted", e);
    }
  }

  @SuppressWarnings("unchecked")
  private static void raiseForStatus(
      int statusCode, Object body, Map<String, String> headers, String raw) {
    Map<String, Object> error = Map.of();
    if (body instanceof Map<?, ?> bodyMap) {
      Object err = bodyMap.get("error");
      if (err instanceof Map<?, ?> errMap) {
        error = (Map<String, Object>) errMap;
      }
    }

    String code = error.get("code") instanceof String s ? s : null;
    String message = error.get("message") instanceof String s ? s : null;
    Object details = error.get("details");
    String requiredPlan = error.get("required_plan") instanceof String s ? s : null;

    if (message == null || message.isEmpty()) {
      String trimmed = raw == null ? "" : raw.trim();
      message =
          !trimmed.isEmpty()
              ? trimmed.substring(0, Math.min(500, trimmed.length()))
              : "API request failed with status " + statusCode + ".";
    }

    Double retryAfter = parseRetryAfterSeconds(headers != null ? headers.get("retry-after") : null);

    if (statusCode == 401) {
      throw new AuthenticationException(
          message, statusCode, code, details, body, headers, requiredPlan, retryAfter);
    }
    if (statusCode == 403) {
      throw new InsufficientScopeException(
          message, statusCode, code, details, body, headers, requiredPlan, retryAfter);
    }
    if (statusCode == 404) {
      throw new NotFoundException(
          message, statusCode, code, details, body, headers, requiredPlan, retryAfter);
    }
    if (statusCode == 429) {
      throw new RateLimitException(
          message, statusCode, code, details, body, headers, requiredPlan, retryAfter);
    }
    if (statusCode >= 400 && statusCode < 500) {
      throw new ValidationException(
          message, statusCode, code, details, body, headers, requiredPlan, retryAfter);
    }

    throw new ApiStatusException(
        message, statusCode, code, details, body, headers, requiredPlan, retryAfter);
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String v : values) {
      if (v != null && !v.isBlank()) {
        return v;
      }
    }
    return null;
  }

  private static String stripTrailingSlash(String s) {
    int end = s.length();
    while (end > 0 && s.charAt(end - 1) == '/') {
      end--;
    }
    return s.substring(0, end);
  }

  /** Fluent builder for {@link Client}. */
  public static final class Builder {
    private String apiKey;
    private String baseUrl;
    private Duration timeout;
    private Integer maxRetries;
    private HttpHandler httpHandler;
    private HttpClient httpClient;

    public Builder apiKey(String apiKey) {
      this.apiKey = apiKey;
      return this;
    }

    public Builder baseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
      return this;
    }

    public Builder timeout(Duration timeout) {
      this.timeout = timeout;
      return this;
    }

    public Builder maxRetries(int maxRetries) {
      this.maxRetries = maxRetries;
      return this;
    }

    public Builder httpHandler(HttpHandler httpHandler) {
      this.httpHandler = httpHandler;
      return this;
    }

    public Builder httpClient(HttpClient httpClient) {
      this.httpClient = httpClient;
      return this;
    }

    public Client build() {
      return new Client(this);
    }
  }
}
