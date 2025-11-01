package nl.tudelft.ch.login.dienst2.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.net.URI;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PeopleResponse implements Serializable {

    private int count;
    private URI next;
    private URI previous;
    private List<Person> results;

    public int getCount() {
        return count;
    }

    public URI getNext() {
        return next;
    }

    public URI getPrevious() {
        return previous;
    }

    public List<Person> getResults() {
        return results;
    }
}

