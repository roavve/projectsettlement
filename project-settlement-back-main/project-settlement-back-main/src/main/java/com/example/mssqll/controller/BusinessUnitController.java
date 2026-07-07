package com.example.mssqll.controller;


import com.example.mssqll.dto.response.BusinessUnitResponseDto;
import com.example.mssqll.models.BusinessUnit;
import com.example.mssqll.service.BusinessUnitService;
import com.example.mssqll.utiles.resonse.ApiResponseUnit;
import lombok.extern.slf4j.Slf4j;

import static com.example.mssqll.utiles.SecurityUtils.getCurrentUsername;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/business-units")
public class BusinessUnitController {

    @Autowired
    private BusinessUnitService businessUnitService;

    @PostMapping
    public ApiResponseUnit<BusinessUnitResponseDto> createBusinessUnit(@RequestBody BusinessUnit businessUnit) {
        log.info("Creating new business unit (requested by {})", getCurrentUsername());
        BusinessUnit savedUnit = businessUnitService.save(businessUnit);
        BusinessUnitResponseDto dto = businessUnitService.convertToDto(savedUnit);
        log.info("Successfully created business unit with id: {} (requested by {})", savedUnit.getId(), getCurrentUsername());
        return ApiResponseUnit.<BusinessUnitResponseDto>builder()
                .success(true)
                .message("Business Unit created successfully")
                .data(dto)
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponseUnit<BusinessUnitResponseDto> getBusinessUnitById(@PathVariable Long id) {
        log.info("Fetching business unit by id: {} (requested by {})", id, getCurrentUsername());
        Optional<BusinessUnit> businessUnit = businessUnitService.findById(id);
        if (businessUnit.isPresent()) {
            BusinessUnitResponseDto dto = businessUnitService.convertToDto(businessUnit.get());
            return ApiResponseUnit.<BusinessUnitResponseDto>builder()
                    .success(true)
                    .message("Business Unit found")
                    .data(dto)
                    .build();
        } else {
            log.warn("Business unit not found for id: {} (by {})", id, getCurrentUsername());
            return ApiResponseUnit.<BusinessUnitResponseDto>builder()
                    .success(false)
                    .message("Business Unit not found")
                    .build();
        }
    }

    @GetMapping
    public ApiResponseUnit<List<BusinessUnitResponseDto>> getAllBusinessUnits() {
        log.info("Fetching all business units (requested by {})", getCurrentUsername());
        List<BusinessUnit> businessUnits = businessUnitService.findAll();
        List<BusinessUnitResponseDto> dtoList = businessUnitService.convertToDtoList(businessUnits);
        log.info("Retrieved {} business units (requested by {})", dtoList.size(), getCurrentUsername());
        return ApiResponseUnit.<List<BusinessUnitResponseDto>>builder()
                .success(true)
                .message("Business Units retrieved successfully")
                .data(dtoList)
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponseUnit<Void> deleteBusinessUnit(@PathVariable Long id) {
        log.info("Deleting business unit with id: {} (requested by {})", id, getCurrentUsername());
        businessUnitService.deleteById(id);
        log.info("Successfully deleted business unit with id: {} (requested by {})", id, getCurrentUsername());
        return ApiResponseUnit.<Void>builder()
                .success(true)
                .message("Business Unit deleted successfully")
                .build();
    }

    @GetMapping("/by-parent/{parentId}")
    public ApiResponseUnit<List<BusinessUnit>> getBusinessUnitsByParent(@PathVariable Long parentId) {
        log.info("Fetching business units by parent id: {} (requested by {})", parentId, getCurrentUsername());
        BusinessUnit parent = new BusinessUnit();
        parent.setId(parentId);
        List<BusinessUnit> units = businessUnitService.getBusinessUnitsByParent(parent);
        log.info("Found {} business units for parent id: {} (requested by {})", units.size(), parentId, getCurrentUsername());
        return ApiResponseUnit.<List<BusinessUnit>>builder()
                .success(true)
                .message("Business Units fetched successfully")
                .data(units)
                .build();
    }

    @GetMapping("/roots")
    public ApiResponseUnit<List<BusinessUnit>> getRootBusinessUnits() {
        log.info("Fetching root business units (requested by {})", getCurrentUsername());
        List<BusinessUnit> rootUnits = businessUnitService.getRootBusinessUnits();
        log.info("Found {} root business units (requested by {})", rootUnits.size(), getCurrentUsername());
        return ApiResponseUnit.<List<BusinessUnit>>builder()
                .success(true)
                .message("Root Business Units fetched successfully")
                .data(rootUnits)
                .build();
    }
    @GetMapping("/unit-key/{unit_type_key}")
    public ResponseEntity<?> getByKey(@PathVariable Integer unit_type_key) {
        log.info("Fetching business units by unit type key: {} (requested by {})", unit_type_key, getCurrentUsername());
        return ResponseEntity.ok().body(businessUnitService.getBykey(unit_type_key));
    }

}