package nl.tudelft.ch.login.dienst2.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.io.Serializable;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Student implements Serializable {

    private Boolean enrolled;
    private String study;
    private Integer firstYear;
    private String studentNumber;

    private String emergencyName;
    private String emergencyPhone;

    private LocalDate dateVerified;

    public Boolean getEnrolled() {
        return enrolled;
    }

    public String getStudy() {
        return study;
    }

    public Integer getFirstYear() {
        return firstYear;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public String getEmergencyName() {
        return emergencyName;
    }

    public String getEmergencyPhone() {
        return emergencyPhone;
    }

    public LocalDate getDateVerified() {
        return dateVerified;
    }
}
