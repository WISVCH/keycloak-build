package ch.wisv.keycloak_custom_providers.claimmapper.models.exception;

public class UserNotFoundException extends Exception {

    public UserNotFoundException() {
        super();
    }

    public UserNotFoundException(String message) {
        super(message);
    }
}
