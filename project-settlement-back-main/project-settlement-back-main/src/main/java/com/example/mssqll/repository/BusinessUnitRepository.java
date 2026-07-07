package com.example.mssqll.repository;

import com.example.mssqll.models.BusinessUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusinessUnitRepository extends JpaRepository<BusinessUnit, Long> {
    List<BusinessUnit> findByParent(BusinessUnit parent);

    List<BusinessUnit> findByParentIsNull();

    List<BusinessUnit> findByUnitTypeKey(int unitTypeKey);

}