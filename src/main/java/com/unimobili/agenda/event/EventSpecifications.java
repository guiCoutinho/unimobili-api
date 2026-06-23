package com.unimobili.agenda.event;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class EventSpecifications {

    private EventSpecifications() {
    }

    public static Specification<Event> filter(Instant de, Instant ate, UUID externalUserId,
                                              UUID createdBy, EventStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (de != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dataHoraInicio"), de));
            }
            if (ate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dataHoraInicio"), ate));
            }
            if (externalUserId != null) {
                predicates.add(cb.equal(root.get("externalUser").get("id"), externalUserId));
            }
            if (createdBy != null) {
                predicates.add(cb.equal(root.get("createdBy"), createdBy));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
