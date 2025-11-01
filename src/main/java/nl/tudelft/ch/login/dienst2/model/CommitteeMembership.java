package nl.tudelft.ch.login.dienst2.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class CommitteeMembership implements Serializable {

    private Integer id;
    private Integer person;
    private Integer board;
    private String committee;
    private String position;

    public Integer getId() {
        return id;
    }

    public Integer getPerson() {
        return person;
    }

    public Integer getBoard() {
        return board;
    }

    public String getCommittee() {
        return committee;
    }

    public String getPosition() {
        return position;
    }
}
