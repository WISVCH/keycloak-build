package nl.tudelft.ch.login.federation;

import com.github.benmanes.caffeine.cache.Cache;
import nl.tudelft.ch.login.dienst2.client.PeopleApiClient;
import nl.tudelft.ch.login.dienst2.model.Person;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Caches successful Dienst2 lookup results.
 */
final class CachedDienst2Lookup {
    private static final Logger LOGGER = Logger.getLogger(CachedDienst2Lookup.class);

    private final Cache<String, Person> personCache;
    private final Cache<String, List<String>> groupCache;

    CachedDienst2Lookup(Cache<String, Person> personCache, Cache<String, List<String>> groupCache) {
        this.personCache = personCache;
        this.groupCache = groupCache;
    }

    /** Finds a person by Dienst2 ID. */
    Optional<Person> findPersonById(String componentId, Integer personId, PeopleApiClient client) throws IOException {
        String cacheKey = personCacheKey(componentId, "id", personId.toString());
        Person cached = personCache.getIfPresent(cacheKey);
        if (cached != null) {
            LOGGER.tracef("Using cached person %s", personId);
            return Optional.of(cached);
        }

        Optional<Person> person = client.getPersonById(personId);
        person.ifPresent(value -> cachePerson(componentId, value));
        return person;
    }

    /** Finds a person by NetID. */
    Optional<Person> findPersonByNetId(String componentId, String netId, PeopleApiClient client) throws IOException {
        String cacheKey = personCacheKey(componentId, "netid", netId);
        Person cached = personCache.getIfPresent(cacheKey);
        if (cached != null) {
            LOGGER.tracef("Using cached person for netid=%s", netId);
            return Optional.of(cached);
        }

        Optional<Person> person = client.findByNetId(netId);
        person.ifPresent(value -> cachePerson(componentId, value));
        return person;
    }

    /** Finds a person by Google username. */
    Optional<Person> findPersonByGoogleUsername(String componentId, String googleUsername, PeopleApiClient client) throws IOException {
        String cacheKey = personCacheKey(componentId, "google-username", googleUsername);
        Person cached = personCache.getIfPresent(cacheKey);
        if (cached != null) {
            LOGGER.tracef("Using cached person for google_username=%s", googleUsername);
            return Optional.of(cached);
        }

        Optional<Person> person = client.findByGoogleUsername(googleUsername);
        person.ifPresent(value -> cachePerson(componentId, value));
        return person;
    }

    /** Returns Google group names for a person. */
    List<String> getGoogleGroups(String componentId, Integer personId, PeopleApiClient client) throws IOException {
        String cacheKey = groupCacheKey(componentId, personId);
        List<String> cached = groupCache.getIfPresent(cacheKey);
        if (cached != null) {
            LOGGER.tracef("Using cached google groups for person %s", personId);
            return cached;
        }

        List<String> groups = client.getGoogleGroups(personId).stream()
                .filter(Objects::nonNull)
                .filter(name -> !name.isBlank())
                .distinct()
                .toList();
        if (!groups.isEmpty()) {
            groupCache.put(cacheKey, groups);
        }
        return groups;
    }

    private void cachePerson(String componentId, Person person) {
        if (person.getId() != null) {
            personCache.put(personCacheKey(componentId, "id", person.getId().toString()), person);
        }
        if (person.getNetid() != null && !person.getNetid().isBlank()) {
            personCache.put(personCacheKey(componentId, "netid", person.getNetid()), person);
        }
        if (person.getGoogleUsername() != null && !person.getGoogleUsername().isBlank()) {
            personCache.put(personCacheKey(componentId, "google-username", person.getGoogleUsername()), person);
        }
    }

    private String personCacheKey(String componentId, String lookupType, String value) {
        return componentId + ":person:" + lookupType + ':' + value;
    }

    private String groupCacheKey(String componentId, Integer personId) {
        return componentId + ":groups:" + personId;
    }
}
