package nl.tudelft.ch.login.dienst2.model;

import java.util.Arrays;
import java.util.Optional;

public enum MembershipStatus {
    NONE(0),
    DONATING(10),
    ALUMNUS(20),
    REGULAR(30),
    ASSOCIATE(40),
    MERIT(50),
    HONORARY(60);

    private final int code;

    MembershipStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static Optional<MembershipStatus> fromCode(Integer code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(status -> status.code == code)
                .findFirst();
    }
}

