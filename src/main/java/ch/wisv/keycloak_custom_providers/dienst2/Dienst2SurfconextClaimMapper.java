package ch.wisv.keycloak_custom_providers.dienst2;

import ch.wisv.keycloak_custom_providers.dienst2.models.api.Dienst2PeopleResponse;
import ch.wisv.keycloak_custom_providers.dienst2.models.api.Dienst2Person;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.apache.v2.GoogleApacheHttpTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.services.CommonGoogleClientRequestInitializer;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.cloudidentity.v1.CloudIdentity;
import com.google.api.services.cloudidentity.v1.CloudIdentityRequest;
import com.google.api.services.cloudidentity.v1.CloudIdentityRequestInitializer;
import com.google.api.services.cloudidentity.v1.CloudIdentityScopes;
import com.google.api.services.cloudidentity.v1.model.SearchTransitiveGroupsResponse;
import com.google.auth.oauth2.GoogleCredentials;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.mappers.AbstractClaimMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;

// Kijk naar UserAttributeMapper voor goed voorbeeld
//UserAttributeMapper
public class Dienst2SurfconextClaimMapper extends AbstractClaimMapper {

    public static final String PROVIDER_ID = "dienst2-surfconext-claim-mapper";

    public static final String[] COMPATIBLE_PROVIDERS = {KeycloakOIDCIdentityProviderFactory.PROVIDER_ID, OIDCIdentityProviderFactory.PROVIDER_ID};
    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES = new HashSet<>(Arrays.asList(IdentityProviderSyncMode.values()));

    private static final Logger logger = Logger.getLogger(Dienst2SurfconextClaimMapper.class);

    private final List<ProviderConfigProperty> configMetadata;

    private final ObjectMapper mapper;
    private final JsonFactory jsonFactory;

    public Dienst2SurfconextClaimMapper() {

        this.mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                .configure(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION, true);
        this.jsonFactory = mapper.getFactory();
        configMetadata = ProviderConfigurationBuilder.create()
                .property()
                .name("dienst2Url")
                .label("Dienst2 URL")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("https://dienst2.wisvch.internal")
//                .helpText("Rest schema to call external services")
                .add()
                .property()
                .name("Dienst2ApiKey")
                .label("Dienst2 API Key")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("")
//                .helpText("Hostname of the external service")
                .add()
                .property()
                .name("dienst2Endpoint")
                .label("Dienst2 endpoint")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("/ldb/api/v3/people?google_username=joshuag")
//                .helpText("Port of the external service")
                .add()
                .build();
    }

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return IDENTITY_PROVIDER_SYNC_MODES.contains(syncMode);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configMetadata;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory() {
        return "Claim mapper";
    }

    @Override
    public String getDisplayType() {
        return "Dienst2 Surf Mapper";
    }

    @Override
    public String getHelpText() {
        return "If all claims exists, grant the user the specified realm or client role.";
    }


    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return configMetadata;
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        logger.info("updateBrokeredUser");
        CloseableHttpClient httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();
        String url = mapperModel.getConfig().get("dienst2Url");
        String apiKey = mapperModel.getConfig().get("Dienst2ApiKey");
        String endpoint = mapperModel.getConfig().get("dienst2Endpoint");
        HttpUriRequest req = new HttpGet(url + endpoint);
        req.setHeader(HttpHeaders.AUTHORIZATION, "Token " + apiKey);
        logger.info("Request: " + req.getURI().toString());
        try {
            GoogleCredentials creds = GoogleCredentials.getApplicationDefault();
            String credential = creds.getAccessToken().getTokenValue();

            logger.info("api cred: " + credential + ", type: " + creds.getAuthenticationType());
            HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();

            CloudIdentityRequestInitializer initializer = new CloudIdentityRequestInitializer(credential);
            com.google.api.client.json.JsonFactory jsonFactory1 = new GsonFactory();
            CloudIdentity cloudIdentity = new CloudIdentity.Builder(transport, jsonFactory1, transport.createRequestFactory().getInitializer()).setCloudIdentityRequestInitializer(initializer).build();

            String email = "joshuag@ch.tudelft.nl";
            String customerId = "C03nrg5fp";
            String parent = "groups/-";
            SearchTransitiveGroupsResponse response =  cloudIdentity.groups().memberships()
                    .searchTransitiveGroups(parent)
                    .set("query", "member_key_id == '" + email + "' && 'cloudidentity.googleapis.com/groups.discussion_forum' in labels && parent == 'customers/" + customerId +  "'")
                    .execute();
            logger.info(response.toString());

            response.getMemberships().forEach(member -> {logger.info(member.getGroup());});

            CloseableHttpResponse resp = httpClient.execute(req);
            Dienst2PeopleResponse result = mapper.readValue(resp.getEntity().getContent(), Dienst2PeopleResponse.class );
            if (result != null && result.results != null) {
                logger.info("updateBrokeredUser json object result returned: " + result.results.size() + " results");
                if(result.results.size() == 1) {
                    Dienst2Person person = result.results.getFirst();
                    user.setFirstName(person.getFirstname());
                    user.setLastName(person.getSurname());
                    user.setSingleAttribute("google_username", person.getGoogle_username());
                    user.setSingleAttribute("netid", person.getNetid());
                    user.setSingleAttribute("membership_status", String.valueOf(person.getMembership_status()));
                }

            }

        } catch (IOException e) {
            logger.error("updateBrokeredUser ging fout: " + e);
            throw new RuntimeException(e);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }


    }
}
