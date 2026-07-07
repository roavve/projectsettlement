package com.example.mssqll.service.impl;

import com.example.mssqll.dto.response.ExtractionResponseDto;
import com.example.mssqll.models.*;
import com.example.mssqll.repository.ConnectionFeeRepository;
import com.example.mssqll.repository.ExtractionRepository;
import com.example.mssqll.repository.ExtractionTaskRepository;
import com.example.mssqll.service.ExcelService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PagedModel;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import static com.example.mssqll.utiles.SecurityUtils.getCurrentUsername;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Slf4j
@RequiredArgsConstructor
@Service
public class ExcelServiceImpl implements ExcelService {
    private final EntityManager entityManager;
    private final ExtractionRepository extractionRepository;
    private final ExtractionTaskRepository extractionTaskRepository;
    private final ConnectionFeeRepository connectionFeeRepository;
    @Value("${upload.directory}")
    private String uploadDirectory;

    private boolean isCellEmpty(Cell cell) {
        return cell == null || cell.getCellType() == CellType.BLANK;
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<ExtractionResponseDto> readExcel(MultipartFile file) {
        log.info("Starting Excel file processing: {} (by {})", file.getOriginalFilename(), getCurrentUsername());

        LocalDateTime today = LocalDateTime.now();
        String fileName;
        try {
            fileName = storeFile(file);
            log.debug("File stored successfully at: {} (by {})", fileName, getCurrentUsername());
        } catch (IOException e) {
            log.error("Failed to store file: {} (by {})", file.getOriginalFilename(), getCurrentUsername(), e);
            throw new RuntimeException(e);
        }
        ExtractionTask extTask = extractionTaskRepository.save(new ExtractionTask(today, fileName, FileStatus.GOOD));
        List<ExtractionResponseDto> extractionResponseDtoList = new ArrayList<>();
        try {
            XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream());
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                boolean fileStatus = true;
                for (Row row : sheet) {
                    if (row.getRowNum() < 1) {
                        continue;
                    }


                    LocalDate date = null;
                    double totalAmount = 0.0;
                    String purpose = "";
                    String description = "";
                    String tax = "";

                    Status status = Status.GOOD;

                    for (int cellNum = 0; cellNum < row.getLastCellNum(); cellNum++) {
                        Cell cell = row.getCell(cellNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);

                        if (!isCellEmpty(cell)) {
                            String cellValue = getCellValueAsString(cell);

                            switch (cellNum) {
                                case 0:
                                    if (!cellValue.isEmpty()) {
                                        try {
                                            date = LocalDate.parse(cellValue, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                                        } catch (Exception e) {
                                            continue;
                                        }
                                    }
                                    break;
                                case 1:
                                    if (!cellValue.isEmpty()) {
                                        try {
                                            totalAmount = Double.parseDouble(cellValue);
                                        } catch (NumberFormatException e) {
                                            totalAmount = 0.0;
                                        }
                                    }
                                    break;
                                case 2:
                                    purpose = cellValue;
                                    break;
                                case 3:
                                    description = cellValue;
                                    break;
                                case 4:
                                    tax = cellValue;
                            }
                        }
                    }

                    if (date == null && totalAmount == 0.0 && purpose.isEmpty() && description.isEmpty()) {
                        continue;
                    }

                    if (date == null || totalAmount == 0 || purpose.isEmpty() || description.isEmpty()) {
                        status = Status.WARNING;
                        fileStatus = false;
                    }

                    Extraction extraction = new Extraction();
                    extraction.setDate(date);
                    extraction.setTotalAmount(totalAmount);
                    extraction.setPurpose(purpose);
                    extraction.setDescription(description.trim());
                    log.debug("Processing extraction with description length: {} (by {})", description.length(), getCurrentUsername());
                    extraction.setStatus(status);
                    extraction.setTax(tax);
                    extraction.setExtractionTask(extTask);

                    if (!fileStatus) {
                        extTask.setStatus(FileStatus.WARNING);
                    } else {
                        extTask.setStatus(FileStatus.GOOD);
                    }
                    extractionTaskRepository.save(extTask);
                    Extraction excelExtraction= new Extraction();
                    try {
                        excelExtraction = extractionRepository.save(extraction);
                    } catch (Exception e) {
                        log.error("Failed to save extraction: {} (by {})", e.getMessage(), getCurrentUsername(), e);
                    }
                    extractionResponseDtoList.add(ExtractionResponseDto.builder()
                            .id(excelExtraction.getId())
                            .date(excelExtraction.getDate())
                            .totalAmount(excelExtraction.getTotalAmount())
                            .purpose(excelExtraction.getPurpose())
                            .description(excelExtraction.getDescription())
                            .status(excelExtraction.getStatus())
                            .extractionTask(excelExtraction.getExtractionTask())
                            .build());
                }
            }
        } catch (IOException e) {
            log.error("IOException while reading Excel file: {} (by {})", e.getMessage(), getCurrentUsername(), e);
        }
        log.info("Excel file processing completed. Total extractions: {} (by {})", extractionResponseDtoList.size(), getCurrentUsername());
        return extractionResponseDtoList;
    }


    public String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    return new SimpleDateFormat("dd/MM/yyyy").format(date);
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return "Unknown Cell Type";
        }
    }


    @Override
    public PagedModel<Extraction> getAllExtractions(int page, int size) {
        return new PagedModel<>(extractionRepository.findAll(PageRequest.of(page, size)));
    }

    @Override
    public PagedModel<Extraction> getAllWarningExtractions(int page, int size) {
        return new PagedModel<>(extractionRepository.findByStatus(Status.WARNING, PageRequest.of(page, size)));
    }

    @Override
    public PagedModel<Extraction> getAllOkExtractions(int page, int size) {
        return new PagedModel<>(extractionRepository.findByStatus(Status.GOOD, PageRequest.of(page, size)));
    }

    @Override
    public PagedModel<Extraction> getAllExtractionsWithFile(int page, int size, Long fileId) {
        return new PagedModel<>(extractionRepository.findByExtractionTaskId(fileId, PageRequest.of(page, size)));
    }

    @Override
    public PagedModel<Extraction> getAllOkExtractionsWithFile(int page, int size, Long fileid) {
        return new PagedModel<>(extractionRepository.findByExtractionTaskIdAndStatus(fileid, Status.GOOD, PageRequest.of(page, size)));
    }

    @Override
    public Long getGrandTotal(ExtractionTask taskId) {
        return extractionRepository.sumTotalAmountByExtractionTask(taskId);
    }

    @Override
    public Long getTotalWarning() {
        return extractionRepository.countAllByStatus(Status.WARNING);
    }

    @Override
    public Long getTotalOk() {
        return extractionRepository.countAllByStatus(Status.GOOD);
    }

    @Override
    public Long getTotalExtractionCount() {
        return extractionRepository.count();
    }

    @Override
    public Long getGrandTotal() {
        return extractionRepository.sumTotalAmount();
    }


    @Override
    public Long getExtractionCountByExtractionTaskId(ExtractionTask extractionTask) {
        return extractionRepository.countAllByExtractionTaskId(extractionTask.getId());
    }

    @Override
    public Long sumTotalAmountByStatus(Status status) {
        return extractionRepository.sumTotalAmountByStatus(Status.GOOD);
    }

    @Override
    public Long getTotalAmountByExtractionTaskId(Long id) {
        return extractionRepository.sumTotalAmountByExtractionTask(
                extractionTaskRepository.getReferenceById(id)
        );
    }

    @Override
    public Long getCountWarningsByFileId(Long fileId) {
        return extractionRepository.countWarningsByFileId(extractionTaskRepository.getReferenceById(fileId));

    }

    @Override
    public Long getCountOkByFileId(Long fileId) {
        return extractionRepository.countOkByFileId(extractionTaskRepository.getReferenceById(fileId));
    }

    @Override
    public PagedModel<Extraction> getWarningByExtractionTask(Long extractionTaskId, int page, int size) {
        return new PagedModel<>(extractionRepository.findByExtractionTaskIdAndStatus(extractionTaskId, Status.WARNING, PageRequest.of(page, size))
        );
    }

    @Override
    public void deleteById(Long id) {
        log.info("Deleting extraction task ID: {} (by {})", id, getCurrentUsername());

        extractionRepository.deleteByExtractionTask(extractionTaskRepository.getReferenceById(id));
        connectionFeeRepository.deleteByExtractionTaskId(id);
        extractionTaskRepository.deleteById(id);

        log.info("Successfully deleted extraction task ID: {} (by {})", id, getCurrentUsername());
    }


    @Override
    public Map<String, Object> letsDoExtractionFilter(Specification<Extraction> spec, int page, int size, String sortBy, String sortDir) {
        log.debug("Filtering extractions with page={}, size={}, sortBy={}, sortDir={} (by {})", page, size, sortBy, sortDir, getCurrentUsername());
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        PagedModel<Extraction> excPage = new PagedModel<>(extractionRepository.findAll(spec, PageRequest.of(page, size, sort)));

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Double> sumQuery = cb.createQuery(Double.class);
        Root<Extraction> sumRoot = sumQuery.from(Extraction.class);
        sumQuery.select(cb.sum(sumRoot.get("totalAmount")));

        Predicate sumPredicate = spec.toPredicate(sumRoot, sumQuery, cb);
        if (sumPredicate != null) {
            sumQuery.where(sumPredicate);
        }
        Double totalAmountSum = entityManager.createQuery(sumQuery).getSingleResult();

        CriteriaQuery<Long> countGoodQuery = cb.createQuery(Long.class);
        Root<Extraction> goodRoot = countGoodQuery.from(Extraction.class);
        countGoodQuery.select(cb.count(goodRoot));
        Predicate goodPredicate = spec.toPredicate(goodRoot, countGoodQuery, cb);

        Predicate goodStatusPredicate = cb.equal(goodRoot.get("status"), Status.GOOD);
        if (goodPredicate != null) {
            countGoodQuery.where(cb.and(goodPredicate, goodStatusPredicate));
        } else {
            countGoodQuery.where(goodStatusPredicate);
        }
        Long goodCount = entityManager.createQuery(countGoodQuery).getSingleResult();

        CriteriaQuery<Long> countWarnQuery = cb.createQuery(Long.class);
        Root<Extraction> warnRoot = countWarnQuery.from(Extraction.class);
        countWarnQuery.select(cb.count(warnRoot));

        Predicate warnPredicate = spec.toPredicate(warnRoot, countWarnQuery, cb);
        Predicate warnStatusPredicate = cb.equal(warnRoot.get("status"), Status.WARNING);
        if (warnPredicate != null) {
            countWarnQuery.where(cb.and(warnPredicate, warnStatusPredicate));
        } else {
            countWarnQuery.where(warnStatusPredicate);
        }
        Long warnCount = entityManager.createQuery(countWarnQuery).getSingleResult();

        Map<String, Object> response = new HashMap<>();
        response.put("totalAmountSum", totalAmountSum);
        response.put("excPage", excPage);
        response.put("ok", goodCount);
        response.put("warn", warnCount);

        return response;
    }

    @Override
    public ExtractionRepository getRepo() {
        return this.extractionRepository;
    }

    public String storeFile(MultipartFile file) throws IOException {
        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        if (originalFileName.contains("..")) {
            log.warn("Attempted to upload file with invalid path: {} (by {})", originalFileName, getCurrentUsername());
            throw new IOException("Invalid file name: " + originalFileName);
        }

        String uniqueFileName = LocalDateTime.now().toString().replace(':', '-') + "_" + originalFileName;

        Path targetLocation = Paths.get(uploadDirectory).resolve(uniqueFileName);
        Files.createDirectories(targetLocation.getParent());

        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        return uniqueFileName;
    }
}
