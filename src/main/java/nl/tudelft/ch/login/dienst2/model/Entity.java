package nl.tudelft.ch.login.dienst2.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.io.Serializable;
import java.net.URI;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Entity implements Serializable {

    private Integer id;
    private URI url;

    private String streetAddress;
    private String formattedAddress;
    private String streetName;
    private String houseNumber;
    @JsonProperty("address_2")
    private String address2;
    @JsonProperty("address_3")
    private String address3;
    private String postcode;
    private String city;
    private String country;
    private String countryFull;

    private String email;
    private String phoneFixed;

    private Boolean machazine;

    private String comment;

    private String revisionComment;

    public Integer getId() {
        return id;
    }

    public URI getUrl() {
        return url;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public String getFormattedAddress() {
        return formattedAddress;
    }

    public String getStreetName() {
        return streetName;
    }

    public String getHouseNumber() {
        return houseNumber;
    }

    public String getAddress2() {
        return address2;
    }

    public String getAddress3() {
        return address3;
    }

    public String getPostcode() {
        return postcode;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public String getCountryFull() {
        return countryFull;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneFixed() {
        return phoneFixed;
    }

    public Boolean getMachazine() {
        return machazine;
    }

    public String getComment() {
        return comment;
    }

    public String getRevisionComment() {
        return revisionComment;
    }

    public void setRevisionComment(String revisionComment) {
        this.revisionComment = revisionComment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Entity entity = (Entity) o;
        return Objects.equals(id, entity.id)
                && Objects.equals(url, entity.url)
                && Objects.equals(streetAddress, entity.streetAddress)
                && Objects.equals(formattedAddress, entity.formattedAddress)
                && Objects.equals(streetName, entity.streetName)
                && Objects.equals(houseNumber, entity.houseNumber)
                && Objects.equals(address2, entity.address2)
                && Objects.equals(address3, entity.address3)
                && Objects.equals(postcode, entity.postcode)
                && Objects.equals(city, entity.city)
                && Objects.equals(country, entity.country)
                && Objects.equals(countryFull, entity.countryFull)
                && Objects.equals(email, entity.email)
                && Objects.equals(phoneFixed, entity.phoneFixed)
                && Objects.equals(machazine, entity.machazine)
                && Objects.equals(comment, entity.comment)
                && Objects.equals(revisionComment, entity.revisionComment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                url,
                streetAddress,
                formattedAddress,
                streetName,
                houseNumber,
                address2,
                address3,
                postcode,
                city,
                country,
                countryFull,
                email,
                phoneFixed,
                machazine,
                comment,
                revisionComment
        );
    }
}
