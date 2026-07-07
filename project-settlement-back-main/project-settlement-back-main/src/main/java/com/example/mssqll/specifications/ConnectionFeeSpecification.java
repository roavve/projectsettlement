package com.example.mssqll.specifications;

import com.example.mssqll.models.ConnectionFee;
import com.example.mssqll.models.ExtractionTask;
import com.example.mssqll.models.OrderStatus;
import com.example.mssqll.models.Status;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.example.mssqll.utiles.SecurityUtils.getCurrentUsername;

@Slf4j
public class ConnectionFeeSpecification {


    /**
     * Extended behaviour: return parent rows where EITHER the parent itself OR any
     * of its direct children match the filters.  Each parent appears at most once
     * (no duplicates) because the child check uses an EXISTS sub-query.
     */
    public static Specification<ConnectionFee> getSpecificationsIncludingChildren(Map<String, Object> filters) {
        return (root, query, criteriaBuilder) -> {
            // Only top-level (parent) records
            Predicate isParent = criteriaBuilder.isNull(root.get("parent"));

            // Predicates that apply to the parent row itself
            List<Predicate> parentPredicates = buildFilterPredicates(root, criteriaBuilder, filters);
            Predicate parentMatches = criteriaBuilder.and(parentPredicates.toArray(new Predicate[0]));

            // Non-correlated sub-query: collect the parent_id values of children that
            // match the filters.  Using IN instead of EXISTS avoids implicit joins that
            // Hibernate can introduce for correlated sub-queries, which would multiply
            // the outer rows and produce duplicates.
            Subquery<Long> parentIdSubquery = query.subquery(Long.class);
            Root<ConnectionFee> childRoot = parentIdSubquery.from(ConnectionFee.class);
            List<Predicate> childPredicates = buildFilterPredicates(childRoot, criteriaBuilder, filters);
            // Only real children (those that do have a parent)
            childPredicates.add(criteriaBuilder.isNotNull(childRoot.get("parent")));
            parentIdSubquery
                    .select(childRoot.get("parent").get("id"))   // → SELECT c.parent_id
                    .where(criteriaBuilder.and(childPredicates.toArray(new Predicate[0])));

            // Parent is a hit if its own ID appears in the set of parent_ids from matching children
            Predicate hasMatchingChild = root.get("id").in(parentIdSubquery);

            // A top-level row is included when either the parent or a child matches
            return criteriaBuilder.and(isParent, criteriaBuilder.or(parentMatches, hasMatchingChild));
        };
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Builds filter predicates for the given root WITHOUT adding a parent IS NULL
     * constraint.  Shared by both getSpecifications and getSpecificationsIncludingChildren.
     */
    private static List<Predicate> buildFilterPredicates(
            Root<ConnectionFee> root,
            CriteriaBuilder criteriaBuilder,
            Map<String, Object> filters) {

        List<Predicate> predicates = new ArrayList<>();

        if (!filters.containsKey("status")) {
            predicates.add(criteriaBuilder.notEqual(root.get("status"), Status.SOFT_DELETED));
        }

        filters.forEach((key, value) -> {
            if (value != null) {
                switch (key) {
                    case "id":
                        predicates.add(criteriaBuilder.equal(root.get("id"), value));
                        break;
                    case "orderStatus":
                        predicates.add(criteriaBuilder.equal(root.get("orderStatus"),
                                OrderStatus.valueOf(value.toString().toUpperCase())));
                        break;
                    case "status":
                        predicates.add(criteriaBuilder.equal(root.get("status"),
                                Status.valueOf(value.toString().toUpperCase())));
                        break;
                    case "orderN":
                        if (value instanceof List<?>) {
                            List<Predicate> likePredicates = new ArrayList<>();
                            for (String type : (List<String>) value) {
                                likePredicates.add(criteriaBuilder.like(root.get("orderN"), "%" + type + "%"));
                            }
                            predicates.add(criteriaBuilder.or(likePredicates.toArray(new Predicate[0])));
                        } else {
                            predicates.add(criteriaBuilder.like(root.get("withdrawType"), "%" + value + "%"));
                        }
                        break;
                    case "region":
                        predicates.add(criteriaBuilder.like(root.get("region"), "%" + value + "%"));
                        break;
                    case "serviceCenter":
                        predicates.add(criteriaBuilder.like(root.get("serviceCenter"), "%" + value + "%"));
                        break;
                    case "projectID":
                        predicates.add(criteriaBuilder.like(root.get("projectID"), "%" + value + "%"));
                        break;
                    case "withdrawType":
                        if (value instanceof List<?>) {
                            List<Predicate> withdrawTypePredicates = new ArrayList<>();
                            boolean containsNull = false;
                            for (String type : (List<String>) value) {
                                if ("null".equalsIgnoreCase(type)) {
                                    containsNull = true;
                                } else {
                                    withdrawTypePredicates.add(
                                            criteriaBuilder.like(root.get("withdrawType"), type));
                                }
                            }
                            if (containsNull) {
                                withdrawTypePredicates.add(criteriaBuilder.isNull(root.get("withdrawType")));
                            }
                            if (!withdrawTypePredicates.isEmpty()) {
                                predicates.add(criteriaBuilder.or(
                                        withdrawTypePredicates.toArray(new Predicate[0])));
                            }
                        } else if (value instanceof String stringValue) {
                            if (stringValue.length() == 1) {
                                predicates.add(criteriaBuilder.like(root.get("withdrawType"),
                                        stringValue + "_%"));
                            } else {
                                predicates.add(criteriaBuilder.like(root.get("withdrawType"),
                                        stringValue + "%"));
                            }
                        }
                        break;
                    case "extractionTask":
                        predicates.add(criteriaBuilder.equal(
                                root.get("extractionTask").get("id"), value));
                        break;
                    case "clarificationDateStart":
                        LocalDateTime clarificationDateStart = parseToLocalDateTime(value.toString());
                        if (clarificationDateStart != null) {
                            predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                    root.get("clarificationDate"), clarificationDateStart));
                        }
                        break;
                    case "clarificationDateEnd":
                        LocalDateTime clarificationDateEnd = parseToLocalDateTime(value.toString());
                        if (clarificationDateEnd != null) {
                            predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                    root.get("clarificationDate"), clarificationDateEnd));
                        }
                        break;
                    case "changeDateStart":
                        LocalDateTime changeDateStart = parseToLocalDateTime(value.toString());
                        if (changeDateStart != null) {
                            predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                    root.get("changeDate"), changeDateStart));
                        }
                        break;
                    case "changeDateEnd":
                        LocalDateTime changeDateEnd = parseToLocalDateTime(value.toString());
                        if (changeDateEnd != null) {
                            predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                    root.get("changeDate"), changeDateEnd));
                        }
                        break;
                    case "transferDateStart":
                        LocalDateTime transferDateStart = parseToLocalDateTime(value.toString());
                        if (transferDateStart != null) {
                            predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                    root.get("transferDate"), transferDateStart));
                        }
                        break;
                    case "transferDateEnd":
                        LocalDateTime transferDateEnd = parseToLocalDateTime(value.toString());
                        if (transferDateEnd != null) {
                            predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                    root.get("transferDate"), transferDateEnd));
                        }
                        break;
                    case "extractionId":
                        predicates.add(criteriaBuilder.equal(root.get("extractionId"), value));
                        break;
                    case "note":
                        predicates.add(criteriaBuilder.like(root.get("note"), "%" + value + "%"));
                        break;
                    case "extractionDateStart":
                        LocalDate extractionDateStart = LocalDate.parse(value.toString());
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                root.get("extractionDate"), extractionDateStart));
                        break;
                    case "extractionDateEnd":
                        LocalDate extractionDateEnd = LocalDate.parse(value.toString());
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                root.get("extractionDate"), extractionDateEnd));
                        break;
                    case "totalAmountStart":
                        Double totalAmountStart = Double.parseDouble(value.toString());
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                root.get("totalAmount"), totalAmountStart));
                        break;
                    case "totalAmountEnd":
                        Double totalAmountEnd = Double.parseDouble(value.toString());
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                root.get("totalAmount"), totalAmountEnd));
                        break;
                    case "purpose":
                        predicates.add(criteriaBuilder.like(root.get("purpose"), "%" + value + "%"));
                        break;
                    case "description":
                        predicates.add(criteriaBuilder.like(root.get("description"), "%" + value + "%"));
                        break;
                    case "tax":
                        predicates.add(criteriaBuilder.like(root.get("tax"), "%" + value + "%"));
                        break;
                    case "change_person":
                        predicates.add(criteriaBuilder.equal(root.get("changePerson").get("id"), value));
                        break;
                    case "file":
                        Join<ConnectionFee, ExtractionTask> extractionTaskJoin =
                                root.join("extractionTask");
                        predicates.add(criteriaBuilder.like(
                                extractionTaskJoin.get("fileName"), "%" + value + "%"));
                        break;
                    case "history":
                        predicates.add(criteriaBuilder.equal(root.get("historyId"), value));
                        break;
                    case "download":
                        log.debug("Applying download filter to exclude REMINDER status records (by {})",
                                getCurrentUsername());
                        predicates.add(criteriaBuilder.notEqual(root.get("status"), Status.REMINDER));
                        break;
                }
            }
        });

        return predicates;
    }

    private static LocalDateTime parseToLocalDateTime(String dateTimeStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
        try {
            return LocalDateTime.parse(dateTimeStr, formatter);
        } catch (DateTimeParseException e) {
            throw new DateTimeParseException(e.getMessage(), dateTimeStr, 0);
        }
    }
}
