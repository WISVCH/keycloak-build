package ch.wisv.keycloak_custom_providers.dienst2;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;

import java.util.List;

public class Dienst2UserStorageProviderFactory implements UserStorageProviderFactory<Dienst2UserStorageProvider> {

    public static final String PROVIDER_ID = "dienst2-user-federation-provider";

    private final List<ProviderConfigProperty> configMetadata;

    public Dienst2UserStorageProviderFactory() {
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
    public List<ProviderConfigProperty> getConfigProperties() {
        return configMetadata;
    }

    @Override
    public Dienst2UserStorageProvider create(KeycloakSession keycloakSession, ComponentModel componentModel) {
        return new Dienst2UserStorageProvider(keycloakSession, componentModel);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
