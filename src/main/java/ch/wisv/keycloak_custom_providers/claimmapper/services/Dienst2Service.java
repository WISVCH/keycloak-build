package ch.wisv.keycloak_custom_providers.claimmapper.services;

import ch.wisv.keycloak_custom_providers.claimmapper.models.api.Dienst2PeopleResponse;
import ch.wisv.keycloak_custom_providers.claimmapper.models.api.Person;
import ch.wisv.keycloak_custom_providers.claimmapper.models.exception.UserNotFoundException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.logging.Logger;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;

import java.io.IOException;

public class Dienst2Service {


    private final Logger logger = Logger.getLogger(Dienst2Service.class);
    private final ObjectMapper mapper;

    public Dienst2Service() {
        this.mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                .configure(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION, true);
    }

    public Person getDienst2PersonByNetId(String netId, KeycloakSession session, IdentityProviderMapperModel mapperModel) throws UserNotFoundException {
        try {
            logger.info("Retrieving dienst2 person by netId: " + netId);
            CloseableHttpClient httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();

            UriBuilder uriBuilder = setupDienstUri(mapperModel);
            uriBuilder.queryParam("netid", netId);

            Dienst2PeopleResponse result = executePeopleRequest(mapperModel, httpClient, uriBuilder);
            if (result != null && result.results != null) {
                logger.info("updateBrokeredUser json object result returned: " + result.results.size() + " results");
                if (result.results.size() == 1) {
                    return result.results.getFirst();
                } else if (result.results.size() > 1) {
                    logger.error("meer dan 1 persoon, dat is niet best");
                    throw new UserNotFoundException();
                } else {
                    logger.error("No persons found in Dienst2 for netid: " + netId);
                    throw new UserNotFoundException();
                }
            }
            logger.error("Invalid result received from Dienst2 for netid: " + netId);
            throw new UserNotFoundException();
        } catch (IOException e) {
            logger.error("Could not get dienst2 person for netid: " + netId, e);
            throw new UserNotFoundException();
        }
    }

    public Person getDienst2PersonByGoogleUsername(String googleUsername, KeycloakSession session, IdentityProviderMapperModel mapperModel) throws UserNotFoundException {
        try {
            logger.info("Retrieving dienst2 person by google username: " + googleUsername);
            CloseableHttpClient httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();

            UriBuilder uriBuilder = setupDienstUri(mapperModel);
            uriBuilder.queryParam("google_username", googleUsername);
            Dienst2PeopleResponse result = executePeopleRequest(mapperModel, httpClient, uriBuilder);
            if (result != null && result.results != null) {
                logger.info("updateBrokeredUser json object result returned: " + result.results.size() + " results");
                if (result.results.size() == 1) {
                    return result.results.getFirst();
                } else if (result.results.size() > 1) {
                    logger.error("meer dan 1 persoon, dat is niet best");
                    throw new UserNotFoundException();
                } else {
                    logger.error("No persons found in Dienst2 for google_username: " + googleUsername);
                    throw new UserNotFoundException();
                }
            }
            logger.error("Invalid result received from Dienst2 for google_username: " + googleUsername);
            throw new UserNotFoundException();
        } catch (IOException e) {
            logger.error("Could not get dienst2 person for google_username: " + googleUsername, e);
            throw new UserNotFoundException();
        }
    }

    private Dienst2PeopleResponse executePeopleRequest(IdentityProviderMapperModel mapperModel, CloseableHttpClient httpClient, UriBuilder uriBuilder) throws IOException {
        HttpUriRequest req = new HttpGet(uriBuilder.build());

        String apiKey = mapperModel.getConfig().get("Dienst2ApiKey");
        req.setHeader(HttpHeaders.AUTHORIZATION, "Token " + apiKey);

        CloseableHttpResponse resp = httpClient.execute(req);
        return mapper.readValue(resp.getEntity().getContent(), Dienst2PeopleResponse.class);
    }

    private UriBuilder setupDienstUri(IdentityProviderMapperModel mapperModel) {
        String url = mapperModel.getConfig().get("dienst2Url");
        String endpoint = mapperModel.getConfig().get("dienst2Endpoint");
        UriBuilder uriBuilder = UriBuilder.fromUri(url + endpoint);
        return uriBuilder;
    }
}
