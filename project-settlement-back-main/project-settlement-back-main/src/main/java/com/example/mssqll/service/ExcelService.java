package com.example.mssqll.service;

import com.example.mssqll.dto.response.ExtractionResponseDto;
import com.example.mssqll.models.Extraction;
import com.example.mssqll.models.ExtractionTask;
import com.example.mssqll.models.Status;
import com.example.mssqll.repository.ExtractionRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PagedModel;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;


public interface ExcelService{
     ExtractionRepository getRepo();
     List<ExtractionResponseDto> readExcel(MultipartFile file);
     PagedModel<Extraction> getAllExtractions(int page, int size) ;
     PagedModel<Extraction> getAllWarningExtractions(int page, int size) ;
     PagedModel<Extraction> getAllOkExtractions(int page, int size) ;
     PagedModel<Extraction> getAllOkExtractionsWithFile(int page, int size,Long fileid) ;
     PagedModel<Extraction> getAllExtractionsWithFile(int page, int size, Long fileid);
     Long getGrandTotal(ExtractionTask extractionTask);
     Long getTotalWarning();
     Long getTotalOk();
     Long getTotalExtractionCount();
     Long getGrandTotal();
     Long getExtractionCountByExtractionTaskId(ExtractionTask extractionTask);
     Long sumTotalAmountByStatus(Status status);
     Long getTotalAmountByExtractionTaskId(Long id);
     Long getCountWarningsByFileId(Long fileId) ;
     Long getCountOkByFileId(Long fileId) ;
     PagedModel<Extraction> getWarningByExtractionTask(Long extractionTaskId, int page, int size) ;
     void deleteById(Long id);
     Map<String, Object> letsDoExtractionFilter(Specification<Extraction> spec, int page, int size, String sortBy, String sortDir);
}
