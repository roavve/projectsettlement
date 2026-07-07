package com.example.mssqll.repository;

import com.example.mssqll.models.ClarificationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClarificationRuleRepository extends JpaRepository<ClarificationRule, Long> {

    /** The single rule currently driving the clarificationDate logic. */
    Optional<ClarificationRule> findFirstByActiveTrueOrderByUpdatedAtDesc();
}
