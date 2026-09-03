package com.example.mssqll.controller;

import com.example.mssqll.dto.request.ConnectionFeeUpdateRequestDto;
import com.example.mssqll.dto.response.ConnectionFeeChildrenDTO;
import com.example.mssqll.models.ConnectionFee;
import com.example.mssqll.service.ConnectionFeeService;
import com.example.mssqll.utiles.exceptions.DivideException;
import com.example.mssqll.utiles.exceptions.ResourceNotFoundException;
import com.example.mssqll.utiles.exceptions.UserIsDeletedException;
import com.example.mssqll.utiles.resonse.ApiResponse;
import com.example.mssqll.models.OrderStatus;
import com.example.mssqll.models.Status;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.example.mssqll.utiles.SecurityUtils.getCurrentUsername;
import org.apache.poi.util.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/connection-fees")
@RequiredArgsConstructor
@Tag(name = "Connection Fees", description = "CRUD and filtering operations for connection fees")
public class ConnectionFeeController {

    private final ConnectionFeeService connectionFeeService;

    @Value("${upload.directory}")
    private String uploadDirectory;

    @Operation(summary = "Get paginated list of connection fees")
    @GetMapping
    public ResponseEntity<PagedModel<ConnectionFee>> getExtractions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching connection fees - page: {}, size: {} (requested by {})", page, size, getCurrentUsername());
        int adjustedPage = (page < 1) ? 0 : page - 1;
        PagedModel<ConnectionFee> fees = connectionFeeService.getAllFee(adjustedPage, size);
        return ResponseEntity.ok().body(fees);
    }

    @Operation(summary = "Test exception endpoint", description = "Always throws an exception — used for error-handling tests")
    @GetMapping("/exception")
    public String throwException() {
        log.warn("Test exception endpoint called (requested by {})", getCurrentUsername());
        try {
            throw new AccessDeniedException("This is a test exception!");
        } catch (Exception e) {
            log.error("Test exception thrown: {} (by {})", e.getMessage(), getCurrentUsername());
            throw new UserIsDeletedException(e.getMessage());
        }
    }
    @Operation(summary = "Get a single connection fee by ID")
    @GetMapping("/{id}")
    public ApiResponse<ConnectionFee> getConnectionFee(@PathVariable Long id) {
        log.info("Fetching connection fee by id: {} (requested by {})", id, getCurrentUsername());
        Optional<ConnectionFee> fee = connectionFeeService.getFee(id);
        if (fee.isPresent()) {
            return ApiResponse.<ConnectionFee>builder()
                    .success(true)
                    .message("Operation successful")
                    .data(fee.get())
                    .build();
        } else {
            log.warn("Connection fee not found for id: {} (by {})", id, getCurrentUsername());
            return ApiResponse.<ConnectionFee>builder()
                    .success(false)
                    .message("Connection Fee not found")
                    .build();
        }
    }

    @Operation(summary = "Create connection fees from an extraction task (ADMIN/MANAGER)")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping("/{extractionTaskId}")
    public ResponseEntity<List<ConnectionFee>> createConnectionFee(@PathVariable Long extractionTaskId) {
        log.info("Creating connection fees for extraction task id: {} (requested by {})", extractionTaskId, getCurrentUsername());
        List<ConnectionFee> createdConnectionFee = connectionFeeService.saveFee(extractionTaskId);
        log.info("Successfully created {} connection fees for task id: {} (requested by {})", createdConnectionFee.size(), extractionTaskId, getCurrentUsername());
        return ResponseEntity.ok().body(createdConnectionFee);
    }

    @Operation(summary = "Create a single connection fee (ADMIN/MANAGER)")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping()
    public ResponseEntity<ConnectionFee> createConnectionFee(@RequestBody ConnectionFee connectionFee) {
        log.info("Creating single connection fee (requested by {})", getCurrentUsername());
        ConnectionFee fee = connectionFeeService.save(connectionFee);
        log.info("Successfully created connection fee with id: {} (requested by {})", fee.getId(), getCurrentUsername());
        return ResponseEntity.ok().body(fee);
    }

    @Operation(summary = "Update a connection fee (ADMIN/MANAGER/OPERATOR)")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
    @PutMapping("/{connectionFeeId}")
    public ResponseEntity<ConnectionFee> updateConnectionFee(
            @PathVariable Long connectionFeeId,
            @RequestBody ConnectionFeeUpdateRequestDto connectionFeeDetails) {
        ConnectionFee updatedConnectionFee = connectionFeeService.updateFee(connectionFeeId, connectionFeeDetails);
        log.info("Successfully updated connection fee with id: {} (requested by {})", connectionFeeId, getCurrentUsername());
        return ResponseEntity.ok().body(updatedConnectionFee);
    }

    @Operation(summary = "Filter and paginate connection fees")
    @Parameters({
        @Parameter(name = "orderStatus",         in = ParameterIn.QUERY, schema = @Schema(implementation = OrderStatus.class)),
        @Parameter(name = "status",              in = ParameterIn.QUERY, schema = @Schema(implementation = Status.class)),
        @Parameter(name = "region",              in = ParameterIn.QUERY, schema = @Schema(type = "string")),
        @Parameter(name = "serviceCenter",       in = ParameterIn.QUERY, schema = @Schema(type = "string")),
        @Parameter(name = "projectID",           in = ParameterIn.QUERY, schema = @Schema(type = "string")),
        @Parameter(name = "note",                in = ParameterIn.QUERY, schema = @Schema(type = "string")),
        @Parameter(name = "purpose",             in = ParameterIn.QUERY, schema = @Schema(type = "string")),
        @Parameter(name = "description",         in = ParameterIn.QUERY, schema = @Schema(type = "string")),
        @Parameter(name = "tax",                 in = ParameterIn.QUERY, schema = @Schema(type = "string")),
        @Parameter(name = "file",                in = ParameterIn.QUERY, schema = @Schema(type = "string")),
        @Parameter(name = "extractionTask",      in = ParameterIn.QUERY, schema = @Schema(type = "integer")),
        @Parameter(name = "extractionId",        in = ParameterIn.QUERY, schema = @Schema(type = "integer")),
        @Parameter(name = "history",             in = ParameterIn.QUERY, schema = @Schema(type = "integer")),
        @Parameter(name = "id",                  in = ParameterIn.QUERY, schema = @Schema(type = "integer")),
        @Parameter(name = "change_person",       in = ParameterIn.QUERY, schema = @Schema(type = "integer")),
        @Parameter(name = "transferDateStart",   in = ParameterIn.QUERY, schema = @Schema(type = "string", example = "2024-01-01 00:00:00.000000")),
        @Parameter(name = "transferDateEnd",     in = ParameterIn.QUERY, schema = @Schema(type = "string", example = "2024-12-31 23:59:59.999999")),
        @Parameter(name = "clarificationDateStart", in = ParameterIn.QUERY, schema = @Schema(type = "string", example = "2024-01-01 00:00:00.000000")),
        @Parameter(name = "clarificationDateEnd",   in = ParameterIn.QUERY, schema = @Schema(type = "string", example = "2024-12-31 23:59:59.999999")),
        @Parameter(name = "changeDateStart",     in = ParameterIn.QUERY, schema = @Schema(type = "string", example = "2024-01-01 00:00:00.000000")),
        @Parameter(name = "changeDateEnd",       in = ParameterIn.QUERY, schema = @Schema(type = "string", example = "2024-12-31 23:59:59.999999")),
        @Parameter(name = "extractionDateStart", in = ParameterIn.QUERY, schema = @Schema(type = "string", format = "date", example = "2024-01-01")),
        @Parameter(name = "extractionDateEnd",   in = ParameterIn.QUERY, schema = @Schema(type = "string", format = "date", example = "2024-12-31")),
        @Parameter(name = "totalAmountStart",    in = ParameterIn.QUERY, schema = @Schema(type = "number")),
        @Parameter(name = "totalAmountEnd",      in = ParameterIn.QUERY, schema = @Schema(type = "number")),
        @Parameter(name = "sortBy",              in = ParameterIn.QUERY, schema = @Schema(type = "string",
            allowableValues = {"transferDate", "clarificationDate", "changeDate", "extractionDate", "totalAmount", "id"},
            defaultValue = "transferDate")),
        @Parameter(name = "sortDir",             in = ParameterIn.QUERY, schema = @Schema(type = "string",
            allowableValues = {"ASC", "DESC"}, defaultValue = "DESC")),
    })
    @GetMapping("/filter")
    public ResponseEntity<PagedModel<?>> filterConnectionFees(
            @Parameter(hidden = true) @RequestParam Map<String, String> filters,
            @RequestParam(required = false) List<String> withdrawType,
            @RequestParam(required = false) List<String> orderN,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(hidden = true) @RequestParam(defaultValue = "transferDate") String sortBy,
            @Parameter(hidden = true) @RequestParam(defaultValue = "DESC") String sortDir) {
        log.info("Filtering connection fees - page: {}, size: {}, sortBy: {}, sortDir: {}, filters: {} (requested by {})",
                page, size, sortBy, sortDir, filters.keySet(), getCurrentUsername());

        int adjustedPage = (page < 1) ? 0 : page - 1;

        Map<String, Object> updatedFilters = new HashMap<>(filters);
        if (withdrawType != null) {
            log.info("withdrawType values: {}", withdrawType);
            updatedFilters.put("withdrawType", withdrawType);
        }
        if (orderN != null) {
            updatedFilters.put("orderN", orderN);
        }

        PagedModel<?> resPage = connectionFeeService.letDoFilterCustom(updatedFilters, adjustedPage, size, sortBy, sortDir);
        return ResponseEntity.ok().body(resPage);
    }

    @Operation(summary = "Delete all connection fees for an extraction task (ADMIN/MANAGER/OPERATOR)")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
    @DeleteMapping("/delete-by-task/{extractionTaskId}")
    public ResponseEntity<?> deleteConnectionFeeByTaskId(@PathVariable Long extractionTaskId) {
        log.info("Deleting connection fees for extraction task id: {} (requested by {})", extractionTaskId, getCurrentUsername());
        connectionFeeService.deleteByTaskId(extractionTaskId);
        log.info("Successfully deleted connection fees for task id: {} (requested by {})", extractionTaskId, getCurrentUsername());
        return ResponseEntity.ok().body(Collections.singletonMap("message", "Connection fee deleted successfully"));
    }

    @Operation(summary = "Soft-delete a connection fee (ADMIN/MANAGER/OPERATOR)")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @DeleteMapping("/soft-delete/{fee}")
    public ResponseEntity<?> softDeleteConnectionFee(@PathVariable Long fee) {
        log.info("Soft deleting connection fee with id: {} (requested by {})", fee, getCurrentUsername());
        Optional<ConnectionFee> optionalConnectionFee = connectionFeeService.findById(fee);
        if (optionalConnectionFee.isPresent()) {
            connectionFeeService.softDeleteById(fee);
            log.info("Successfully soft deleted connection fee with id: {} (requested by {})", fee, getCurrentUsername());
            return ResponseEntity.ok().body(Collections.singletonMap("message", "Connection fee deleted successfully"));
        } else {
            log.warn("Connection fee not found for soft delete, id: {} (by {})", fee, getCurrentUsername());
            return ResponseEntity.ok().body(Collections.singletonMap("message", "Connection fee not found"));
        }
    }

    @Operation(summary = "Download filtered connection fees as CSV (ADMIN/MANAGER/OPERATOR)")
    @Parameters({
        @Parameter(name = "orderStatus",         in = ParameterIn.QUERY, schema = @Schema(implementation = OrderStatus.class)),
        @Parameter(name = "status",              in = ParameterIn.QUERY, schema = @Schema(implementation = Status.class)),
        @Parameter(name = "region",              in = ParameterIn.QUERY, schema = @Schema(type = "string")),
        @Parameter(name = "serviceCenter",       in = ParameterIn.QUERY, schema = @Schema(type = "string")),
        @Parameter(name = "projectID",           in = ParameterIn.QUERY, schema = @Schema(type = "string")),
        @Parameter(name = "note",                in = ParameterIn.QUERY, schema = @Schema(type = "string")),
        @Parameter(name = "purpose",             in = ParameterIn.QUERY, schema = @Schema(type = "string")),
        @Parameter(name = "description",         in = ParameterIn.QUERY, schema = @Schema(type = "string")),
        @Parameter(name = "tax",                 in = ParameterIn.QUERY, schema = @Schema(type = "string")),
        @Parameter(name = "file",                in = ParameterIn.QUERY, schema = @Schema(type = "string")),
        @Parameter(name = "transferDateStart",   in = ParameterIn.QUERY, schema = @Schema(type = "string", example = "2024-01-01 00:00:00.000000")),
        @Parameter(name = "transferDateEnd",     in = ParameterIn.QUERY, schema = @Schema(type = "string", example = "2024-12-31 23:59:59.999999")),
        @Parameter(name = "clarificationDateStart", in = ParameterIn.QUERY, schema = @Schema(type = "string", example = "2024-01-01 00:00:00.000000")),
        @Parameter(name = "clarificationDateEnd",   in = ParameterIn.QUERY, schema = @Schema(type = "string", example = "2024-12-31 23:59:59.999999")),
        @Parameter(name = "extractionDateStart", in = ParameterIn.QUERY, schema = @Schema(type = "string", format = "date", example = "2024-01-01")),
        @Parameter(name = "extractionDateEnd",   in = ParameterIn.QUERY, schema = @Schema(type = "string", format = "date", example = "2024-12-31")),
        @Parameter(name = "totalAmountStart",    in = ParameterIn.QUERY, schema = @Schema(type = "number")),
        @Parameter(name = "totalAmountEnd",      in = ParameterIn.QUERY, schema = @Schema(type = "number")),
    })
    @GetMapping("/download")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
    public ResponseEntity<?> downloadExcel(@Parameter(hidden = true) @RequestParam Map<String, String> filters) throws IOException {
        log.info("Downloading connection fees Excel with filters: {} (requested by {})", filters.keySet(), getCurrentUsername());

        Map<String, Object> downloadFilters = new HashMap<>(filters);
        List<ConnectionFee> resti = connectionFeeService.getExportData(downloadFilters);
        log.info("Generating Excel file with {} connection fees (requested by {})", resti.size(), getCurrentUsername());
        ByteArrayInputStream excelStream = connectionFeeService.createExcel(resti);
        HttpHeaders headers = new HttpHeaders();
        String time = LocalDateTime.now(ZoneId.of("Asia/Tbilisi"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        headers.add("Content-Disposition",
                "attachment; filename=" +
                        time +
                        " connection_fees.csv");
        headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Content-Disposition");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new InputStreamResource(excelStream));
    }


    @Operation(summary = "Divide a connection fee into multiple parts (ADMIN/MANAGER/OPERATOR)")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
    @PostMapping("/divide-fee/{id}")
    public ResponseEntity<?> divideFee(@PathVariable Long id, @RequestBody Double[] arr) {
        log.info("Dividing fee with id: {} into {} parts (requested by {})", id, arr.length, getCurrentUsername());
        try {
            connectionFeeService.divideFee(id, arr);
            log.info("Successfully divided fee with id: {} (requested by {})", id, getCurrentUsername());
            return ResponseEntity.ok().body(Collections.singletonMap("message", "Divide Successfully"));
        } catch (Exception e) {
            log.error("Error dividing fee with id: {}, error: {} (by {})", id, e.getMessage(), getCurrentUsername());
            throw new DivideException(e.getMessage());
        }
    }

    @Operation(summary = "Get child connection fees of a parent fee")
    @GetMapping("/find-by-parent/{id}")
    public ResponseEntity<List<ConnectionFeeChildrenDTO>> findByParent(@PathVariable Long id) {
        log.info("Finding connection fees by parent id: {} (requested by {})", id, getCurrentUsername());
        try {
            return ResponseEntity.ok().body(
                    connectionFeeService.getFeesByParent(id)
            );
        } catch (Exception e) {
            log.error("Error finding fees by parent id: {}, error: {} (by {})", id, e.getMessage(), getCurrentUsername());
            return ResponseEntity.noContent().build();
        }
    }


    @Operation(summary = "Upload historical connection fees from Excel (ADMIN/MANAGER)")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping("/upload-history")
    public ResponseEntity<?> handleFileUpload(@RequestParam("file") MultipartFile file) {
        log.info("Uploading history file: {} (requested by {})", file.getOriginalFilename(), getCurrentUsername());
        Integer count;
        if (file.isEmpty()) {
            log.warn("Upload attempt with empty file (by {})", getCurrentUsername());
            throw new ResourceNotFoundException("Please select a file to upload");
        }
        IOUtils.setByteArrayMaxOverride(143_656_941);
        try {
            count = connectionFeeService.uploadHistory(file);
            log.info("Successfully uploaded history file with {} records (requested by {})", count, getCurrentUsername());
            return ResponseEntity.ok(
                    Map.of(
                            "message", "Successfully uploaded",
                            "count", count
                    ));
        } catch (Exception e) {
            log.error("Failed to upload history file: {}, error: {} (by {})", file.getOriginalFilename(), e.getMessage(), getCurrentUsername());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process the file: " + e.getMessage());
        }
    }

    @Operation(summary = "Get connection fees by tax payer ID")
    @GetMapping("/by-taxpayer")
    public ResponseEntity<PagedModel<?>> getByTaxPayerId(
            @RequestParam String taxPayerId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching connection fees by tax payer id: {} - page: {}, size: {} (requested by {})", taxPayerId, page, size, getCurrentUsername());
        int adjustedPage = (page < 1) ? 0 : page - 1;
        PagedModel<?> fees = connectionFeeService.getByTaxPayerId(taxPayerId, adjustedPage, size);
        return ResponseEntity.ok().body(fees);
    }

    @Operation(summary = "Download an uploaded file by name (ADMIN/MANAGER/OPERATOR)")
    @GetMapping("/download-ext")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
    public ResponseEntity<?> downloadFile(@RequestParam String fileName) {
        log.info("Downloading external file: {} (requested by {})", fileName, getCurrentUsername());

        try {
            Path storageRoot = Paths.get(uploadDirectory).toAbsolutePath().normalize();

            String decodedFileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);

            boolean hasControlChars = decodedFileName.chars().anyMatch(c -> c < 32 || c == 127);
            if (hasControlChars) {
                log.warn("Rejected fileName with invalid control characters: {} (by {})", decodedFileName, getCurrentUsername());
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("Invalid file name.");
            }

            if (decodedFileName.contains("..") ||
                    decodedFileName.contains("\\") ||
                    decodedFileName.startsWith("/") ||
                    decodedFileName.startsWith("\\")) {

                log.warn("Rejected suspicious file path: {} (by {})", decodedFileName, getCurrentUsername());
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("Suspicious file path: " + decodedFileName);
            }

            Path targetPath = storageRoot.resolve(decodedFileName).normalize();

            if (!targetPath.startsWith(storageRoot)) {
                log.warn("Path traversal attempt detected: {} -> {} (by {})", decodedFileName, targetPath, getCurrentUsername());
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body("Forbidden: Path traversal attempt detected");
            }

            File file = targetPath.toFile();

            if (!file.exists() || !file.isFile()) {
                log.warn("File not found: {} (by {})", decodedFileName, getCurrentUsername());

                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body("File not found: " + decodedFileName);
            }

            Resource resource = new FileSystemResource(file);

            String encodedFileName = URLEncoder.encode(decodedFileName, StandardCharsets.UTF_8)
                    .replace("+", "%20");

            log.info("File prepared for download: {} (requested by {})", decodedFileName, getCurrentUsername());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (Exception e) {
            log.error("Error downloading file: {}, error: {} (by {})", fileName, e.getMessage(), getCurrentUsername());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}