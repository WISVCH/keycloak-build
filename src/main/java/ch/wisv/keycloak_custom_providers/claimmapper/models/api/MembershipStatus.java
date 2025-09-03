package ch.wisv.keycloak_custom_providers.claimmapper.models.api;

public enum MembershipStatus {
    NOT_MEMBER(0),
    DONATING(10),
    ALUMNUS(20),
    REGULAR_MEMBER(30),
    EXTRAORDINARY_MEMBER(40),
    MEMBER_OF_MERIT(50),
    HONORARY_MEMBER(60),
    ;

    private int value;

    MembershipStatus(int value) {
            this.value = value;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
