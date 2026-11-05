package com.northbank.customerservice.repository;

import com.northbank.customerservice.dto.request.CustomerFilter;
import com.northbank.customerservice.entity.Credentials;
import com.northbank.customerservice.entity.Customer;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class CustomerSpecifications {
    private CustomerSpecifications() {}

    public static Specification<Customer> fromFilter(CustomerFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }

            if (filter.role() != null) {
                Subquery<UUID> roleCheck = query.subquery(UUID.class);
                Root<Credentials> cred = roleCheck.from(Credentials.class);
                roleCheck.select(cred.<UUID>get("customerId"))
                        .where(
                                cb.equal(cred.get("customerId"), root.get("id")),
                                cb.equal(cred.get("role"), filter.role())
                        );
                predicates.add(cb.exists(roleCheck));
            }

            String search = filter.search();
            if (search != null && !search.isBlank()) {
                String pattern = "%" + escapeLike(search.strip().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.<String>get("firstName")), pattern, '\\'),
                        cb.like(cb.lower(root.<String>get("lastName")), pattern, '\\'),
                        cb.like(cb.lower(root.<String>get("email")), pattern, '\\')
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Stops users from injecting their own wildcards
    private static String escapeLike(String input) {
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
