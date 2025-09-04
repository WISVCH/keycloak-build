package ch.wisv.keycloak_custom_providers.claimmapper.services;

import ch.wisv.keycloak_custom_providers.claimmapper.models.exception.UserNotFoundException;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.cloudidentity.v1.CloudIdentity;
import com.google.api.services.cloudidentity.v1.CloudIdentityRequestInitializer;
import com.google.api.services.cloudidentity.v1.model.SearchTransitiveGroupsResponse;
import com.google.auth.oauth2.GoogleCredentials;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

public class GoogleAccountService {

    private final String CUSTOMER_ID = "C03nrg5fp";
    private final String PARENT = "groups/-";

    private static final Logger logger = Logger.getLogger(GoogleAccountService.class);

    private GoogleCredentials credentials;
    private final CloudIdentity cloudIdentity;

    public GoogleAccountService() {
        try {
            HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();

            CloudIdentityRequestInitializer initializer = new CloudIdentityRequestInitializer();
            com.google.api.client.json.JsonFactory jsonFactory1 = new GsonFactory();
            this.cloudIdentity = new CloudIdentity.Builder(transport, jsonFactory1, transport.createRequestFactory().getInitializer()).setCloudIdentityRequestInitializer(initializer).build();

        } catch (IOException | GeneralSecurityException e) {
            logger.fatal("Could not get initialize google service, google will not work correctly: ", e);
            throw new IllegalStateException(e);
        }
    }

    public void initializeCredentials() {
        try {
            credentials = GoogleCredentials.getApplicationDefault();
        } catch (IOException e) {
            logger.fatal("Could not get initialize google service, google will not work correctly: ", e);
            throw new IllegalStateException(e);
        }
    }

    public List<String> retrieveGoogleGroups(String email) throws UserNotFoundException {
        logger.info("Retrieving groups for email: " + email);
        List<String> googleGroups = new ArrayList<>();
        try {
            if (credentials == null) {
                initializeCredentials();
            }

            if (credentials != null) {
                if (credentials.getAccessToken() == null) {
                    credentials.refresh();
                }
                credentials.refreshIfExpired();
                String accessToken = credentials.getAccessToken().getTokenValue();

                SearchTransitiveGroupsResponse response = cloudIdentity.groups().memberships()
                        .searchTransitiveGroups(PARENT)
                        .setQuery("member_key_id == '" + email + "' && 'cloudidentity.googleapis.com/groups.discussion_forum' in labels && parent == 'customers/" + CUSTOMER_ID + "'")
                        .setAccessToken(accessToken)
                        .execute();

                response.getMemberships().forEach(member -> {
                    googleGroups.add(member.getGroupKey().getId());
                });
                logger.info("Found " + response.getMemberships().size() + " groups for email " + email + ", groups: " + String.join(", ", googleGroups));
            } else {
                logger.error("creds zijn null, lijst blijft leeg...");
                throw new UserNotFoundException();
            }
        } catch (IOException e) {
            logger.error("Could not get google groups.", e);
            throw new UserNotFoundException();
        }
        return getSlugsFromEmails(googleGroups);
    }

    private List<String> getSlugsFromEmails(List<String> groupEmails) {
        List<String> slugs = new ArrayList<>();
        for (String email : groupEmails) {
            String slug = email
                    .replace("-commissie@ch.tudelft.nl", "")
                    .replace("-group@ch.tudelft.nl", "")
                    .replace("@ch.tudelft.nl", "");
            slugs.add(slug);
        }
        return slugs;
    }
}
