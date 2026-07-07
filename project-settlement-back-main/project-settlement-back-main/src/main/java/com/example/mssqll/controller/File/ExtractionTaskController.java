package com.example.mssqll.controller.File;


import com.example.mssqll.models.ExtractionTask;
import com.example.mssqll.service.impl.ExtractionTaskServiceImpl;
import com.example.mssqll.utiles.resonse.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import static com.example.mssqll.utiles.SecurityUtils.getCurrentUsername;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PagedModel;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/extraction-task")
@Tag(name = "Extraction Tasks", description = "Manage extraction task records")
public class ExtractionTaskController {
    @Autowired
    ExtractionTaskServiceImpl extractionTaskService;

    @Operation(summary = "Get all extraction tasks (paginated, MANAGER/ADMIN)")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @GetMapping("/all-upls")
    public ApiResponse<PagedModel<ExtractionTask>> getAllUpls(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching all extraction tasks - page: {}, size: {} (requested by {})", page, size, getCurrentUsername());
        int adjustedPage = (page < 1) ? 0 : page - 1;
        PagedModel<ExtractionTask> tasks = extractionTaskService.getExtractionTasks(adjustedPage, size);
        log.info("Retrieved extraction tasks for page: {} (requested by {})", page, getCurrentUsername());
        return new ApiResponse<>(true, "Data fetched", tasks);
    }
    @Operation(summary = "Find extraction tasks by file name")
    @GetMapping("/find-by-name/{fileName}")
    public ApiResponse<List<ExtractionTask>> findByName(@PathVariable String fileName) {
        log.info("Finding extraction tasks by file name: {} (requested by {})", fileName, getCurrentUsername());
        List<ExtractionTask> extractionTasks = extractionTaskService.findByName(fileName);
        log.info("Found {} extraction tasks for file name: {} (requested by {})", extractionTasks.size(), fileName, getCurrentUsername());
        return new ApiResponse<>(true, "Files fetched", extractionTasks);
    }
}
