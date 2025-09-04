package ch.wisv.keycloak_custom_providers.claimmapper;

import ch.wisv.keycloak_custom_providers.claimmapper.models.api.Dienst2Person;
import ch.wisv.keycloak_custom_providers.claimmapper.models.exception.UserNotFoundException;
import ch.wisv.keycloak_custom_providers.claimmapper.services.Dienst2Service;
import ch.wisv.keycloak_custom_providers.claimmapper.services.GoogleAccountService;
import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.mappers.AbstractClaimMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Kijk naar UserAttributeMapper voor goed voorbeeld
//UserAttributeMapper
public class SurfconextClaimMapper extends AbstractClaimMapper {

    public static final String PROVIDER_ID = "surfconext-claim-mapper";

    public static final String[] COMPATIBLE_PROVIDERS = {KeycloakOIDCIdentityProviderFactory.PROVIDER_ID, OIDCIdentityProviderFactory.PROVIDER_ID};
    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES = new HashSet<>(Arrays.asList(IdentityProviderSyncMode.values()));

    private static final Logger logger = Logger.getLogger(SurfconextClaimMapper.class);

    private final List<ProviderConfigProperty> configMetadata;

    private final Dienst2Service dienst2Service;
    private final GoogleAccountService googleAccountService;

    public SurfconextClaimMapper() {
        dienst2Service = new Dienst2Service();
        googleAccountService = new GoogleAccountService();

        configMetadata = ProviderConfigurationBuilder.create()
                .property()
                .name("dienst2Url")
                .label("Dienst2 URL")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("http://dienst2.wisvch.internal")
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
                .defaultValue("/ldb/api/v3/people")
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
        return "Surfconext Claim Mapper";
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
        logger.info("Updating surf brokered user: " + user.getId() + " user email: " + user.getEmail() + " context email: " + context.getEmail());
        user.getAttributes().forEach((key, value) -> {
            logger.info("attribute: " + key + " : " + value.toString());
        });
        String netid = "jgort";
        try {
            Dienst2Person person = dienst2Service.getDienst2PersonByNetId(netid, session, mapperModel);
            user.setFirstName(person.getFirstname());
            user.setLastName(person.getSurname());
            user.setSingleAttribute("google_username", person.getGoogle_username());
            user.setSingleAttribute("netid", person.getNetid());
            user.setSingleAttribute("membership_status", String.valueOf(person.getMembership_status()));

            String googleEmail = person.getGoogle_username() + "@ch.tudelft.nl";
            List<String> googleGroups = googleAccountService.retrieveGoogleGroups(googleEmail);
            user.setAttribute("google_groups", googleGroups);
        } catch (
                UserNotFoundException e) {
            logger.warn("Could not find user with netid: " + netid);
            user.setEnabled(false);
        }
    }

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        logger.info("Importing surf brokered user: " + user.getId() + " user email: " + user.getEmail() + " context email: " + context.getEmail());
        user.getAttributes().forEach((key, value) -> {
            logger.info("attribute: " + key + " : " + value.toString());
        });
        String netid = "jgort";
        try {
            Dienst2Person person = dienst2Service.getDienst2PersonByNetId(netid, session, mapperModel);
            user.setFirstName(person.getFirstname());
            user.setLastName(person.getSurname());
            user.setSingleAttribute("google_username", person.getGoogle_username());
            user.setSingleAttribute("netid", person.getNetid());
            user.setSingleAttribute("membership_status", String.valueOf(person.getMembership_status()));

            String googleEmail = person.getGoogle_username() + "@ch.tudelft.nl";
            List<String> googleGroups = googleAccountService.retrieveGoogleGroups(googleEmail);
            user.setAttribute("google_groups", googleGroups);
        } catch (UserNotFoundException e) {
            logger.warn("Could not find user with netid: " + netid);
            user.setEnabled(false);
        }
    }
}
