package com.devhouse.financial_plan.infrastructure.repository.jpa;

import jakarta.persistence.*;

import java.time.Instant;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "group_menu_children")
public class GroupMenuChildrenEntityJpa {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
    private String name;
    private String endpoint;
    private String icon;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_menu_id")
    private GroupMenuEntityJpa groupMenu;
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public GroupMenuEntityJpa getGroupMenu() { return groupMenu; }
    public void setGroupMenu(GroupMenuEntityJpa groupMenu) { this.groupMenu = groupMenu; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
