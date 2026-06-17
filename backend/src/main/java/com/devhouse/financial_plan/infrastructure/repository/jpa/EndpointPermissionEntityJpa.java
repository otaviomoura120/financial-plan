package com.devhouse.financial_plan.infrastructure.repository.jpa;

import jakarta.persistence.*;

import java.time.Instant;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "endpoint_permissions")
public class EndpointPermissionEntityJpa {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
    @Version
    private Integer version;
    private String endpoint;
    private String name;
    private String icon;
    private Integer sequence;
    private String type;
    @Column(name = "permitted_methods")
    private String permittedMethods;
    @Column(name = "ep_group")
    private String epGroup;
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;

    public Long getId() { return id; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Integer getSequence() { return sequence; }
    public void setSequence(Integer sequence) { this.sequence = sequence; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPermittedMethods() { return permittedMethods; }
    public void setPermittedMethods(String permittedMethods) { this.permittedMethods = permittedMethods; }
    public String getEpGroup() { return epGroup; }
    public void setEpGroup(String epGroup) { this.epGroup = epGroup; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
