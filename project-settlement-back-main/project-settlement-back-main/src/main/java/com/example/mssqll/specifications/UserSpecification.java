package com.example.mssqll.specifications;

import com.example.mssqll.models.Role;
import com.example.mssqll.models.User;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserSpecification {

    public static Specification<User> getSpecifications(Map<String, Object> filters) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Always exclude SOFT_DELETED (The "AND" part)
            predicates.add(criteriaBuilder.notEqual(root.get("role"), Role.SOFT_DELETED));

            filters.forEach((key, value) -> {
                if (value != null && !value.toString().isEmpty()) {
                    String searchTerm = "%" + value.toString().toLowerCase() + "%";

                    switch (key) {
                        case "username":
                            Expression<String> fullName = criteriaBuilder.concat(
                                    criteriaBuilder.concat(root.get("firstName"), " "),
                                    root.get("lastName")
                            );
                            predicates.add(criteriaBuilder.like(
                                    criteriaBuilder.lower(fullName),
                                    searchTerm
                            ));
                            break;
                        case "email":
                            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), searchTerm));
                            break;
                    }
                }
            });

            // 3. Combine everything with AND
            // Result: Role != SOFT_DELETED AND (firstName LIKE %x% OR lastName LIKE %x%) AND email LIKE %y%
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
