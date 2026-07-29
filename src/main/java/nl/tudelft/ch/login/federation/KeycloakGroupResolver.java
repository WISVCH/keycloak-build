package nl.tudelft.ch.login.federation;

import org.jboss.logging.Logger;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;

import java.util.*;

/**
 * Resolves Dienst2 group names to groups in a Keycloak realm.
 */
final class KeycloakGroupResolver {
    private static final Logger LOGGER = Logger.getLogger(KeycloakGroupResolver.class);

    /**
     * Returns the realm groups corresponding to the supplied Dienst2 names.
     */
    List<GroupModel> resolve(RealmModel realm, List<String> groupNames) {
        if (groupNames.isEmpty()) {
            return List.of();
        }

        Map<String, GroupModel> groupsByName = new HashMap<>();
        realm.getGroupsStream().forEach(group -> groupsByName.putIfAbsent(group.getName(), group));
        List<GroupModel> resolvedGroups = new ArrayList<>();
        for (String name : groupNames) {
            GroupModel group = groupsByName.get(name);
            if (group == null) {
                group = realm.createGroup(name);
                groupsByName.put(name, group);
                LOGGER.debugf("Created Keycloak group '%s'", name);
            }
            resolvedGroups.add(group);
        }
        return Collections.unmodifiableList(resolvedGroups);
    }
}
