package com.example.mssqll.specifications;

import com.example.mssqll.models.Extraction;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.example.mssqll.utiles.SecurityUtils.getCurrentUsername;

@Slf4j
public class ExcelSpecification {
    public static Specification<Extraction> getSpecifications(Map<String, Object> filters) {
        log.debug("Building specifications with {} filters (by {})", filters != null ? filters.size() : 0, getCurrentUsername());
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            filters.forEach((key, value) -> {
                log.debug("Processing filter: {} = {} (by {})", key, value, getCurrentUsername());
                if (value != null) {
                    switch (key) {
                        case "fileId":
                            Join<Object, Object> extractionTaskJoin = root.join("extractionTask");
                            predicates.add(criteriaBuilder.equal(extractionTaskJoin.get("id"), value));
                            break;
                        case "startDate":
                            LocalDate dateStart = parseToLocalDate(value.toString());
                            if (dateStart != null) {
                                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("date"), dateStart));
                            }
                            break;
                        case "endDate":
                            LocalDate dateEnd = parseToLocalDate(value.toString());
                            if (dateEnd != null) {
                                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("date"), dateEnd));
                            }
                            break;
                        case "totalAmountStart":
                            Double totalAmountStart = Double.parseDouble(value.toString());
                            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("totalAmount"), totalAmountStart));
                            break;
                        case "totalAmountEnd":
                            Double totalAmountEnd = Double.parseDouble(value.toString());
                            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("totalAmount"), totalAmountEnd));
                            break;
                        case "purpose":
                            predicates.add(criteriaBuilder.like(root.get("purpose"), "%" + value + "%"));
                            break;
                        case "tax":
                            predicates.add(criteriaBuilder.like(root.get("tax"),  value + "%"));
                            break;
                        case "description":
                            predicates.add(criteriaBuilder.like(root.get("description"), "%" + value + "%"));
                            break;
                        case "status":
                            predicates.add(criteriaBuilder.equal(root.get("status"), value));
                            break;
                    }
                }
            });
            log.debug("Built {} predicates for query (by {})", predicates.size(), getCurrentUsername());
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static LocalDate parseToLocalDate(String dateStr) {
        log.debug("Parsing date string: {} (by {})", dateStr, getCurrentUsername());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-M-d");
        try {
            LocalDate date = LocalDate.parse(dateStr, formatter);
            log.debug("Successfully parsed date: {} (by {})", date, getCurrentUsername());
            return date;
        } catch (DateTimeParseException e) {
            log.error("Failed to parse date string '{}': {} (by {})", dateStr, e.getMessage(), getCurrentUsername());
            throw new DateTimeParseException(e.getMessage(), dateStr, 0);
        }
    }
}
