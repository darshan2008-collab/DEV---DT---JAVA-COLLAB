package com.smartambulance.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "hospitals")
public class Hospital {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "emergency_contact", length = 64)
    private String emergencyContact;

    @Column(name = "total_beds")
    private Integer totalBeds = 50;

    @Column(name = "available_beds")
    private Integer availableBeds = 12;

    @Column(length = 255)
    private String notes;

    public Hospital() {}

    public Hospital(String id, String name, String address, Double latitude, Double longitude, String emergencyContact, Integer totalBeds, Integer availableBeds, String notes) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.emergencyContact = emergencyContact;
        this.totalBeds = totalBeds;
        this.availableBeds = availableBeds;
        this.notes = notes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }
    public Integer getTotalBeds() { return totalBeds; }
    public void setTotalBeds(Integer totalBeds) { this.totalBeds = totalBeds; }
    public Integer getAvailableBeds() { return availableBeds; }
    public void setAvailableBeds(Integer availableBeds) { this.availableBeds = availableBeds; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
