package com.example.mssqll.service.impl;

import com.example.mssqll.models.ExtractionTask;
import com.example.mssqll.repository.ExtractionTaskRepository;
import com.example.mssqll.service.ExtractionTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;

import static com.example.mssqll.utiles.SecurityUtils.getCurrentUsername;

import java.util.List;

@Slf4j
@Service
public class ExtractionTaskServiceImpl implements ExtractionTaskService {
    @Autowired
    private ExtractionTaskRepository extractionTaskRepository;

    @Override
    public PagedModel<ExtractionTask> getExtractionTasks(int page, int size) {
        log.info("Retrieving extraction tasks - Page: {}, Size: {} (by {})", page, size, getCurrentUsername());

        PagedModel<ExtractionTask> result = new PagedModel<>(extractionTaskRepository.findAllByStatusDelete(PageRequest.of(page, size)));
        log.info("Retrieved {} extraction tasks from page {} (by {})", result.getContent().size(), page, getCurrentUsername());
        log.debug("Total elements: {}, Total pages: {} (by {})", result.getContent().size(), result.getMetadata().totalPages(), getCurrentUsername());

        return result;
    }

    @Override
    public List<ExtractionTask> findByName(String name) {
        log.info("Finding extraction tasks by file name: {} (by {})", name, getCurrentUsername());

        List<ExtractionTask> tasks = extractionTaskRepository.findByFileName(name);

        if (tasks.isEmpty()) {
            log.warn("No extraction tasks found with file name: {} (by {})", name, getCurrentUsername());
        } else {
            log.info("Found {} extraction task(s) with file name: {} (by {})", tasks.size(), name, getCurrentUsername());
        }

        return tasks;
    }
}
