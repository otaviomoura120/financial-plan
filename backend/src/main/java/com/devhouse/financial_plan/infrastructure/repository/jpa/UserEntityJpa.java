package com.devhouse.financial_plan.infrastructure.repository.jpa;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "members")
public class UserEntityJpa {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
    private Integer version;
    private String name;
    private String nickname;
    @Column(name = "profile_photo")
    private String profilePhoto;
    private String observation;
    private Instant birthdate;
    private String email;
    @Column(name = "phone_number")
    private String phoneNumber;
    private boolean active;
    private String genre;
    @Column(name = "marital_status")
    private String maritalStatus;
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;

    public void setVersion(Integer version) {
        if (!Objects.equals(version, this.version)) {
            throw new OptimisticLockException("Error optimistic locking member", new Exception());
        }
    }

    public Long getId() { return id; }
    public Integer getVersion() { return version; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getProfilePhoto() { return profilePhoto; }
    public void setProfilePhoto(String profilePhoto) { this.profilePhoto = profilePhoto; }
    public String getObservation() { return observation; }
    public void setObservation(String observation) { this.observation = observation; }
    public Instant getBirthdate() { return birthdate; }
    public void setBirthdate(Instant birthdate) { this.birthdate = birthdate; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public String getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(String maritalStatus) { this.maritalStatus = maritalStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
