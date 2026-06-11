package com.devhouse.financial_plan.domain;

import com.devhouse.financial_plan.domain.exception.DomainException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.util.Objects;

public class User {

    private Long id;
    private Integer version;
    private Long familyId;
    private String name;
    private String ministry;
    private String nickname;
    private String profilePhoto;
    private String observation;
    private Instant birthdate;
    private String email;
    private String phoneNumber;
    private boolean active;
    private String genre;
    private String maritalStatus;
    private final Instant createdDate;
    private Instant updatedDate;

    public User(Long id, Integer version, Long familyId, String name, String ministry, String nickname, String profilePhoto, String observation, Instant birthdate, String email, String phoneNumber, boolean active, String genre, String maritalStatus, Instant createdDate, Instant updatedDate) {
        this.id = id;
        this.version = version;
        this.familyId = familyId;
        this.name = name;
        this.ministry = ministry;
        this.nickname = nickname;
        this.profilePhoto = profilePhoto;
        this.observation = observation;
        this.birthdate = birthdate;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.active = active;
        this.genre = genre;
        this.maritalStatus = maritalStatus;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
    }

    public void validate() {
        if (name == null || name.isBlank()) {
            throw new DomainException("User name cannot be empty");
        }
        if (email == null || email.isBlank()) {
            throw new DomainException("User email cannot be empty");
        }
    }

    public void update(String name, String ministry, String nickname, String profilePhoto,
                       String observation, Instant birthdate, String phoneNumber,
                       String genre, String maritalStatus) {
        this.name = name;
        this.ministry = ministry;
        this.nickname = nickname;
        this.profilePhoto = profilePhoto;
        this.observation = observation;
        this.birthdate = birthdate;
        this.phoneNumber = phoneNumber;
        this.genre = genre;
        this.maritalStatus = maritalStatus;
        this.updatedDate = Instant.now();
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) {
        if (!Objects.equals(version, this.version)) {
            throw new ObjectOptimisticLockingFailureException("Error optimistic locking user", new Exception());
        }
        this.version = version;
    }

    public Long getFamilyId() { return familyId; }
    public void setFamilyId(Long familyId) { this.familyId = familyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMinistry() { return ministry; }
    public void setMinistry(String ministry) { this.ministry = ministry; }
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
    public Instant getCreatedDate() { return createdDate; }
    public Instant getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(Instant updatedDate) { this.updatedDate = updatedDate; }
}
