# Keycloak Build for W.I.S.V. 'Christiaan Huygens'

![Example login page](docs/login_page.png)

This repository contains a Keycloak build with:
- a custom user federation provider (`Dienst2`)
- a custom login theme (`chtheme`)

## How the login flow works

1. A user logs in with Google or SURFconext.
2. An IdP mapper turns external claims into a Keycloak username used for lookup.
3. Keycloak calls the `Dienst2` federation provider with that username.
4. The provider resolves the person in Dienst2 and returns a federated Keycloak user (`WISVCH.<id>`), including membership-related attributes.
5. Google groups are fetched by Dienst2 from Google Workspace and returned to Keycloak as groups.
6. On first broker login, Keycloak auto-links the IdP account to the existing federated user (without confirmation).
7. Keycloak issues OIDC tokens to your applications.

This means users are not managed as local Keycloak users. Dienst2 is the source of truth.

## Build

Run from the `keycloak-build` repository root:

```bash
mvn clean package
KEYCLOAK_VERSION="$(mvn -q -DforceStdout help:evaluate -Dexpression=keycloak.version)"
docker build --build-arg KEYCLOAK_VERSION=${KEYCLOAK_VERSION} -t keycloak-wisvch .
```

`pom.xml` (`<keycloak.version>`) is the source of truth for the Keycloak runtime version.
CI reads that value and passes it to Docker as `--build-arg KEYCLOAK_VERSION=...`, so provider dependencies and image base version stay in sync.

The image copies:
- `target/keycloak-wisvch-custom-providers.jar` to `/opt/keycloak/providers/`
- `themes/chtheme` to `/opt/keycloak/themes`

## Local development

The repository includes a Docker Compose development stack with PostgreSQL. It
mounts the theme and the locally built provider JAR, so it is separate from the
production image build above.

### First start

```bash
cp .env.example .env
# Set the local/test credentials in .env.
./scripts/dev-up.sh
```

`.env` is the single runtime configuration source; The values are used as follows:

- `KC_BOOTSTRAP_ADMIN_USERNAME` and `KC_BOOTSTRAP_ADMIN_PASSWORD` create the
  local admin-console account. Choose these yourself.
- `KC_DB_PASSWORD` protects the local PostgreSQL database. Choose this yourself.
- `DIENST2_BASE_URL` and `DIENST2_API_KEY` configure the Dienst2 federation
  provider. It is best to set up a local Dienst2 instance, following the
  instructions in the [Dienst2 repository](https://github.com/wisvch/dienst2).
  Use that instance's URL and API key here. If a local instance is not possible,
  ask in the Beheer chat for a development endpoint and API key that you may use;
  do not reuse a production key.
- `SURFCONEXT_CLIENT_ID` and `SURFCONEXT_CLIENT_SECRET` enable `NetID test`.
  Ask in the Beheer chat for an existing development credential first. If you
  need to create one, sign in to [SURFconext SP
  Dashboard](https://sp.surfconext.nl/) with eduID and the Beheer email account,
  select the test environment, and create a client secret there. Put its client
  ID and secret in `.env`. `NetID test` is not the real NetID login: it uses
  fake NetIDs. Ask in the Beheer chat for the test usernames and passwords.
- `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` enable Google login. Ask in the
  Beheer chat for an existing development credential first. Otherwise, in Google
  Cloud Console, open [APIs & Services → Credentials](https://console.cloud.google.com/apis/credentials),
  and create an **OAuth client ID** for a **Web application**. Complete the
  required consent-screen details, then copy the generated client ID and client
  secret into `.env`.

Keycloak is available at [http://localhost:8181](http://localhost:8181). The
admin credentials are `KC_BOOTSTRAP_ADMIN_USERNAME` and
`KC_BOOTSTRAP_ADMIN_PASSWORD` from `.env`.

After signing in, use the realm selector in the upper-left corner to switch from
the `master` realm to `ch-dev`. Configure or inspect the development identity
providers, federation provider, theme, and login flow in `ch-dev`, not `master`.

The initial realm is `ch-dev`, defined in `realm/ch-dev-realm.json`. It includes
the CH login theme, production-compatible Google and SURFconext mapping/login
behaviour, and the SURFconext test endpoints (shown as `NetID test`). It also
includes the automatic broker-linking flow, the CH user-profile attributes, and
the Dienst2 user-federation component. Its client secrets and Dienst2 API key
are placeholders resolved from `.env` at import time.

Test the login flow at
[http://localhost:8181/realms/ch-dev/account/](http://localhost:8181/realms/ch-dev/account/).

### Development

- Theme changes are mounted directly; refresh the browser to see them. Theme
  and template caching is disabled for the local container.
- After changing Java/provider code, run `./scripts/dev-rebuild.sh`.
- Stop the stack with `./scripts/dev-down.sh`.
- After changing the realm JSON, run `./scripts/dev-reset.sh`. It explicitly
  deletes the local database volume and re-imports the realm; startup imports
  never overwrite an existing realm.

## Keycloak setup

Use your own URLs, client IDs, secrets, and API tokens.  
Only settings that are required for this architecture are listed below.

### 1) Configure realm behavior

Set these realm settings:
- `frontendUrl=https://<your-login-host>`
- `loginTheme=chtheme` (if you want to use the bundled login theme from this repo)
- `registrationAllowed=false`
- `rememberMe=false`
- `resetPasswordAllowed=false`
- `verifyEmail=false`
- `loginWithEmailAllowed=false`
- `duplicateEmailsAllowed=true`

Disable all Required Actions.

Account linking in this setup is username-driven and automatic, so self-service and required-action interruptions should be off.

### 2) Configure user profile attributes

Add these custom user profile attributes:
- `google_username`
- `netid`
- `membership_status`
- `formatted_name`

Set them to non-editable.  
Set visibility to match the current model:
- `google_username`, `netid`, `formatted_name`: view by `admin` and `user`
- `membership_status`: view by `admin` only

Set `membership_status` as required for role `user`.

These attributes are populated from Dienst2 and used for authorization/state checks; users should not be able to edit them in Keycloak.

### 3) Configure first broker login flow

Create a top-level flow with alias `link exisiting without confimation` and add:
1. `idp-detect-existing-broker-user` as `REQUIRED`
2. `idp-auto-link` as `REQUIRED`

This makes first IdP login link directly to an existing user without a confirmation screen.
This flow must be top-level because IdPs reference it via `firstBrokerLoginFlowAlias`.

### 4) Configure user federation (`Dienst2`)

Add a User Federation provider:
- Provider ID: `Dienst2`
- `baseUrl=https://<dienst2-host>`
- `apiEndpoint=/<dienst2-api-path>`
- `apiKey=<dienst2-api-token>`
- `enabled=true`

Recommended cache policy:
- `MAX_LIFESPAN` with a lifespan of `60000` milliseconds (60 seconds).

The provider additionally keeps successful Dienst2 person and Google-group
lookups in a process-local cache for 60 seconds. This absorbs repeated broker
lookups that intentionally bypass Keycloak's user cache. Failed and not-found
lookups are not cached.

Behavior that drives the rest of the setup:
- `surfconext.<netid>` is resolved via Dienst2 `netid`
- `google.<localpart>` is resolved via Dienst2 `google_username`
- resolved users are exposed as `WISVCH.<id>`
- Google groups are fetched through Dienst2 (which syncs from Google Workspace) and mapped to Keycloak groups

### 5) Configure identity providers

For each IdP, set:
- `syncMode=FORCE`
- `firstBrokerLoginFlowAlias=link exisiting without confimation`
- `trustEmail=false`

Google IdP:
- Use Keycloak `google` provider.
- Optional restriction: `hostedDomain=<your-domain>`
- Keep `disableUserInfo=false`.
- Add mapper `oidc-username-idp-mapper`:
  - template `${ALIAS}.${CLAIM.email | localpart}`
  - target `LOCAL`
  - sync mode `IMPORT`

SURFconext (or another OIDC IdP):
- Use `oidc` provider with your own OIDC endpoints and issuer.
- Keep signature validation enabled (`validateSignature=true`, `useJwksUrl=true`).
- Use `clientAuthMethod=client_secret_post` unless your provider requires a different method.
- In the SURF SP Dashboard, set **Subject type** to **Persistent** (not **Transient**). Returning-user linking depends on stable user identifiers; with transient IDs, returning users cannot be linked reliably.
- Add mapper `oidc-username-idp-mapper`:
  - template `${ALIAS}.${CLAIM.uids}`
  - target `LOCAL`
  - sync mode `FORCE`

Important coupling with federation code:
- aliases and username templates must produce usernames that match the federation lookup patterns (`google.*`, `surfconext.*`), unless you also change the provider code.
