package nl.tudelft.ch.login.dienst2.client;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.ws.rs.core.UriBuilder;
import nl.tudelft.ch.login.dienst2.model.PeopleResponse;
import nl.tudelft.ch.login.dienst2.model.Person;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;

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

    public Optional<Person> getPersonById(Integer id) throws IOException {
        if (id == null) {
            return Optional.empty();
        }

        URI personUri = apiRoot.resolve("people/" + id + "/");
        HttpGet request = new HttpGet(personUri);
        addDienst2AuthorizationHeader(request);

        traceRequest("GET", personUri);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            traceResponse(personUri, statusCode);
            if (statusCode == 404) {
                LOGGER.tracef("Dienst2 person not found id=%d", id);
                EntityUtils.consumeQuietly(response.getEntity());
                return Optional.empty();
            }
            if (statusCode < 200 || statusCode >= 300) {
                throw unexpectedResponse(statusCode, personUri);
            }

            if (response.getEntity() == null) {
                LOGGER.warnf("Dienst2 returned an empty person response id=%d", id);
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
        addDienst2AuthorizationHeader(request);

        traceRequest("GET", uri);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            traceResponse(uri, statusCode);
            if (statusCode == 404) {
                LOGGER.tracef("Dienst2 Google groups not found personId=%d", personId);
                EntityUtils.consumeQuietly(response.getEntity());
                return Collections.emptyList();
            }
            if (statusCode < 200 || statusCode >= 300) {
                throw unexpectedResponse(statusCode, uri);
            }

            if (response.getEntity() == null) {
                LOGGER.warnf("Dienst2 returned an empty Google groups response personId=%d", personId);
                return Collections.emptyList();
            }

            try (InputStream content = response.getEntity().getContent()) {
                List<String> groups = objectMapper.readValue(content, new TypeReference<List<String>>() {
                });
                if (groups == null || groups.isEmpty()) {
                    return Collections.emptyList();
                }
                LOGGER.tracef("Dienst2 returned Google groups personId=%s count=%s", personId.toString(), Integer.toString(groups.size()));
                return Collections.unmodifiableList(groups);
            }
        }
    }

    private Optional<Person> findSingleByFilter(String filterName, String filterValue) throws IOException {
        if (filterValue == null || filterValue.isBlank()) {
            return Optional.empty();
        }

        URI listUri = buildPeopleSearchUri(filterName, filterValue, SINGLE_RESULT_LIMIT, null);
        PeopleResponse response = executePeopleRequest(listUri);
        if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
            LOGGER.tracef("Dienst2 person not found filter=%s value=%s", filterName, filterValue);
            return Optional.empty();
        }
        List<Person> results = response.getResults();
        if (results.size() > 1) {
            LOGGER.warnf("Dienst2 returned multiple people filter=%s value=%s", filterName, filterValue);
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

    private void addDienst2AuthorizationHeader(HttpUriRequest request) {
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        request.setHeader(HttpHeaders.AUTHORIZATION, "Token " + apiKey);
    }

    private PeopleResponse executePeopleRequest(URI uri) throws IOException {
        HttpGet request = new HttpGet(uri);
        addDienst2AuthorizationHeader(request);

        traceRequest("GET", uri);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            traceResponse(uri, statusCode);
            if (statusCode < 200 || statusCode >= 300) {
                throw unexpectedResponse(statusCode, uri);
            }

            if (response.getEntity() == null) {
                LOGGER.warnf("Dienst2 returned an empty people response path=%s", uri.getPath());
                return null;
            }

            try (InputStream content = response.getEntity().getContent()) {
                PeopleResponse peopleResponse = objectMapper.readValue(content, PeopleResponse.class);
                if (peopleResponse != null && LOGGER.isTraceEnabled()) {
                    if (peopleResponse.getResults() != null && !peopleResponse.getResults().isEmpty()) {
                        peopleResponse.getResults().stream()
                                .filter(Objects::nonNull)
                                .forEach(person -> tracePerson("list", person));
                    }
                }
                return peopleResponse;
            }
        }
    }

    private IOException unexpectedResponse(int statusCode, URI uri) {
        return new IOException("Unexpected Dienst2 response status=" + statusCode + " path=" + uri.getPath());
    }

    private void traceRequest(String method, URI uri) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.tracef("Dienst2 request method=%s uri=%s", method, uri);
        }
    }

    private void traceResponse(URI uri, int statusCode) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.tracef("Dienst2 response status=%d uri=%s", statusCode, uri);
        }
    }

    private void tracePerson(String context, Person person) {
        if (!LOGGER.isTraceEnabled() || person == null) {
            return;
        }
        LOGGER.tracef("Dienst2 person response context=%s id=%s", context, person.getId());
    }
}
