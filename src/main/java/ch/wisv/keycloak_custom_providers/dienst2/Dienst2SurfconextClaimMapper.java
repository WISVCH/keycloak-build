package ch.wisv.keycloak_custom_providers.dienst2;

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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Kijk naar UserAttributeMapper voor goed voorbeeld
//UserAttributeMapper
public class Dienst2SurfconextClaimMapper extends AbstractClaimMapper {

    public static final String PROVIDER_ID = "dienst2-surfconext-claim-mapper";

    public static final String[] COMPATIBLE_PROVIDERS = {KeycloakOIDCIdentityProviderFactory.PROVIDER_ID, OIDCIdentityProviderFactory.PROVIDER_ID};
    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES = new HashSet<>(Arrays.asList(IdentityProviderSyncMode.values()));

    private static final Logger logger = Logger.getLogger(Dienst2SurfconextClaimMapper.class);

    private final List<ProviderConfigProperty> configMetadata;

    public Dienst2SurfconextClaimMapper() {
        configMetadata = ProviderConfigurationBuilder.create()
                .property()
                .name("dienst2Url")
                .label("Dienst2 URL")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("https://dienst2.ch.tudelft.nl")
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
            CloseableHttpResponse resp = httpClient.execute(req);
            String result = new BufferedReader(new InputStreamReader(resp.getEntity().getContent())).lines().collect(Collectors.joining("\n"));
            logger.info("updateBrokeredUser returned " + result);
        } catch (IOException e) {
            logger.error("updateBrokeredUser ging fout: " + e);
            throw new RuntimeException(e);
        }
    }
}
