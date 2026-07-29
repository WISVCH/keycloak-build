package nl.tudelft.ch.login.federation;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import nl.tudelft.ch.login.dienst2.model.Person;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.utils.StringUtil;

import java.time.Duration;
import java.util.List;

public class Dienst2UserProviderFactory implements UserStorageProviderFactory<Dienst2UserProvider> {

    public static final String PROVIDER_ID = "Dienst2";
    private static final Duration DIENST2_CACHE_LIFESPAN = Duration.ofSeconds(60);
    private static final int PERSON_CACHE_MAXIMUM_SIZE = 10_000;
    private static final int GROUP_CACHE_MAXIMUM_SIZE = 10_000;
    static final String BASE_URL = "baseUrl";
    static final String API_KEY = "apiKey";
    static final String API_ENDPOINT = "apiEndpoint";
    private final List<ProviderConfigProperty> configMetadata;
    private final Cache<String, Person> personCache = Caffeine.newBuilder()
            .maximumSize(PERSON_CACHE_MAXIMUM_SIZE)
            .expireAfterWrite(DIENST2_CACHE_LIFESPAN)
            .build();
    private final Cache<String, List<String>> groupCache = Caffeine.newBuilder()
            .maximumSize(GROUP_CACHE_MAXIMUM_SIZE)
            .expireAfterWrite(DIENST2_CACHE_LIFESPAN)
            .build();

    public Dienst2UserProviderFactory() {
        configMetadata = ProviderConfigurationBuilder.create()
                .property(BASE_URL,
                        "Base URL",
                        "Dienst2 base url",
                        ProviderConfigProperty.STRING_TYPE,
                        "http://dienst2.wisvch.internal",
                        null)
                .property(API_KEY,
                        "API Key",
                        "Dienst2 API Key",
                        ProviderConfigProperty.PASSWORD,
                        "",
                        null)
                .property(API_ENDPOINT,
                        "Endpoint",
                        "Members database end point",
                        ProviderConfigProperty.STRING_TYPE,
                        "/ldb/api/v3",
                        null)
                .build();
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
        if (StringUtil.isBlank(config.get(BASE_URL)) ||
                StringUtil.isBlank(config.get(API_KEY)) ||
                StringUtil.isBlank(config.get(API_ENDPOINT))) {
            throw new ComponentValidationException("Missing configuration");
        }
    }

    @Override
    public Dienst2UserProvider create(KeycloakSession keycloakSession, ComponentModel componentModel) {
        return new Dienst2UserProvider(keycloakSession, componentModel, personCache, groupCache);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "WISV CH Dienst2 User Federation Provider";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configMetadata;
    }
}
