package nl.tudelft.ch.login.federation;

import nl.tudelft.ch.login.dienst2.Dienst2UserAdapter;
import nl.tudelft.ch.login.dienst2.client.PeopleApiClient;
import nl.tudelft.ch.login.dienst2.model.Person;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Keycloak user-storage provider backed by the Dienst2 people API.
 */
public class Dienst2UserProvider implements UserStorageProvider, UserLookupProvider {
    private static final Logger LOGGER = Logger.getLogger(Dienst2UserProvider.class);
    private static final String SURFCONEXT_PREFIX = "surfconext.";
    private static final String GOOGLE_PREFIX = "google.";
    private static final String WISVCH_PREFIX = "wisvch.";

    private final KeycloakSession session;
    private final ComponentModel model;
    private final String componentId;
    private final PeopleApiClient peopleApiClient;
    private final CachedDienst2Lookup lookup;
    private final KeycloakGroupResolver groupResolver;

    Dienst2UserProvider(
            KeycloakSession session,
            ComponentModel model,
            CachedDienst2Lookup lookup,
            KeycloakGroupResolver groupResolver) {
        this.session = session;
        this.model = model;
        this.componentId = model.getId();
        this.lookup = lookup;
        this.groupResolver = groupResolver;

        String baseUrl = model.get(Dienst2UserProviderFactory.BASE_URL);
        String apiKey = model.get(Dienst2UserProviderFactory.API_KEY);
        String endpoint = model.get(Dienst2UserProviderFactory.API_ENDPOINT);
        HttpClientProvider httpClientProvider = session.getProvider(HttpClientProvider.class);
        this.peopleApiClient = new PeopleApiClient(httpClientProvider.getHttpClient(), baseUrl, endpoint, apiKey);
        LOGGER.debugf("Initialized Dienst2UserProvider baseUrl=%s endpoint=%s", baseUrl, endpoint);
    }

    @Override
    public void close() {
        // Keycloak owns the HTTP client's lifecycle.
    }

    /**
     * Resolves the Dienst2 external ID embedded in Keycloak's storage ID.
     */
    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        String externalId = new StorageId(id).getExternalId();
        if (externalId == null) {
            return null;
        }

        try {
            return lookup.findPersonById(componentId, Integer.valueOf(externalId), peopleApiClient)
                    .map(person -> toUserModel(realm, person))
                    .orElse(null);
        } catch (NumberFormatException exception) {
            LOGGER.warnf("Invalid Dienst2 person id '%s'", externalId);
            return null;
        } catch (IOException exception) {
            LOGGER.errorf(exception, "Failed to fetch Dienst2 person by id %s", externalId);
            return null;
        }
    }

    /**
     * Resolves broker usernames in the form surfconext.*, google.*, or WISVCH.*.
     */
    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        if (username == null || username.isBlank()) {
            return null;
        }

        try {
            return findPersonByUsername(username)
                    .map(person -> toUserModel(realm, person))
                    .orElse(null);
        } catch (IOException exception) {
            LOGGER.errorf(exception, "Failed to fetch Dienst2 person for username %s", username);
            return null;
        }
    }

    /**
     * Dienst2 does not support an email lookup, so this provider does not claim one.
     */
    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        return null;
    }

    private Optional<Person> findPersonByUsername(String username) throws IOException {
        if (username.startsWith(SURFCONEXT_PREFIX)) {
            return lookup.findPersonByNetId(componentId, username.substring(SURFCONEXT_PREFIX.length()), peopleApiClient);
        }
        if (username.startsWith(GOOGLE_PREFIX)) {
            return lookup.findPersonByGoogleUsername(componentId, username.substring(GOOGLE_PREFIX.length()), peopleApiClient);
        }
        if (username.regionMatches(true, 0, WISVCH_PREFIX, 0, WISVCH_PREFIX.length())) {
            try {
                Integer personId = Integer.valueOf(username.substring(WISVCH_PREFIX.length()));
                return lookup.findPersonById(componentId, personId, peopleApiClient);
            } catch (NumberFormatException exception) {
                LOGGER.warnf("WISVCH username contains invalid id '%s'", username);
                return Optional.empty();
            }
        }

        LOGGER.debugf("Ignoring unsupported Dienst2 username prefix: %s", username);
        return Optional.empty();
    }

    private UserModel toUserModel(RealmModel realm, Person person) {
        try {
            List<String> groupNames = getGoogleGroups(person);
            return new Dienst2UserAdapter(session, realm, model, person, groupResolver.resolve(realm, groupNames));
        } catch (IOException exception) {
            LOGGER.errorf(exception, "Failed to fetch Google groups for Dienst2 person %s", person.getId());
            return new Dienst2UserAdapter(session, realm, model, person, List.of());
        }
    }

    private List<String> getGoogleGroups(Person person) throws IOException {
        if (person.getId() == null) {
            return List.of();
        }
        if (person.getGoogleUsername() == null || person.getGoogleUsername().isBlank()) {
            return List.of();
        }
        return lookup.getGoogleGroups(componentId, person.getId(), peopleApiClient);
    }
}
