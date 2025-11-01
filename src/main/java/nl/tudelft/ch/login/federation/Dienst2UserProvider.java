package nl.tudelft.ch.login.federation;

import nl.tudelft.ch.login.dienst2.Dienst2UserAdapter;
import nl.tudelft.ch.login.dienst2.client.PeopleApiClient;
import nl.tudelft.ch.login.dienst2.model.Person;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Dienst2UserProvider implements UserStorageProvider, UserLookupProvider {
    private static final Logger LOGGER = Logger.getLogger(Dienst2UserProvider.class);

    private final KeycloakSession session;
    private final ComponentModel model;
    private final PeopleApiClient peopleApiClient;

    public Dienst2UserProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.model = model;

        final String baseUrl = model.get(Dienst2UserProviderFactory.BASE_URL);
        final String apiKey = model.get(Dienst2UserProviderFactory.API_KEY);
        final String endpoint = model.get(Dienst2UserProviderFactory.API_ENDPOINT);

        HttpClientProvider httpClientProvider = session.getProvider(HttpClientProvider.class);
        this.peopleApiClient = new PeopleApiClient(httpClientProvider.getHttpClient(), baseUrl, endpoint, apiKey);
        LOGGER.debugf("Initialized Dienst2UserProvider baseUrl=%s endpoint=%s", baseUrl, endpoint);
    }

    @Override
    public void close() {
        LOGGER.trace("Dienst2UserProvider.close()");
        // nothing to close; Keycloak manages the HTTP client lifecycle
    }

    @Override
    public UserModel getUserById(RealmModel realmModel, String id) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.tracef("getUserById realm=%s id=%s", realmModel.getName(), id);
        }
        StorageId storageId = new StorageId(id);
        String externalId = storageId.getExternalId();
        if (externalId == null) {
            LOGGER.trace("getUserById ignored because externalId is null");
            return null;
        }

        try {
            Optional<Person> person = peopleApiClient.getPersonById(Integer.valueOf(externalId));
            LOGGER.tracef("getUserById result present=%s", person.isPresent());
            return person.map(value -> toUserModel(realmModel, value)).orElse(null);
        } catch (NumberFormatException e) {
            LOGGER.warnf("Invalid Dienst2 person id '%s'", externalId);
            return null;
        } catch (IOException e) {
            LOGGER.errorf(e, "Failed to fetch Dienst2 person by id %s", externalId);
            return null;
        }
    }

    @Override
    public UserModel getUserByUsername(RealmModel realmModel, String username) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.tracef("getUserByUsername realm=%s username=%s", realmModel.getName(), username);
        }

        if (username == null || username.isBlank()) {
            LOGGER.trace("getUserByUsername ignored because username is blank");
            return null;
        }

        try {
            Optional<Person> person;
            if (username.startsWith("surfconext.")) {
                String netid = username.substring("surfconext.".length());
                person = peopleApiClient.findByNetId(netid);
                LOGGER.tracef("Resolved via netid=%s present=%s", netid, person.isPresent());
            } else if (username.startsWith("google.")) {
                String googleUsername = username.substring("google.".length());
                person = peopleApiClient.findByGoogleUsername(googleUsername);
                LOGGER.tracef("Resolved via google_username=%s present=%s", googleUsername, person.isPresent());
            } else if (username.startsWith("wisvch.") || username.startsWith("WISVCH.")) {
                String idPart = username.substring("wisvch.".length());
                try {
                    Integer id = Integer.valueOf(idPart);
                    person = peopleApiClient.getPersonById(id);
                    LOGGER.tracef("Resolved via id=%s present=%s", Optional.of(id), person.isPresent());
                } catch (NumberFormatException ex) {
                    LOGGER.warnf("Wisvch username prefix contains invalid id '%s'", idPart);
                    person = Optional.empty();
                }
            } else {
                LOGGER.warnf("Unsupported username prefix for Dienst2UserProvider: %s", username);
                throw new IllegalArgumentException("Unsupported username prefix for Dienst2UserProvider: " + username);
            }

            return person.map(value -> toUserModel(realmModel, value)).orElse(null);
        } catch (IOException e) {
            LOGGER.errorf(e, "Failed to fetch Dienst2 person for username %s", username);
            return null;
        }
    }

    @Override
    public UserModel getUserByEmail(RealmModel realmModel, String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        LOGGER.debugf("getUserByEmail called with %s but lookup by email is unsupported", email);
        throw new UnsupportedOperationException("Dienst2UserProvider does not support lookup by email");
    }

    private UserModel toUserModel(RealmModel realm, Person person) {
        List<GroupModel> groups = resolveGoogleGroups(realm, person);
        return new Dienst2UserAdapter(session, realm, model, person, groups);
    }

    private List<GroupModel> resolveGoogleGroups(RealmModel realm, Person person) {
        if (person.getId() == null) {
            LOGGER.trace("resolveGoogleGroups skipped: person id is null");
            return Collections.emptyList();
        }
        if (person.getGoogleUsername() == null || person.getGoogleUsername().isBlank()) {
            LOGGER.tracef("resolveGoogleGroups skipped: no google_username for person %s", person.getId());
            return Collections.emptyList();
        }

        try {
            List<String> groupNames = peopleApiClient.getGoogleGroups(person.getId());
            if (groupNames == null || groupNames.isEmpty()) {
                LOGGER.tracef("No google groups for person %s", person.getId());
                return Collections.emptyList();
            }

            Map<String, GroupModel> existing = realm.getGroupsStream()
                    .collect(Collectors.toMap(GroupModel::getName, Function.identity(), (a, b) -> a));

            List<GroupModel> resolved = new ArrayList<>();
            for (String name : groupNames) {
                if (name == null || name.isBlank()) {
                    continue;
                }
                GroupModel group = existing.get(name);
                if (group == null) {
                    group = realm.createGroup(name);
                    existing.put(name, group);
                    LOGGER.debugf("Created Keycloak group '%s' from Dienst2 google groups", name);
                }
                if (!resolved.contains(group)) {
                    resolved.add(group);
                }
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.tracef("Google groups for person %s -> %s", person.getId(), groupNames);
            }

            return Collections.unmodifiableList(resolved);
        } catch (IOException e) {
            LOGGER.errorf(e, "Failed to fetch google groups for Dienst2 person %s", person.getId());
            return Collections.emptyList();
        }
    }
}
