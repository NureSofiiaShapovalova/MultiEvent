package nure.com.eventflow.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Objects;
import nure.com.eventflow.shared.domain.BaseEntity;

@Entity
@Table(name = "venues")
public class Venue extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(name = "address_line", nullable = false, length = 200)
    private String addressLine;

    @Column(name = "postal_code", length = 30)
    private String postalCode;

    @Column(nullable = false, length = 120)
    private String country;

    protected Venue() {
    }

    public Venue(String name, String city, String addressLine, String postalCode, String country) {
        this.name = name;
        this.city = city;
        this.addressLine = addressLine;
        this.postalCode = postalCode;
        this.country = country;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void update(String name, String city, String addressLine, String postalCode, String country) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.city = Objects.requireNonNull(city, "city must not be null");
        this.addressLine = Objects.requireNonNull(addressLine, "addressLine must not be null");
        this.postalCode = postalCode;
        this.country = Objects.requireNonNull(country, "country must not be null");
    }
}
