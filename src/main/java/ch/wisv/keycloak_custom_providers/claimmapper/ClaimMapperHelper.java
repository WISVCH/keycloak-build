package ch.wisv.keycloak_custom_providers.claimmapper;

import ch.wisv.keycloak_custom_providers.claimmapper.models.api.Person;
import ch.wisv.keycloak_custom_providers.claimmapper.models.api.Student;
import org.keycloak.models.UserModel;

import java.util.List;

public class ClaimMapperHelper {

    protected static void setUserAttributes(UserModel user, Person person, List<String> googleGroups) {
        user.setFirstName(person.getFirstname());
        user.setLastName(person.getSurname());

        //Claims to set got from https://github.com/WISVCH/connect/blob/master/src/main/java/ch/wisv/connect/services/CHScopeClaimTranslationService.java
//        user.setSingleAttribute("", person.);

        user.setSingleAttribute("name", person.getFormattedName());
        user.setSingleAttribute("preferred_username", !person.getGoogleUsername().isBlank() ? person.getGoogleUsername() : person.getNetid());
        user.setSingleAttribute("given_name", person.getFirstname());
        user.setSingleAttribute("family_name", person.getSurnameWithPreposition());
//        user.setSingleAttribute("middle_name", person.);
//        user.setSingleAttribute("nickname", person.);
//        user.setSingleAttribute("profile", person.);
//        user.setSingleAttribute("picture", person.);
//        user.setSingleAttribute("website", person.);

        //Not very woke
        switch (person.getGender()) {
            case "M":
                user.setSingleAttribute("gender", "male");
                break;
            case "F":
                user.setSingleAttribute("gender", "female");
                break;
        }
//        user.setSingleAttribute("zone_info", person.);
//        user.setSingleAttribute("locale", person.);
//        user.setSingleAttribute("updated_at", person.);

        user.setSingleAttribute("birthdate", person.getBirthdate().toString());

        user.setSingleAttribute("email", person.getEmail());
//        user.setSingleAttribute("email_verified", person.);

        user.setSingleAttribute("phone_number", person.getPhoneMobile());
//        user.setSingleAttribute("phone_number_verified", person.);

        user.setSingleAttribute("address.street_address", person.getStreetAddress());
        user.setSingleAttribute("address.postal_code", person.getPostcode());
        user.setSingleAttribute("address.locality", person.getCity());
        user.setSingleAttribute("address.country", person.getCountry());
        user.setSingleAttribute("address.formatted", person.getFormattedAddress());

        user.setSingleAttribute("google_username", person.getGoogleUsername());
        user.setAttribute("google_groups", googleGroups);
        user.setSingleAttribute("netid", person.getNetid());

        if (person.getStudent().map(Student::isEnrolled).orElse(false)) {
            user.setSingleAttribute("student_number", person.getStudent().map(Student::getStudentNumber).orElse(null));
            user.setSingleAttribute("study", person.getStudent().map(Student::getStudy).orElse(null));
        }
    }
}
