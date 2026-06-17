package com.devhouse.financial_plan.infrastructure.repository.jpa;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "space_members")
public class SpaceMemberEntityJpa {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
    @Version
    private Integer version;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SpaceEntityJpa space;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntityJpa user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private RoleEntityJpa role;
    @Column(name = "joined_at")
    private Instant joinedAt;

    public Long getId() { return id; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public SpaceEntityJpa getSpace() { return space; }
    public void setSpace(SpaceEntityJpa space) { this.space = space; }
    public UserEntityJpa getUser() { return user; }
    public void setUser(UserEntityJpa user) { this.user = user; }
    public RoleEntityJpa getRole() { return role; }
    public void setRole(RoleEntityJpa role) { this.role = role; }
    public Instant getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Instant joinedAt) { this.joinedAt = joinedAt; }
}
