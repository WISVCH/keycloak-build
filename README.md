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
mvn package
KEYCLOAK_VERSION="$(mvn -q -DforceStdout help:evaluate -Dexpression=keycloak.version)"
docker build --build-arg KEYCLOAK_VERSION=${KEYCLOAK_VERSION} -t keycloak-wisvch .
```

`pom.xml` (`<keycloak.version>`) is the source of truth for the Keycloak runtime version.
CI reads that value and passes it to Docker as `--build-arg KEYCLOAK_VERSION=...`, so provider dependencies and image base version stay in sync.

The image copies:
- `target/keycloak-wisvch-custom-providers.jar` to `/opt/keycloak/providers/`
- `themes/chtheme` to `/opt/keycloak/themes`

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
- `EVICT_DAILY` at `00:00`

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
- Add mapper `oidc-username-idp-mapper`:
  - template `${ALIAS}.${CLAIM.uids}`
  - target `LOCAL`
  - sync mode `FORCE`

Important coupling with federation code:
- aliases and username templates must produce usernames that match the federation lookup patterns (`google.*`, `surfconext.*`), unless you also change the provider code.
