package com.example.mssqll.repository;

import com.example.mssqll.models.ClarificationFact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClarificationFactRepository extends JpaRepository<ClarificationFact, Long> {

    List<ClarificationFact> findAllByActiveTrue();

    Optional<ClarificationFact> findByName(String name);

    boolean existsByName(String name);
}
