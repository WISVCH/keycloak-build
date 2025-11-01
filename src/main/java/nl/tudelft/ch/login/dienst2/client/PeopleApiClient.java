package nl.tudelft.ch.login.dienst2.client;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import nl.tudelft.ch.login.dienst2.model.PeopleResponse;
import nl.tudelft.ch.login.dienst2.model.Person;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;

import jakarta.ws.rs.core.UriBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class PeopleApiClient {

    private static final Logger LOGGER = Logger.getLogger(PeopleApiClient.class);
    private static final int SINGLE_RESULT_LIMIT = 2;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI apiRoot;
    private final String apiKey;

    public PeopleApiClient(CloseableHttpClient httpClient, String baseUrl, String endpoint, String apiKey) {
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                .configure(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION, true);
        this.apiRoot = buildApiRoot(Objects.requireNonNull(baseUrl, "baseUrl"), Objects.requireNonNull(endpoint, "endpoint"));
        this.apiKey = apiKey;
    }

    public Optional<Person> getPersonById(Integer id) throws IOException {
        if (id == null) {
            return Optional.empty();
        }

        URI personUri = apiRoot.resolve("people/" + id + "/");
        HttpGet request = new HttpGet(personUri);
        addDefaultHeaders(request);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            traceRequest("GET", personUri, null);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 404) {
                LOGGER.debugf("People API returned 404 for id %d", id);
                EntityUtils.consumeQuietly(response.getEntity());
                return Optional.empty();
            }
            if (statusCode < 200 || statusCode >= 300) {
                String body = readBodyQuietly(response);
                throw new IOException("Unexpected response " + statusCode + " from " + personUri + ": " + body);
            }

            if (response.getEntity() == null) {
                return Optional.empty();
            }

            try (InputStream content = response.getEntity().getContent()) {
                Person person = objectMapper.readValue(content, Person.class);
                tracePerson("getPersonById", person);
                return Optional.ofNullable(person);
            }
        }
    }

    public Optional<Person> findByGoogleUsername(String googleUsername) throws IOException {
        return findSingleByFilter("google_username", googleUsername);
    }

    public Optional<Person> findByNetId(String netId) throws IOException {
        return findSingleByFilter("netid", netId);
    }

    public Optional<Person> findByLdapUsername(String ldapUsername) throws IOException {
        return findSingleByFilter("ldap_username", ldapUsername);
    }

    public List<String> getGoogleGroups(Integer personId) throws IOException {
        if (personId == null) {
            return Collections.emptyList();
        }

        URI uri = apiRoot.resolve("people/" + personId + "/google_groups/");
        HttpGet request = new HttpGet(uri);
        addDefaultHeaders(request);
        traceRequest("GET", uri, null);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 404) {
                LOGGER.debugf("People API returned 404 for google_groups of id %d", personId);
                EntityUtils.consumeQuietly(response.getEntity());
                return Collections.emptyList();
            }
            if (statusCode < 200 || statusCode >= 300) {
                String body = readBodyQuietly(response);
                throw new IOException("Unexpected response " + statusCode + " from " + uri + ": " + body);
            }

            if (response.getEntity() == null) {
                return Collections.emptyList();
            }

            try (InputStream content = response.getEntity().getContent()) {
                List<String> groups = objectMapper.readValue(content, new TypeReference<List<String>>() {});
                if (groups == null || groups.isEmpty()) {
                    LOGGER.debugf("People API returned zero google groups for id %d", personId);
                    return Collections.emptyList();
                }
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.tracef("Dienst2 google groups for %d -> %s", personId, groups);
                }
                return Collections.unmodifiableList(groups);
            }
        }
    }

    private Optional<Person> findSingleByFilter(String filterName, String filterValue) throws IOException {
        if (filterValue == null || filterValue.isBlank()) {
            return Optional.empty();
        }

        URI listUri = buildPeopleSearchUri(filterName, filterValue, SINGLE_RESULT_LIMIT, null);
        traceRequest("GET", listUri, Map.of(filterName, filterValue));
        PeopleResponse response = executePeopleRequest(listUri);
        if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
            LOGGER.debugf("People API returned no results for filter %s=%s", filterName, filterValue);
            return Optional.empty();
        }
        List<Person> results = response.getResults();
        if (results.size() > 1) {
            LOGGER.warnf("Multiple persons returned for filter %s=%s", filterName, filterValue);
            return Optional.empty();
        }
        Person single = results.getFirst();
        tracePerson("findSingleByFilter", single);
        return Optional.ofNullable(single);
    }

    private URI buildPeopleSearchUri(String filterName, String filterValue, Integer limit, Integer offset) {
        UriBuilder builder = UriBuilder.fromUri(apiRoot.resolve("people/"));
        builder.queryParam(filterName, filterValue);
        if (limit != null) {
            builder.queryParam("limit", limit);
        }
        if (offset != null) {
            builder.queryParam("offset", offset);
        }
        return builder.build();
    }

    private void addDefaultHeaders(HttpUriRequest request) {
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        request.setHeader(HttpHeaders.AUTHORIZATION, "Token " + apiKey);
    }

    private static URI buildApiRoot(String baseUrl, String endpoint) {
        String normalizedBase = trimTrailingSlash(baseUrl);
        String normalizedEndpoint = trimLeadingSlash(endpoint);
        if (normalizedBase.isEmpty()) {
            throw new IllegalArgumentException("baseUrl must not be empty");
        }
        if (normalizedEndpoint.isEmpty()) {
            throw new IllegalArgumentException("endpoint must not be empty");
        }
        if (!normalizedEndpoint.endsWith("/")) {
            normalizedEndpoint = normalizedEndpoint + "/";
        }
        return URI.create(normalizedBase + "/" + normalizedEndpoint);
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        int length = value.length();
        int index = length;
        while (index > 0 && value.charAt(index - 1) == '/') {
            index--;
        }
        return value.substring(0, index);
    }

    private static String trimLeadingSlash(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        int length = value.length();
        int index = 0;
        while (index < length && value.charAt(index) == '/') {
            index++;
        }
        return value.substring(index);
    }

    private String readBodyQuietly(CloseableHttpResponse response) {
        try {
            if (response.getEntity() == null) {
                return "";
            }
            return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.debug("Failed to read response body", e);
            return "";
        }
    }

    private PeopleResponse executePeopleRequest(URI uri) throws IOException {
        HttpGet request = new HttpGet(uri);
        addDefaultHeaders(request);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode < 200 || statusCode >= 300) {
                String body = readBodyQuietly(response);
                throw new IOException("Unexpected response " + statusCode + " from " + uri + ": " + body);
            }

            if (response.getEntity() == null) {
                LOGGER.debugf("People API returned empty entity for %s", uri);
                return null;
            }

            try (InputStream content = response.getEntity().getContent()) {
                PeopleResponse peopleResponse = objectMapper.readValue(content, PeopleResponse.class);
                if (peopleResponse != null) {
                    if (peopleResponse.getResults() == null || peopleResponse.getResults().isEmpty()) {
                        LOGGER.debugf("People API returned zero people for %s", uri);
                    } else if (LOGGER.isTraceEnabled()) {
                        peopleResponse.getResults().stream()
                                .filter(Objects::nonNull)
                                .forEach(person -> tracePerson("list", person));
                    }
                }
                return peopleResponse;
            }
        }
    }

    private void traceRequest(String method, URI uri, Map<String, String> filters) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.tracef("Dienst2 request %s %s filters=%s", method, uri, filters == null ? "{}" : filters);
        }
    }

    private void tracePerson(String context, Person person) {
        if (!LOGGER.isTraceEnabled() || person == null) {
            return;
        }
        String first = person.getFirstname() == null ? "" : person.getFirstname();
        String last = person.getSurname() == null ? "" : person.getSurname();
        LOGGER.tracef("Dienst2 %s -> id=%s name=%s %s", context, person.getId(), first, last);
    }
}
