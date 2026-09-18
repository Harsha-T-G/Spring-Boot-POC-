package com.codewalnut.resolvehub.repository;

import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.codewalnut.resolvehub.domain.ApplicationRole;
import com.codewalnut.resolvehub.domain.TicketPriority;
import com.codewalnut.resolvehub.domain.TicketStatus;
import com.codewalnut.resolvehub.entity.AppUserEntity;
import com.codewalnut.resolvehub.entity.TicketEntity;

public final class TicketSpecifications {
    private TicketSpecifications() {
    }

    public static Specification<TicketEntity> visibleAndFiltered(AppUserEntity actor, TicketStatus status,
            TicketPriority priority, UUID assignedAgentId, String search, Sort sort) {
        return (root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();
            var roles = actor.getRoles().stream().map(role -> role.getName()).toList();
            if (!roles.contains(ApplicationRole.ADMIN)) {
                var visibility = new ArrayList<Predicate>();
                if (roles.contains(ApplicationRole.CUSTOMER)) {
                    visibility.add(builder.equal(root.get("customer").get("id"), actor.getId()));
                }
                if (roles.contains(ApplicationRole.SUPPORT_AGENT)) {
                    visibility.add(builder.equal(root.get("status"), TicketStatus.OPEN));
                    visibility.add(builder.equal(root.get("assignedAgent").get("id"), actor.getId()));
                }
                predicates.add(builder.or(visibility.toArray(Predicate[]::new)));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(builder.equal(root.get("priority"), priority));
            }
            if (assignedAgentId != null) {
                predicates.add(builder.equal(root.get("assignedAgent").get("id"), assignedAgentId));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                predicates.add(builder.or(builder.like(builder.lower(root.get("title")), pattern, '\\'),
                        builder.like(builder.lower(root.get("referenceNumber")), pattern, '\\')));
            }
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                query.orderBy(sort.stream().map(order -> {
                    jakarta.persistence.criteria.Expression<?> expression = order.getProperty().equals("priority")
                            ? builder.selectCase(root.get("priority"))
                                    .when(TicketPriority.CRITICAL, 0).when(TicketPriority.HIGH, 1)
                                    .when(TicketPriority.MEDIUM, 2).otherwise(3)
                            : root.get(order.getProperty());
                    return order.isAscending() ? builder.asc(expression) : builder.desc(expression);
                }).toList());
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
