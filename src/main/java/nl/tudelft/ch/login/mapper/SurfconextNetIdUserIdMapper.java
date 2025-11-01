package nl.tudelft.ch.login.mapper;

import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.mappers.AbstractClaimMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

/**
 * Mapper that forces surfconext logins to use the stable netid claim as the broker user id.
 */
public class SurfconextNetIdUserIdMapper extends AbstractClaimMapper {

    public static final String PROVIDER_ID = "surfconext-netid-userid";
    private static final Logger LOGGER = Logger.getLogger(SurfconextNetIdUserIdMapper.class);

    private static final String[] COMPATIBLE_PROVIDERS = {
            KeycloakOIDCIdentityProviderFactory.PROVIDER_ID,
            OIDCIdentityProviderFactory.PROVIDER_ID
    };

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES =
            ProviderConfigurationBuilder.create()
                    .property()
                    .name(CLAIM)
                    .label("NetID claim path")
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .defaultValue("uids[0]")
                    .helpText("JSON claim path that contains the Surfconext netid")
                    .add()
                    .build();

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return true;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayCategory() {
        return "Preprocessor";
    }

    @Override
    public String getDisplayType() {
        return "Surfconext NetID as User ID";
    }

    @Override
    public String getHelpText() {
        return "Use the Surfconext netid claim as the broker user identifier";
    }

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        Object claim = getClaimValue(mapperModel, context);
        String netid = extractString(claim);
        if (netid == null || netid.isBlank()) {
            LOGGER.debugf("NetID claim '%s' missing for provider %s", mapperModel.getConfig().get(CLAIM), context.getIdpConfig().getAlias());
            return;
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.tracef("Setting broker user id to netid=%s", netid);
        }
        context.setId(netid);
        context.setLegacyId(netid);
        context.setBrokerUserId(netid);
        context.setUsername(netid);
        context.setModelUsername(netid);
    }

    private String extractString(Object claim) {
        if (claim == null) {
            return null;
        }
        if (claim instanceof List<?> list) {
            if (list.isEmpty()) {
                return null;
            }
            Object first = list.get(0);
            return first == null ? null : first.toString();
        }
        return claim.toString();
    }
}
