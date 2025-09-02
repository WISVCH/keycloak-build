package ch.wisv.keycloak_custom_providers.dienst2;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.credential.CredentialInput;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class Dienst2UserStorageProvider implements UserStorageProvider, UserLookupProvider {

    private static final Logger logger = Logger.getLogger(Dienst2UserStorageProvider.class);
    private final KeycloakSession session;
    private final ComponentModel componentModel;

    public Dienst2UserStorageProvider(KeycloakSession keycloakSession, ComponentModel componentModel) {
        logger.info("Initializing Dienst2UserStorageProvider");
        this.session = keycloakSession;
        this.componentModel = componentModel;
    }

    @Override
    public void close() {

    }

    @Override
    public UserModel getUserById(RealmModel realmModel, String s) {
        return null;
    }

    @Override
    public UserModel getUserByUsername(RealmModel realmModel, String s) {
        logger.info("getUserByUsername");
        CloseableHttpClient httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();
        String url = componentModel.get("dienst2Url");
        String apiKey = componentModel.get("Dienst2ApiKey");
        String endpoint = componentModel.get("dienst2Endpoint");
        HttpUriRequest req = new HttpGet(url + endpoint);
        req.setHeader(HttpHeaders.AUTHORIZATION, "Token " + apiKey);
        logger.info("Request: " + req.getURI().toString());
        try {
            CloseableHttpResponse resp = httpClient.execute(req);
            String result = new BufferedReader(new InputStreamReader(resp.getEntity().getContent())).lines().collect(Collectors.joining("\n"));
            logger.info("getUserByUsername returned " + result);
        } catch (IOException e) {
            logger.error("getUsrByUsername ging fout: " + e);
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public CredentialValidationOutput getUserByCredential(RealmModel realm, CredentialInput input) {
        return UserLookupProvider.super.getUserByCredential(realm, input);
    }



    @Override
    public UserModel getUserByEmail(RealmModel realmModel, String s) {
        return null;
    }


}
