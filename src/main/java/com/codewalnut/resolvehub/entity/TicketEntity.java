package com.codewalnut.resolvehub.entity;

import com.codewalnut.resolvehub.domain.TicketPriority;
import com.codewalnut.resolvehub.domain.TicketStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import com.codewalnut.resolvehub.exception.InvalidTicketTransitionException;

@Entity
@Table(name = "tickets")
public class TicketEntity {

    @Id
    private UUID id;

    @Column(name = "reference_number", nullable = false, unique = true, length = 64)
    private String referenceNumber;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TicketStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private AppUserEntity customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_agent_id")
    private AppUserEntity assignedAgent;

    @ElementCollection(fetch = FetchType.LAZY)
    @BatchSize(size = 16)
    @CollectionTable(name = "ticket_tags", joinColumns = @JoinColumn(name = "ticket_id"))
    @Column(name = "tag", nullable = false, length = 100)
    private Set<String> tags = new LinkedHashSet<>();

    @Column(name = "resolution_summary", length = 500)
    private String resolutionSummary;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected TicketEntity() {
    }

    public TicketEntity(
            UUID id,
            String referenceNumber,
            String title,
            String description,
            TicketPriority priority,
            AppUserEntity customer,
            Set<String> tags,
            Instant createdAt) {
        this.id = id;
        this.referenceNumber = referenceNumber;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = TicketStatus.OPEN;
        this.customer = customer;
        this.tags = new LinkedHashSet<>(tags);
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void resolve(String summary, Instant resolvedAt) {
        if (status != TicketStatus.IN_PROGRESS || assignedAgent == null) {
            throw new InvalidTicketTransitionException(status, TicketStatus.RESOLVED);
        }
        String normalized = summary == null ? "" : summary.trim();
        int characterCount = normalized.codePointCount(0, normalized.length());
        if (characterCount < 20 || characterCount > 500 || resolvedAt == null) {
            throw new IllegalArgumentException("A resolution summary of 20 to 500 characters and a timestamp are required");
        }
        this.status = TicketStatus.RESOLVED;
        this.resolutionSummary = normalized;
        this.resolvedAt = resolvedAt;
        this.updatedAt = resolvedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public AppUserEntity getCustomer() {
        return customer;
    }

    public AppUserEntity getAssignedAgent() {
        return assignedAgent;
    }

    public Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public String getResolutionSummary() {
        return resolutionSummary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public long getVersion() {
        return version;
    }
}
