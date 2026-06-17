package com.devhouse.financial_plan.infrastructure.repository.jpa;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "group_menus")
public class GroupMenuEntityJpa {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
    @Version
    private Integer version;
    private String name;
    private String icon;
    @OneToMany(mappedBy = "groupMenu", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GroupMenuChildrenEntityJpa> children;
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;

    public Long getId() { return id; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public List<GroupMenuChildrenEntityJpa> getChildren() { return children; }
    public void setChildren(List<GroupMenuChildrenEntityJpa> children) { this.children = children; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
