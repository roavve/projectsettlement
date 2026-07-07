package com.example.mssqll.service.impl;

import com.example.mssqll.dto.response.BusinessUnitResponseDto;
import com.example.mssqll.models.BusinessUnit;
import com.example.mssqll.repository.BusinessUnitRepository;
import com.example.mssqll.service.BusinessUnitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.example.mssqll.utiles.SecurityUtils.getCurrentUsername;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BusinessUnitServiceImpl implements BusinessUnitService {

    @Autowired
    private BusinessUnitRepository businessUnitRepository;

    // Save a business unit
    public BusinessUnit save(BusinessUnit businessUnit) {
        log.info("Saving business unit with name: {} (by {})", businessUnit.getName(), getCurrentUsername());
        log.debug("Business unit details - UnitNumber: {}, UnitTypeKey: {}, Parent: {} (by {})",
                  businessUnit.getUnitNumber(), businessUnit.getUnitTypeKey(),
                  businessUnit.getParent() != null ? businessUnit.getParent().getId() : null, getCurrentUsername());

        BusinessUnit savedUnit = businessUnitRepository.save(businessUnit);
        log.info("Successfully saved business unit with ID: {} (by {})", savedUnit.getId(), getCurrentUsername());
        return savedUnit;
    }

    // Find a business unit by ID
    public Optional<BusinessUnit> findById(Long id) {
        log.info("Finding business unit by ID: {} (by {})", id, getCurrentUsername());
        Optional<BusinessUnit> businessUnit = businessUnitRepository.findById(id);

        if (businessUnit.isPresent()) {
            log.debug("Found business unit with ID: {}, Name: {} (by {})", id, businessUnit.get().getName(), getCurrentUsername());
        } else {
            log.warn("Business unit not found with ID: {} (by {})", id, getCurrentUsername());
        }

        return businessUnit;
    }

    // Get all business units
    public List<BusinessUnit> findAll() {
        log.info("Retrieving all business units (by {})", getCurrentUsername());
        List<BusinessUnit> businessUnits = businessUnitRepository.findAll();
        log.info("Retrieved {} business units (by {})", businessUnits.size(), getCurrentUsername());
        return businessUnits;
    }

    // Delete a business unit by ID
    public void deleteById(Long id) {
        log.info("Deleting business unit with ID: {} (by {})", id, getCurrentUsername());
        businessUnitRepository.deleteById(id);
        log.info("Successfully deleted business unit with ID: {} (by {})", id, getCurrentUsername());
    }

    // Convert BusinessUnit to BusinessUnitResponseDto
    public BusinessUnitResponseDto convertToDto(BusinessUnit businessUnit) {
        log.debug("Converting business unit to DTO - ID: {}, Name: {} (by {})", businessUnit.getId(), businessUnit.getName(), getCurrentUsername());
        BusinessUnitResponseDto dto = new BusinessUnitResponseDto();
        dto.setId(businessUnit.getId());
        dto.setUnitNumber(businessUnit.getUnitNumber());
        dto.setName(businessUnit.getName());
        dto.setUnitTypeKey(businessUnit.getUnitTypeKey());
        dto.setParent(businessUnit.getParent()); // Include the entire parent
        return dto;
    }

    @Override
    public List<BusinessUnit> getBusinessUnitsByParent(BusinessUnit parent) {
        log.info("Retrieving business units by parent ID: {} (by {})", parent != null ? parent.getId() : null, getCurrentUsername());
        List<BusinessUnit> businessUnits = businessUnitRepository.findByParent(parent);
        log.info("Found {} business units for parent ID: {} (by {})", businessUnits.size(), parent != null ? parent.getId() : null, getCurrentUsername());
        return businessUnits;
    }

    @Override
    public List<BusinessUnit> getRootBusinessUnits() {
        log.info("Retrieving root business units (no parent) (by {})", getCurrentUsername());
        List<BusinessUnit> rootUnits = businessUnitRepository.findByParentIsNull();
        log.info("Found {} root business units (by {})", rootUnits.size(), getCurrentUsername());
        return rootUnits;
    }

    @Override
    public List<?> getBykey(Integer key) {
        log.info("Retrieving business units by unit type key: {} (by {})", key, getCurrentUsername());
        List<?> businessUnits = businessUnitRepository.findByUnitTypeKey(key);
        log.info("Found {} business units for unit type key: {} (by {})", businessUnits.size(), key, getCurrentUsername());
        return businessUnits;
    }

    public List<BusinessUnitResponseDto> convertToDtoList(List<BusinessUnit> businessUnits) {
        log.debug("Converting {} business units to DTO list (by {})", businessUnits.size(), getCurrentUsername());
        return businessUnits.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

}
