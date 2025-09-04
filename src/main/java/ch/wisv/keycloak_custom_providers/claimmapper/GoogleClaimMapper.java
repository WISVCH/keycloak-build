package ch.wisv.keycloak_custom_providers.claimmapper;

import ch.wisv.keycloak_custom_providers.claimmapper.models.api.Person;
import ch.wisv.keycloak_custom_providers.claimmapper.models.exception.UserNotFoundException;
import ch.wisv.keycloak_custom_providers.claimmapper.services.Dienst2Service;
import ch.wisv.keycloak_custom_providers.claimmapper.services.GoogleAccountService;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.List;

public class GoogleClaimMapper extends ClaimMapper {

    public static final String PROVIDER_ID = "google-claim-mapper";

    private final Dienst2Service dienst2Service;
    private final GoogleAccountService googleAccountService;

    public GoogleClaimMapper() {
        super(PROVIDER_ID);
        dienst2Service = new Dienst2Service();
        googleAccountService = new GoogleAccountService();
    }

    @Override
    public String getDisplayType() {
        return "Google Claim Mapper";
    }


    public void update(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) throws UserNotFoundException {
        String googleEmail = user.getEmail();
        List<String> googleGroups = googleAccountService.retrieveGoogleGroups(googleEmail);

        String googleUsername = googleEmail.split("@")[0];
        Person person = dienst2Service.getDienst2PersonByGoogleUsername(googleUsername, session, mapperModel);

        setUserAttributes(user, person, googleGroups);
    }
}
