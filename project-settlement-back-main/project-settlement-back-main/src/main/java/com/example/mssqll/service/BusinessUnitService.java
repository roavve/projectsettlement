package com.example.mssqll.service;

import com.example.mssqll.dto.response.BusinessUnitResponseDto;
import com.example.mssqll.models.BusinessUnit;

import java.util.List;
import java.util.Optional;

public interface BusinessUnitService {
    BusinessUnit save(BusinessUnit businessUnit);

    Optional<BusinessUnit> findById(Long id);

    List<BusinessUnit> findAll();

    void deleteById(Long id);

    List<BusinessUnitResponseDto> convertToDtoList(List<BusinessUnit> businessUnits);

    BusinessUnitResponseDto convertToDto(BusinessUnit businessUnit);

    List<BusinessUnit> getBusinessUnitsByParent(BusinessUnit parent);

    List<BusinessUnit> getRootBusinessUnits();

    List<?> getBykey(Integer key);
}