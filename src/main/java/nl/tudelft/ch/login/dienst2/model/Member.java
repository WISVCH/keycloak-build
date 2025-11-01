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
public class Member implements Serializable {

    private LocalDate dateFrom;
    private LocalDate dateTo;
    private LocalDate datePaid;
    private Integer amountPaid;

    private Boolean associateMember;
    private Boolean donatingMember;

    private LocalDate meritDateFrom;
    private Boolean meritInvitations;

    private LocalDate honoraryDateFrom;

    private Boolean currentMember;
    private Boolean currentAssociateMember;
    private Boolean currentDonatingMember;
    private Boolean currentMeritMember;
    private Boolean currentHonoraryMember;

    public LocalDate getDateFrom() {
        return dateFrom;
    }

    public LocalDate getDateTo() {
        return dateTo;
    }

    public LocalDate getDatePaid() {
        return datePaid;
    }

    public Integer getAmountPaid() {
        return amountPaid;
    }

    public Boolean getAssociateMember() {
        return associateMember;
    }

    public Boolean getDonatingMember() {
        return donatingMember;
    }

    public LocalDate getMeritDateFrom() {
        return meritDateFrom;
    }

    public Boolean getMeritInvitations() {
        return meritInvitations;
    }

    public LocalDate getHonoraryDateFrom() {
        return honoraryDateFrom;
    }

    public Boolean getCurrentMember() {
        return currentMember;
    }

    public Boolean getCurrentAssociateMember() {
        return currentAssociateMember;
    }

    public Boolean getCurrentDonatingMember() {
        return currentDonatingMember;
    }

    public Boolean getCurrentMeritMember() {
        return currentMeritMember;
    }

    public Boolean getCurrentHonoraryMember() {
        return currentHonoraryMember;
    }
}
