package nl.tudelft.ch.login.dienst2;

import nl.tudelft.ch.login.dienst2.model.MembershipStatus;
import nl.tudelft.ch.login.dienst2.model.Person;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class Dienst2UserAdapter extends AbstractUserAdapterFederatedStorage {

    private final ComponentModel storageModel;
    private final Person person;
    private final List<GroupModel> googleGroups;

    public Dienst2UserAdapter(KeycloakSession session, RealmModel realm, ComponentModel storageModel, Person person, List<GroupModel> googleGroups) {
        super(session, realm, storageModel);
        this.storageModel = storageModel;
        this.person = person;
        this.googleGroups = googleGroups == null ? Collections.emptyList() : List.copyOf(googleGroups);
    }

    @Override
    public String getUsername() {
        Integer id = person.getId();
        return id == null ? null : usernameForId(id);
    }

    @Override
    public void setUsername(String username) {
        setSingleAttributeOrRemove(UserModel.USERNAME, username);
    }

    @Override
    public String getFirstName() {
        return person.getFirstname();
    }

    @Override
    public void setFirstName(String firstName) {
        // No-op: Dienst2 data is authoritative
    }

    @Override
    public String getLastName() {
        String surname = Optional.ofNullable(person.getSurname()).orElse("");
        String preposition = Optional.ofNullable(person.getPreposition()).orElse("");
        return (preposition + " " + surname).trim();
    }

    @Override
    public void setLastName(String lastName) {
        // No-op: Dienst2 data is authoritative
    }

    @Override
    public String getEmail() {
        if (person.getEmail() != null && !person.getEmail().isBlank()) {
            return person.getEmail();
        }
        String googleUsername = person.getGoogleUsername();
        if (googleUsername != null && !googleUsername.isBlank()) {
            return googleUsername + "@ch.tudelft.nl";
        }
        return null;
    }

    @Override
    public void setEmail(String email) {
        // No-op: Dienst2 data is authoritative
    }

    @Override
    public boolean isEmailVerified() {
        return getEmail() != null;
    }

    @Override
    public void setEmailVerified(boolean verified) {
        // No-op; verification mirrors Dienst2 data
    }

    @Override
    public boolean isEnabled() {
        return person.getMembershipStatus()
                .map(status -> status != MembershipStatus.NONE)
                .orElse(false);
    }

    @Override
    public void setEnabled(boolean enabled) {
        // No-op; activation is controlled by Dienst2 membership status
    }

    @Override
    public String getId() {
        Integer id = person.getId();
        if (id == null) {
            return super.getId();
        }
        return StorageId.keycloakId(storageModel, id.toString());
    }

    public Person getPerson() {
        return person;
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        Map<String, List<String>> attributes = new LinkedHashMap<>(super.getAttributes());
        putIfNotBlank(attributes, "netid", person.getNetid());
        putIfNotBlank(attributes, "ldap_username", person.getLdapUsername());
        putIfNotBlank(attributes, "google_username", person.getGoogleUsername());
        if (person.getId() != null) {
            attributes.put(UserModel.USERNAME, List.of(usernameForId(person.getId())));
        }
        putIfNotBlank(attributes, UserModel.FIRST_NAME, person.getFirstname());
        putIfNotBlank(attributes, UserModel.LAST_NAME, getLastName());
        putIfNotBlank(attributes, UserModel.EMAIL, getEmail());
        if (person.getMembershipStatusValue() != null) {
            attributes.put("membership_status", List.of(person.getMembershipStatusValue().toString()));
        }
        putIfNotBlank(attributes, "formatted_name", person.getFormattedName());
        return attributes;
    }

    private void putIfNotBlank(Map<String, List<String>> attributes, String key, String value) {
        if (value != null && !value.isBlank()) {
            attributes.put(key, List.of(value));
        }
    }

    private void setSingleAttributeOrRemove(String name, String value) {
        if (value == null) {
            removeAttribute(name);
        } else {
            setSingleAttribute(name, value);
        }
    }

    private String usernameForId(Integer id) {
        return "WISVCH." + id;
    }

    @Override
    public Stream<RoleModel> getRoleMappingsStream() {
        Stream<RoleModel> base = super.getRoleMappingsStream();
        RoleModel defaultRole = realm.getDefaultRole();
        if (defaultRole == null) {
            return base;
        }
        Stream<RoleModel> defaultRoles = Stream.concat(
                Stream.of(defaultRole),
                defaultRole.getCompositesStream()
        );
        return Stream.concat(base, defaultRoles).distinct();
    }

    @Override
    public Stream<GroupModel> getGroupsStream() {
        return googleGroups.stream();
    }
}
