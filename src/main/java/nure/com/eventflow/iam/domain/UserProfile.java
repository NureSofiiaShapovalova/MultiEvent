package nure.com.eventflow.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import nure.com.eventflow.shared.domain.BaseEntity;

@Entity
@Table(name = "user_profiles")
public class UserProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(length = 120)
    private String city;

    @Column(length = 1000)
    private String bio;

    protected UserProfile() {
    }

    public UserProfile(String firstName, String lastName, String phoneNumber, String city, String bio) {
        update(firstName, lastName, phoneNumber, city, bio);
    }

    public User getUser() {
        return user;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getCity() {
        return city;
    }

    public String getBio() {
        return bio;
    }

    public void update(String firstName, String lastName, String phoneNumber, String city, String bio) {
        this.firstName = blankToNull(firstName);
        this.lastName = blankToNull(lastName);
        this.phoneNumber = blankToNull(phoneNumber);
        this.city = blankToNull(city);
        this.bio = blankToNull(bio);
    }

    void attachTo(User user) {
        this.user = user;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
