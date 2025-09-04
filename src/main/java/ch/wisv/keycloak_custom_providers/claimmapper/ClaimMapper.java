package ch.wisv.keycloak_custom_providers.claimmapper;

import ch.wisv.keycloak_custom_providers.claimmapper.models.api.Person;
import ch.wisv.keycloak_custom_providers.claimmapper.models.api.Student;
import ch.wisv.keycloak_custom_providers.claimmapper.models.exception.UserNotFoundException;
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
public abstract class ClaimMapper extends AbstractClaimMapper {

    private static final Logger logger = Logger.getLogger(SurfconextClaimMapper.class);

    public static final String[] COMPATIBLE_PROVIDERS = {KeycloakOIDCIdentityProviderFactory.PROVIDER_ID, OIDCIdentityProviderFactory.PROVIDER_ID};
    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES = new HashSet<>(Arrays.asList(IdentityProviderSyncMode.values()));

    private final List<ProviderConfigProperty> configMetadata;

    private final String providerId;

    public ClaimMapper(String providerId) {
        this.providerId = providerId;
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

    public abstract void update(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) throws UserNotFoundException;

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        logger.info("Updating for " + providerId + ", user user: " + user.getId() + " user email: " + user.getEmail() + " context email: " + context.getEmail());
        user.getAttributes().forEach((key, value) -> {
            logger.info("attribute: " + key + " : " + value.toString());
        });
        try {
            update(session, realm, user, mapperModel, context);
        } catch (UserNotFoundException e) {
            user.setSingleAttribute("not_found", "true");
        }
    }

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        logger.info("Importing for " + providerId + ", user: " + user.getId() + " user email: " + user.getEmail() + " context email: " + context.getEmail());
        user.getAttributes().forEach((key, value) -> {
            logger.info("attribute: " + key + " : " + value.toString());
        });
        try {
            update(session, realm, user, mapperModel, context);
        } catch (UserNotFoundException e) {
            user.setSingleAttribute("not_found", "true");
        }
    }

    protected static void setUserAttributes(UserModel user, Person person, List<String> googleGroups) {
        user.setFirstName(person.getFirstname());
        user.setLastName(person.getSurnameWithPreposition());
        user.setSingleAttribute("membership_status", String.valueOf(person.getMembershipStatus()));

        //Claims to set got from https://github.com/WISVCH/connect/blob/master/src/main/java/ch/wisv/connect/services/CHScopeClaimTranslationService.java
        user.setSingleAttribute("sub", String.valueOf(person.getId()));

        user.setSingleAttribute("name", person.getFormattedName());
        user.setSingleAttribute("preferred_username", !person.getGoogleUsername().isBlank() ? person.getGoogleUsername() : person.getNetid());
        user.setSingleAttribute("given_name", person.getFirstname());
        user.setSingleAttribute("family_name", person.getSurnameWithPreposition());
//        user.setSingleAttribute("middle_name", person.);
//        user.setSingleAttribute("nickname", person.);
//        user.setSingleAttribute("profile", person.);
//        user.setSingleAttribute("picture", person.);
//        user.setSingleAttribute("website", person.);

        //Not very woke
        switch (person.getGender()) {
            case "M":
                user.setSingleAttribute("gender", "male");
                break;
            case "F":
                user.setSingleAttribute("gender", "female");
                break;
        }
//        user.setSingleAttribute("zone_info", person.);
//        user.setSingleAttribute("locale", person.);
//        user.setSingleAttribute("updated_at", person.);

//        user.setSingleAttribute("birthdate", person.getBirthdate().toString()); //TODO see Person for disabling info

        user.setSingleAttribute("email", person.getEmail());
//        user.setSingleAttribute("email_verified", person.);

        user.setSingleAttribute("phone_number", person.getPhoneMobile());
//        user.setSingleAttribute("phone_number_verified", person.);

        user.setSingleAttribute("address.street_address", person.getStreetAddress());
        user.setSingleAttribute("address.postal_code", person.getPostcode());
        user.setSingleAttribute("address.locality", person.getCity());
        user.setSingleAttribute("address.country", person.getCountry());
        user.setSingleAttribute("address.formatted", person.getFormattedAddress());

        user.setSingleAttribute("google_username", person.getGoogleUsername());
        user.setAttribute("google_groups", googleGroups);
        user.setSingleAttribute("netid", person.getNetid());

        if (person.getStudent().map(Student::isEnrolled).orElse(false)) {
            user.setSingleAttribute("student_number", person.getStudent().map(Student::getStudentNumber).orElse(null));
            user.setSingleAttribute("study", person.getStudent().map(Student::getStudy).orElse(null));
        }
    }

    @Override
    public String getId() {
        return providerId;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
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
    public List<ProviderConfigProperty> getConfigMetadata() {
        return configMetadata;
    }

    @Override
    public String getDisplayCategory() {
        return "Claim mapper";
    }


    @Override
    public String getHelpText() {
        return "If all claims exists, grant the user the specified realm or client role.";
    }

}
