# Keycloak Build for W.I.S.V. 'Christiaan Huygens'

This project contains everything you need to setup the Keycloak instance for authentication and authorization via OIDC
for our study association.

## What do the custom providers provide?

Our custom providers allow us to add claims from Google (Google groups) and Dienst (membership_status, contact details,
etc.) to users on login.
This is done by adding the according mapper to the identity provider in Keycloak (Admin Console -> Identity Providers ->
Select NetID/Google -> Mappers).

To ensure users are correct, an additional conditional flow should be added to the identity provider which checks
membership status (gotten from Dienst) and student status (gotten from SURFconext only).

## Resources

https://www.keycloak.org/docs/latest/server_development/index.html#_providers
https://github.com/keycloak/keycloak-quickstarts/
https://medium.com/@djordjev9/customizing-keycloak-part-1-extending-keycloak-with-user-federation-1633238d8ff5
https://www.keycloak.org/docs-api/latest/javadocs/index.html

