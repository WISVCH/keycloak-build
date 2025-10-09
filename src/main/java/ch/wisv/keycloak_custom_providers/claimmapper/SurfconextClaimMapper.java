package ch.wisv.keycloak_custom_providers.claimmapper;

import ch.wisv.keycloak_custom_providers.claimmapper.models.api.Person;
import ch.wisv.keycloak_custom_providers.claimmapper.models.exception.UserNotFoundException;
import ch.wisv.keycloak_custom_providers.claimmapper.services.Dienst2Service;
import ch.wisv.keycloak_custom_providers.claimmapper.services.GoogleAccountService;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Collections;
import java.util.List;

public class SurfconextClaimMapper extends ClaimMapper {

    public static final String PROVIDER_ID = "surfconext-claim-mapper";

    private static final Logger logger = Logger.getLogger(SurfconextClaimMapper.class);


    private final Dienst2Service dienst2Service;
    private final GoogleAccountService googleAccountService;

    public SurfconextClaimMapper() {
        super(PROVIDER_ID);
        dienst2Service = new Dienst2Service();
        googleAccountService = new GoogleAccountService();
    }

    @Override
    public String getDisplayType() {
        return "Surfconext Claim Mapper";
    }

    @Override
    public void update(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) throws UserNotFoundException {
        List<String> netidAttribute = user.getAttributes().get("netid");
        if (netidAttribute == null) {
            throw new UserNotFoundException();
        }
        String netid = netidAttribute.getFirst();
        if (netid == null) {
           throw new UserNotFoundException();
        }

        Person person = dienst2Service.getDienst2PersonByNetId(netid, session, mapperModel);

        UserModel personByUsername = session.users().getUserByUsername(realm, SUBJECT_PREFIX + person.getId());
        if (personByUsername != null) {
            context.setId(personByUsername.getId());
        }

        List<String> googleGroups;
        if(person.getGoogleUsername() != null) {
            String googleEmail = person.getGoogleUsername() + "@ch.tudelft.nl";
            googleGroups = googleAccountService.retrieveGoogleGroups(googleEmail);
        } else {
            googleGroups = Collections.emptyList();
        }

        setUserAttributes(user, person, googleGroups);
    }
}
