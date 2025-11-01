package nl.tudelft.ch.login.dienst2.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Person extends Entity {

    private String formattedName;
    private String titles;
    private String initials;
    private String firstname;
    private String preposition;
    private String surname;
    private String postfixTitles;

    private String phoneMobile;
    private String gender;
    private String pronouns;

    private LocalDate birthdate;
    private Integer age;

    private Boolean deceased;
    private URI livingWith;

    private Boolean mailAnnouncements;
    private Boolean mailCompany;
    private Boolean mailEducation;

    private String ldapUsername;
    private String googleUsername;
    private String netid;
    private String linkedinId;
    private String facebookId;

    private Integer membershipStatusValue;
    private MembershipStatus membershipStatus;

    private Member member;
    private Student student;
    private Alumnus alumnus;
    private Employee employee;

    private List<CommitteeMembership> committeeMemberships;

    public String getFormattedName() {
        return formattedName;
    }

    public String getTitles() {
        return titles;
    }

    public String getInitials() {
        return initials;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getPreposition() {
        return preposition;
    }

    public String getSurname() {
        return surname;
    }

    public String getPostfixTitles() {
        return postfixTitles;
    }

    public String getPhoneMobile() {
        return phoneMobile;
    }

    public String getGender() {
        return gender;
    }

    public String getPronouns() {
        return pronouns;
    }

    public LocalDate getBirthdate() {
        return birthdate;
    }

    public Integer getAge() {
        return age;
    }

    public Boolean getDeceased() {
        return deceased;
    }

    public URI getLivingWith() {
        return livingWith;
    }

    public Boolean getMailAnnouncements() {
        return mailAnnouncements;
    }

    public Boolean getMailCompany() {
        return mailCompany;
    }

    public Boolean getMailEducation() {
        return mailEducation;
    }

    public String getLdapUsername() {
        return ldapUsername;
    }

    public String getGoogleUsername() {
        return googleUsername;
    }

    public String getNetid() {
        return netid;
    }

    public String getLinkedinId() {
        return linkedinId;
    }

    public String getFacebookId() {
        return facebookId;
    }

    public Optional<MembershipStatus> getMembershipStatus() {
        return Optional.ofNullable(membershipStatus);
    }

    public Integer getMembershipStatusValue() {
        return membershipStatusValue;
    }

    public Optional<Member> getMember() {
        return Optional.ofNullable(member);
    }

    public Optional<Student> getStudent() {
        return Optional.ofNullable(student);
    }

    public Optional<Alumnus> getAlumnus() {
        return Optional.ofNullable(alumnus);
    }

    public Optional<Employee> getEmployee() {
        return Optional.ofNullable(employee);
    }

    public List<CommitteeMembership> getCommitteeMemberships() {
        return committeeMemberships;
    }

    @JsonProperty("membership_status")
    private void setMembershipStatusFromValue(Integer status) {
        this.membershipStatusValue = status;
        this.membershipStatus = MembershipStatus.fromCode(status).orElse(null);
    }
}
