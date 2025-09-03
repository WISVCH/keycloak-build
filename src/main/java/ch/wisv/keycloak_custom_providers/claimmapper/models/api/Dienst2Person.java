package ch.wisv.keycloak_custom_providers.claimmapper.models.api;

public class Dienst2Person {

    private String firstname;
    private String surname;
    private String google_username;
    private String netid;
    private int membership_status;


        public Dienst2Person() {
    }



    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getGoogle_username() {
        return google_username;
    }

    public void setGoogle_username(String google_username) {
        this.google_username = google_username;
    }

    public String getNetid() {
        return netid;
    }

    public void setNetid(String netid) {
        this.netid = netid;
    }

    public int getMembership_status() {
        return membership_status;
    }

    public void setMembership_status(int membership_status) {
        this.membership_status = membership_status;
    }
}
