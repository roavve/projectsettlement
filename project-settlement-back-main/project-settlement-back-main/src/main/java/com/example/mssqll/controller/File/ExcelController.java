package com.example.mssqll.controller.File;

import com.example.mssqll.dto.response.ExtractionResponseDto;
import com.example.mssqll.models.Extraction;
import com.example.mssqll.models.ExtractionTask;
import com.example.mssqll.models.Status;
import com.example.mssqll.repository.ExtractionTaskRepository;
import com.example.mssqll.service.ExcelService;
import com.example.mssqll.specifications.ExcelSpecification;
import com.example.mssqll.utiles.exceptions.FileNotSupportedException;
import com.example.mssqll.utiles.exceptions.ResourceNotFoundException;
import com.example.mssqll.utiles.resonse.ApiResponse;
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

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/excels")
@RequiredArgsConstructor
@Tag(name = "Excel / Extractions", description = "Excel file upload and extraction data queries")
public class ExcelController {

    private final ExcelService excelService;
    private final ExtractionTaskRepository extractionTaskRepository;

    @Operation(summary = "Upload an Excel file and create extractions (ADMIN/MANAGER)")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping("/upload")
    public ApiResponse<List<ExtractionResponseDto>> handleFileUpload(@RequestParam("file") MultipartFile file) {

        // --- Basic null / empty checks ---
        if (file == null || file.isEmpty()) {
            log.warn("Empty file upload attempt (by {})", getCurrentUsername());
            throw new FileNotSupportedException("Please select a non-empty Excel (.xlsx) file to upload");
        }

        // --- Size limit (e.g. 10 MB) ---
        long MAX_EXCEL_FILE_SIZE = 10 * 1024 * 1024L;
        if (file.getSize() > MAX_EXCEL_FILE_SIZE) {
            log.warn("File too large: {} bytes (limit: {}) (by {})",
                    file.getSize(), MAX_EXCEL_FILE_SIZE, getCurrentUsername());
            throw new FileNotSupportedException("File is too large. Maximum allowed size is 10 MB");
        }

        // --- Filename checks ---
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            log.warn("Upload with missing filename (by {})", getCurrentUsername());
            throw new FileNotSupportedException("Invalid file name");
        }

        // Normalize a path and remove oddities
        String cleanedFilename = org.springframework.util.StringUtils.cleanPath(originalFilename);

        // Block obvious path traversal and separators
        if (cleanedFilename.contains("..") ||
                cleanedFilename.contains("/") ||
                cleanedFilename.contains("\\")) {

            log.warn("Rejected suspicious file path: {} (by {})", cleanedFilename, getCurrentUsername());
            throw new FileNotSupportedException("Invalid file path");
        }

        // Enforce .xlsx extension ONLY
        String lowerName = cleanedFilename.toLowerCase(Locale.ROOT);
        if (!lowerName.endsWith(".xlsx")) {
            log.warn("Rejected non-xlsx extension: {} (by {})", cleanedFilename, getCurrentUsername());
            throw new FileNotSupportedException("Only .xlsx Excel files are allowed");
        }

        // Validate allowed characters in base name
        String baseName = cleanedFilename.substring(0, cleanedFilename.length() - ".xlsx".length());
        if (!baseName.matches("[\\p{L}\\p{N}._\\- ]+")) {
            log.warn("Rejected filename with invalid characters: {} (by {})", cleanedFilename, getCurrentUsername());
            throw new FileNotSupportedException("File name contains invalid characters");
        }

        // length limit for filename base
        if (baseName.length() > 100) {
            log.warn("Rejected filename too long: {} (len={}) (by {})",
                    cleanedFilename, baseName.length(), getCurrentUsername());
            throw new FileNotSupportedException("File name is too long");
        }

        // --- MIME type check (strict) ---
        final String XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        String contentType = file.getContentType();

        if (!XLSX_MIME.equals(contentType)) {
            log.warn("Rejected upload with invalid MIME type: {} for file {} (by {})",
                    contentType, cleanedFilename, getCurrentUsername());
            throw new FileNotSupportedException("Only .xlsx Excel files are supported");
        }

        log.info("Accepted Excel upload: '{}' (size: {} bytes, contentType: {}) (by {})",
                cleanedFilename, file.getSize(), contentType, getCurrentUsername());

        List<ExtractionResponseDto> extractionResponseDtoList = excelService.readExcel(file);

        if (extractionResponseDtoList == null || extractionResponseDtoList.isEmpty()) {
            log.warn("Excel processing returned empty result for file {} (by {})",
                    cleanedFilename, getCurrentUsername());
            throw new ResourceNotFoundException("No data found in uploaded Excel file");
        }

        long warn = extractionResponseDtoList.stream()
                .filter(extractionResponseDto -> extractionResponseDto.getStatus() == Status.WARNING)
                .count();

        long ok = extractionResponseDtoList.size() - warn;

        ExtractionTask extractionTask = extractionResponseDtoList.get(0).getExtractionTask();
        long gt = excelService.getGrandTotal(extractionTask) + warn;

        return new ApiResponse<>(
                true,
                "Operation Successful",
                extractionResponseDtoList,
                warn,
                ok,
                gt,
                excelService.getExtractionCountByExtractionTaskId(extractionTask)
        );
    }

    @Operation(summary = "Get all extractions (paginated)")
    @GetMapping("/extractions")
    public ApiResponse<PagedModel<Extraction>> getExtractions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        int adjustedPage = (page < 1) ? 0 : page - 1;
        Long ok = excelService.getTotalOk();
        Long warn = excelService.getTotalWarning();
        Long total = excelService.getTotalExtractionCount();
        PagedModel<Extraction> extractions = excelService.getAllExtractions(adjustedPage, size);
        Long totalAmount = excelService.getGrandTotal();
        return ApiResponse.<PagedModel<Extraction>>builder()
                .success(true)
                .message("Operation successful")
                .data(extractions)
                .warn(warn)
                .ok(ok)
                .grandTotal(total)
                .countAll(totalAmount)
                .build();
    }

    @Operation(summary = "Get extractions with WARNING status (paginated)")
    @GetMapping("/extractions/warning")
    public ApiResponse<PagedModel<Extraction>> getWarningExtractions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        int adjustedPage = (page < 1) ? 0 : page - 1;
        Long ok = excelService.getTotalOk();
        Long warn = excelService.getTotalWarning();
        PagedModel<Extraction> extractions = excelService.getAllWarningExtractions(adjustedPage, size);
        Long totalAmount = excelService.sumTotalAmountByStatus(Status.WARNING);
        return ApiResponse.<PagedModel<Extraction>>builder()
                .success(true)
                .message("Warned Data")
                .data(extractions)
                .warn(warn)
                .ok(ok)
                .grandTotal(totalAmount)
                .countAll(warn)
                .build();
    }


    @Operation(summary = "Get extractions with OK status (paginated)")
    @GetMapping("/extractions/ok")
    public ApiResponse<PagedModel<Extraction>> getOkExtractions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        int adjustedPage = (page < 1) ? 0 : page - 1;
        Long ok = excelService.getTotalOk();
        Long warn = excelService.getTotalWarning();
        PagedModel<Extraction> extractions = excelService.getAllOkExtractions(adjustedPage, size);
        Long totalAmount = excelService.sumTotalAmountByStatus(Status.GOOD);
        return ApiResponse.<PagedModel<Extraction>>builder()
                .success(true)
                .message("Ok Data")
                .data(extractions)
                .warn(warn)
                .ok(ok)
                .grandTotal(totalAmount)
                .countAll(ok)
                .build();
    }

    @Operation(summary = "Get extractions for a specific file (paginated)")
    @GetMapping("/getExtractionsByFile")
    public ApiResponse<PagedModel<Extraction>> getExtractionByFile(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam Long fileId) {
        int adjustedPage = (page < 1) ? 0 : page - 1;
        PagedModel<Extraction> extractionPage = excelService.getAllExtractionsWithFile(adjustedPage, size, fileId);

        Long warn = excelService.getCountWarningsByFileId(fileId);
        Long ok = excelService.getCountOkByFileId(fileId);
        Long grandTotal = excelService.getTotalAmountByExtractionTaskId(fileId);
        Long total = excelService.getExtractionCountByExtractionTaskId(extractionTaskRepository.getReferenceById(fileId));

        return new ApiResponse<>(
                true,
                "Data retrieved successfully",
                extractionPage,
                warn,
                ok,
                grandTotal,
                total
        );
    }

    @Operation(summary = "Get WARNING extractions for a specific file (paginated)")
    @GetMapping("/getWarningExtractionsByFile")
    public ApiResponse<PagedModel<Extraction>> getWarningExtractionByFile(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam Long fileId) {
        int adjustedPage = (page < 1) ? 0 : page - 1;
        PagedModel<Extraction> extractionsPagedModel = excelService.getWarningByExtractionTask(fileId, adjustedPage, size);
        return ApiResponse.<PagedModel<Extraction>>builder()
                .success(true)
                .message("Ok Data")
                .data(extractionsPagedModel)
                .warn(null)
                .ok(null)
                .grandTotal(excelService.getRepo().sumTotalAmountByExtractionTaskAndStatus(extractionTaskRepository.getReferenceById(fileId), Status.WARNING))
                .countAll(extractionsPagedModel.getMetadata().totalElements())
                .build();
    }

    @Operation(summary = "Get OK extractions for a specific file (paginated)")
    @GetMapping("/getOkExtractionsByFile")
    public ApiResponse<PagedModel<Extraction>> getOkExtractionByFile(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam Long fileId) {
        int adjustedPage = (page < 1) ? 0 : page - 1;
        Long ok = excelService.getTotalOk();
        Long warn = excelService.getTotalWarning();
        PagedModel<Extraction> extractions = excelService.getAllOkExtractionsWithFile(adjustedPage, size, fileId);
        Long totalAmount = excelService.getTotalAmountByExtractionTaskId(fileId);
        return ApiResponse.<PagedModel<Extraction>>builder()
                .success(true)
                .message("Ok Data")
                .data(extractions)
                .warn(warn)
                .ok(ok)
                .grandTotal(totalAmount)
                .countAll(extractions.getMetadata().totalElements())
                .build();
    }

    @Operation(summary = "Delete an extraction task and its data (ADMIN/MANAGER)")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @DeleteMapping("/delete")
    public ApiResponse<ExtractionTask> deleteExtractionTask(@RequestParam Long taskId) {
        excelService.deleteById(taskId);
        return ApiResponse.<ExtractionTask>builder()
                .success(true)
                .message("File Deleted")
                .data(null)
                .warn(null)
                .ok(null)
                .grandTotal(null)
                .countAll(null)
                .build();
    }


    @Operation(summary = "Filter extractions with dynamic criteria")
    @Parameters({
        @Parameter(name = "status",          in = ParameterIn.QUERY, schema = @Schema(implementation = Status.class)),
        @Parameter(name = "fileId",          in = ParameterIn.QUERY, schema = @Schema(type = "integer")),
        @Parameter(name = "purpose",         in = ParameterIn.QUERY, schema = @Schema(type = "string")),
        @Parameter(name = "description",     in = ParameterIn.QUERY, schema = @Schema(type = "string")),
        @Parameter(name = "tax",             in = ParameterIn.QUERY, schema = @Schema(type = "string")),
        @Parameter(name = "startDate",       in = ParameterIn.QUERY, schema = @Schema(type = "string", format = "date", example = "2024-01-01")),
        @Parameter(name = "endDate",         in = ParameterIn.QUERY, schema = @Schema(type = "string", format = "date", example = "2024-12-31")),
        @Parameter(name = "totalAmountStart", in = ParameterIn.QUERY, schema = @Schema(type = "number")),
        @Parameter(name = "totalAmountEnd",  in = ParameterIn.QUERY, schema = @Schema(type = "number")),
        @Parameter(name = "sortBy",          in = ParameterIn.QUERY, schema = @Schema(type = "string",
            allowableValues = {"id", "date", "totalAmount", "purpose"}, defaultValue = "id")),
        @Parameter(name = "sortDir",         in = ParameterIn.QUERY, schema = @Schema(type = "string",
            allowableValues = {"ASC", "DESC"}, defaultValue = "ASC")),
    })
    @GetMapping("/filter")
    public ResponseEntity<Map<?, ?>> filterConnectionFees(
            @Parameter(hidden = true) @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(hidden = true) @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(hidden = true) @RequestParam(defaultValue = "ASC") String sortDir) {
        log.info("Filtering extractions with filters: {}, page: {}, size: {}, sortBy: {}, sortDir: {} (requested by {})", filters, page, size, sortBy, sortDir, getCurrentUsername());
        int adjustedPage = (page < 1) ? 0 : page - 1;
        Specification<Extraction> spec = ExcelSpecification.getSpecifications((Map) filters);
        return ResponseEntity.ok().body(excelService.letsDoExtractionFilter(spec, adjustedPage, size, sortBy, sortDir));
    }
}
