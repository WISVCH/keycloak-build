package nl.tudelft.ch.login.dienst2.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Alumnus implements Serializable {

    private String study;
    private Integer studyFirstYear;
    private Integer studyLastYear;
    private String studyResearchGroup;
    private String studyPaper;
    private String studyProfessor;

    private String workCompany;
    private String workPosition;
    private String workSector;

    private String contactMethod;

    public String getStudy() {
        return study;
    }

    public Integer getStudyFirstYear() {
        return studyFirstYear;
    }

    public Integer getStudyLastYear() {
        return studyLastYear;
    }

    public String getStudyResearchGroup() {
        return studyResearchGroup;
    }

    public String getStudyPaper() {
        return studyPaper;
    }

    public String getStudyProfessor() {
        return studyProfessor;
    }

    public String getWorkCompany() {
        return workCompany;
    }

    public String getWorkPosition() {
        return workPosition;
    }

    public String getWorkSector() {
        return workSector;
    }

    public String getContactMethod() {
        return contactMethod;
    }
}
