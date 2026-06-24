package nure.com.eventflow.organizer.application;

import jakarta.validation.constraints.Size;

public class OrganizerCancelForm {

    @Size(max = 1000)
    private String notes;

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
