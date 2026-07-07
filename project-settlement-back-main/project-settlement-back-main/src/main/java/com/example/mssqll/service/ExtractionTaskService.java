package com.example.mssqll.service;

import com.example.mssqll.models.ExtractionTask;
import org.springframework.data.web.PagedModel;

import java.util.List;

public interface ExtractionTaskService {
    public PagedModel<ExtractionTask> getExtractionTasks(int page, int size);
    List<ExtractionTask> findByName(String name);
}
