package com.example.mssqll.service.impl;


import com.example.mssqll.dto.request.ConnectionFeeUpdateRequestDto;
import com.example.mssqll.dto.response.ConnectionFeeChildrenDTO;
import com.example.mssqll.dto.response.ConnectionFeeResponseDto;
import com.example.mssqll.dto.response.ConnectionFeeTaxPayerDto;
import com.example.mssqll.dto.response.UserResponseDto;
import com.example.mssqll.models.*;
import com.example.mssqll.repository.ConnectionFeeCustomRepository;
import com.example.mssqll.repository.ConnectionFeeRepository;
import com.example.mssqll.repository.ExtractionRepository;
import com.example.mssqll.repository.ExtractionTaskRepository;
import com.example.mssqll.service.ConnectionFeeService;
import com.example.mssqll.utiles.ConnectionFeeUtils;
import com.example.mssqll.utiles.exceptions.FileAlreadyTransferredException;
import com.example.mssqll.utiles.exceptions.ResourceNotFoundException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PagedModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import static com.example.mssqll.utiles.SecurityUtils.getCurrentUsername;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConnectionFeeServiceImpl implements ConnectionFeeService {
    @Autowired
    private final ConnectionFeeRepository connectionFeeRepository;
    @Autowired
    private final ExtractionRepository extractionRepository;
    @Autowired
    private final ExtractionTaskRepository extractionTaskRepository;
    @Autowired
    private final ConnectionFeeCustomRepository connectionFeeCustomRepository;
    @Autowired
    private final ConnectionFeeUtils connectionFeeUtils;

    public ConnectionFeeServiceImpl(ConnectionFeeRepository connectionFeeRepository,
                                    ExtractionRepository extractionRepository,
                                    ExtractionTaskRepository extractionTaskRepository,
                                    ConnectionFeeCustomRepository connectionFeeCustomRepository,
                                    ConnectionFeeUtils connectionFeeUtils) {
        this.connectionFeeRepository = connectionFeeRepository;
        this.extractionRepository = extractionRepository;
        this.extractionTaskRepository = extractionTaskRepository;
        this.connectionFeeCustomRepository = connectionFeeCustomRepository;
        this.connectionFeeUtils = connectionFeeUtils;
    }

    @Override
    public PagedModel<ConnectionFee> getAllFee(int page, int size) {
        return new PagedModel<>(connectionFeeRepository.findAll(PageRequest.of(page, size)));
    }

    @Override
    public Optional<ConnectionFee> getFee(Long id) {
        return connectionFeeRepository.findById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ConnectionFee> saveFee(Long extractionTask) {
        log.info("=== START saveFee: extractionTaskId={}, user={} ===", extractionTask, getCurrentUsername());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User userDetails = (User) authentication.getPrincipal();
        log.debug("Authenticated user: id={}, email={}", userDetails.getId(), userDetails.getEmail());

        Optional<ExtractionTask> extractionTaskOptional = extractionTaskRepository.findById(extractionTask);
        if (extractionTaskOptional.isEmpty()) {
            log.error("Extraction task not found with ID: {} (by {})", extractionTask, getCurrentUsername());
            throw new ResourceNotFoundException("Extraction task not found");
        }

        ExtractionTask extractionTask1 = extractionTaskOptional.get();
        log.info("ExtractionTask found: id={}, fileName='{}', currentStatus={}",
                extractionTask1.getId(), extractionTask1.getFileName(), extractionTask1.getStatus());

        if (extractionTask1.getStatus() == FileStatus.TRANSFERRED_GOOD ||
                extractionTask1.getStatus() == FileStatus.TRANSFERRED_WARNING) {
            log.warn("Attempted to transfer already transferred file with ID: {} status={} (by {})",
                    extractionTask1.getId(), extractionTask1.getStatus(), getCurrentUsername());
            throw new FileAlreadyTransferredException("file with id: " + extractionTask1.getId() + " already transferred");
        }

        List<Extraction> extractions = extractionRepository.findByExtractionTask(extractionTask1);
        int expectedCount = extractions.size();
        log.info("Found {} extractions for taskId={} (file='{}')", expectedCount, extractionTask, extractionTask1.getFileName());

        if (extractions.isEmpty()) {
            log.warn("No extractions found for taskId={} — nothing to transfer", extractionTask);
            return Collections.emptyList();
        }

        log.debug("Source extractions to be transferred:");
        for (int i = 0; i < extractions.size(); i++) {
            Extraction e = extractions.get(i);
            log.debug("  [{}] id={}, tax='{}', amount={}, date={}, purpose='{}', description='{}'",
                    i + 1, e.getId(), e.getTax(), e.getTotalAmount(), e.getDate(), e.getPurpose(), e.getDescription());
        }

        // Update Task Status
        FileStatus prevStatus = extractionTask1.getStatus();
        if (extractionTask1.getStatus().equals(FileStatus.WARNING)) {
            extractionTask1.setStatus(FileStatus.TRANSFERRED_WARNING);
        } else {
            extractionTask1.setStatus(FileStatus.TRANSFERRED_GOOD);
        }
        extractionTask1.setSendDate(LocalDateTime.now());
        extractionTaskRepository.save(extractionTask1);
        log.info("ExtractionTask status updated: {} -> {} (id={})", prevStatus, extractionTask1.getStatus(), extractionTask1.getId());

        List<ConnectionFee> fees = new ArrayList<>();
        List<Long> failedExtractionIds = new ArrayList<>();

        for (int i = 0; i < extractions.size(); i++) {
            Extraction extraction = extractions.get(i);
            try {
                ConnectionFee fee = ConnectionFee.builder()
                        .orderStatus(OrderStatus.ORDER_INCOMPLETE)
                        .purpose(extraction.getPurpose())
                        .totalAmount(extraction.getTotalAmount())
                        .extractionDate(extraction.getDate())
                        .status(Status.TRANSFERRED)
                        .transferDate(LocalDateTime.now())
                        .extractionTask(extraction.getExtractionTask())
                        .description(extraction.getDescription())
                        .extractionId(extraction.getId())
                        .tax(extraction.getTax())
                        .transferPerson(userDetails)
                        .changePerson(userDetails)
                        .build();
                fees.add(fee);
                log.debug("  Mapped [{}/{}]: extractionId={}, tax='{}', amount={}, date={}, purpose='{}'",
                        i + 1, expectedCount, extraction.getId(), extraction.getTax(),
                        extraction.getTotalAmount(), extraction.getDate(), extraction.getPurpose());
            } catch (Exception ex) {
                failedExtractionIds.add(extraction.getId());
                log.error("  MAPPING FAILED [{}/{}]: extractionId={}, tax='{}', amount={}, date={}, purpose='{}', error='{}'",
                        i + 1, expectedCount, extraction.getId(), extraction.getTax(),
                        extraction.getTotalAmount(), extraction.getDate(), extraction.getPurpose(), ex.getMessage(), ex);
            }
        }

        if (!failedExtractionIds.isEmpty()) {
            log.error("ROLLING BACK — mapping failed for {}/{} extractions. Failed extractionIds={} (taskId={}, file='{}', by={})",
                    failedExtractionIds.size(), expectedCount, failedExtractionIds,
                    extractionTask, extractionTask1.getFileName(), getCurrentUsername());
            throw new RuntimeException("Transfer failed: could not map " + failedExtractionIds.size() +
                    " extraction(s). Failed IDs: " + failedExtractionIds);
        }

        log.info("All {} extractions mapped, calling saveAll (taskId={})", fees.size(), extractionTask);

        List<ConnectionFee> saved;
        try {
            saved = connectionFeeRepository.saveAll(fees);
        } catch (Exception ex) {
            log.error("ROLLING BACK — saveAll threw exception after mapping {} fees (taskId={}, file='{}', by={}): {}",
                    fees.size(), extractionTask, extractionTask1.getFileName(), getCurrentUsername(), ex.getMessage(), ex);
            throw ex;
        }

        int savedCount = saved.size();
        if (savedCount != expectedCount) {
            Set<Long> savedExtractionIds = saved.stream()
                    .map(ConnectionFee::getExtractionId)
                    .collect(Collectors.toSet());
            List<Long> missingIds = extractions.stream()
                    .map(Extraction::getId)
                    .filter(id -> !savedExtractionIds.contains(id))
                    .collect(Collectors.toList());
            log.error("ROLLING BACK — count mismatch: expected={}, saved={}, missingExtractionIds={} (taskId={}, file='{}', by={})",
                    expectedCount, savedCount, missingIds,
                    extractionTask, extractionTask1.getFileName(), getCurrentUsername());
            throw new RuntimeException("Transfer count mismatch: expected " + expectedCount +
                    " rows but only " + savedCount + " were saved. Missing extractionIds: " + missingIds);
        }

        log.info("=== END saveFee: saved={}/{} connection fees, taskId={}, file='{}', by={} ===",
                savedCount, expectedCount, extractionTask, extractionTask1.getFileName(), getCurrentUsername());
        return saved;
    }

    @Override
    public ConnectionFee save(ConnectionFee connectionFee) {
        connectionFee.setTransferDate(LocalDateTime.now());
        return connectionFeeRepository.save(connectionFee);
    }

    @Override
    public Optional<ConnectionFee> findById(Long id) {
        return connectionFeeRepository.findById(id);
    }

    @Override
    public ConnectionFee updateFee(Long connectionFeeId, ConnectionFeeUpdateRequestDto connectionFeeDetails) {
        ConnectionFee existingFee = connectionFeeRepository.findById(connectionFeeId)
                .orElseThrow(() -> new ResourceNotFoundException("ConnectionFee not found with id: " + connectionFeeId));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User userDetails = (User) authentication.getPrincipal();
        
        connectionFeeUtils.logFieldChanges(existingFee, connectionFeeDetails, connectionFeeId, userDetails);

        // If the request carries no order/identification fields at all, treat it as a reset:
        // clear those fields and persist, skipping the normal field-update flow (which would
        // otherwise NPE on the null request values and re-populate the canceled lists).
        if (connectionFeeUtils.clearConnectionFeeIfRequestEmpty(existingFee, connectionFeeDetails)) {
            log.info("[updateFee] Empty request — clearing ConnectionFee id={} (by {})", connectionFeeId, getCurrentUsername());
            return connectionFeeRepository.save(existingFee);
        }

        // Values needed both by the extracted helpers and the rest of this method.
        String newProjectID = connectionFeeDetails.getProjectID() != null && !connectionFeeDetails.getProjectID().trim().isEmpty() ? connectionFeeDetails.getProjectID().trim() : null;
        boolean projectIdChanged = !Objects.equals(existingFee.getProjectID(), newProjectID);

        // clarificationDate business rule — reads the old orderStatus/orderN/clarificationDate,
        // so it must run before any setter mutates them.
        connectionFeeUtils.applyClarificationDate(existingFee, connectionFeeDetails, connectionFeeId, newProjectID, userDetails);

        // Set basic fields
        existingFee.setStatus(connectionFeeDetails.getStatus());
        existingFee.setRegion(connectionFeeDetails.getRegion().trim());
        existingFee.setServiceCenter(connectionFeeDetails.getServiceCenter().trim());

        // Handle first withdraw type
        if (existingFee.getFirstWithdrawType() == null) {
            existingFee.setFirstWithdrawType(connectionFeeDetails.getWithdrawType());
        }
        existingFee.setWithdrawType(connectionFeeDetails.getWithdrawType().trim());
        existingFee.setPaymentOrderSentDate(connectionFeeDetails.getPaymentOrderSentDate());
        // Handle extraction task
        if (connectionFeeDetails.getExtractionTaskId() != null) {
            ExtractionTask extractionTask = extractionTaskRepository.findById(connectionFeeDetails.getExtractionTaskId())
                    .orElseThrow(() -> new ResourceNotFoundException("ExtractionTask not found with id: " + connectionFeeDetails.getExtractionTaskId()));
            existingFee.setExtractionTask(extractionTask);
        }

        existingFee.setNote(connectionFeeDetails.getNote() != null ? connectionFeeDetails.getNote().trim() : null);
        existingFee.setExtractionDate(connectionFeeDetails.getExtractionDate());
        existingFee.setTotalAmount(connectionFeeDetails.getTotalAmount());

        if (connectionFeeDetails.getPurpose() != null) {
            existingFee.setPurpose(connectionFeeDetails.getPurpose().trim());
        }

        if (connectionFeeDetails.getDescription() != null) {
            existingFee.setDescription(connectionFeeDetails.getDescription().trim());
        }

        existingFee.setTreasuryRefundDate(connectionFeeDetails.getTreasuryRefundDate());

        // Handle canceled projects
        List<String> canceledProjects;

        // If canceled projects are provided in request (even if empty), replace all with the given values
        if (connectionFeeDetails.getCanceledProject() != null) {
            canceledProjects = new ArrayList<>(connectionFeeDetails.getCanceledProject());
        } else {
            // Otherwise, use existing logic to manage canceled projects
            canceledProjects = existingFee.getCanceledProject() != null ?
                    new ArrayList<>(existingFee.getCanceledProject()) : new ArrayList<>();

            boolean isCanceledOrder = connectionFeeDetails.getOrderStatus() == OrderStatus.CANCELED;

            if (isCanceledOrder || projectIdChanged) {
                // If project ID changed, add old project to canceled list
                if (!canceledProjects.isEmpty()) {
                    if (!Objects.equals(existingFee.getProjectID(), canceledProjects.get(canceledProjects.size() - 1))) {
                        canceledProjects.add(existingFee.getProjectID());
                    }
                } else {
                    if (existingFee.getProjectID() != null) {
                        canceledProjects.add(existingFee.getProjectID());
                    }
                }
            }
        }

        existingFee.setCanceledProject(canceledProjects);
        existingFee.setProjectID(newProjectID);

        // Handle canceled orders
        if (!Objects.equals(existingFee.getOrderN(), connectionFeeDetails.getOrderN())) {
            List<String> canceledOrders = existingFee.getCanceledOrders() != null ?
                    new ArrayList<>(existingFee.getCanceledOrders()) : new ArrayList<>();
            if (existingFee.getOrderN() != null) {
                canceledOrders.add(existingFee.getOrderN());
            }
            existingFee.setCanceledOrders(canceledOrders);
            existingFee.setOrderN(connectionFeeDetails.getOrderN());
        }

        existingFee.setStatus(Status.TRANSFER_COMPLETE);
        existingFee.setOrderStatus(connectionFeeDetails.getOrderStatus());


        return connectionFeeRepository.save(existingFee);
    }

    @Override
    public void deleteByTaskId(Long taskId) {
        Optional<ExtractionTask> extractionTask = extractionTaskRepository.findById(taskId);

        if (extractionTask.isPresent()) {
            ExtractionTask extractionTask1 = extractionTask.get();
            extractionTask1.setStatus(FileStatus.SOFT_DELETED);
            connectionFeeRepository.updateStatusByExtractionTask(Status.SOFT_DELETED, extractionTask1);
        }
    }

    @Override
    public void softDeleteById(Long id) {
        log.info("Soft deleting connection fee ID: {} (by {})", id, getCurrentUsername());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User userDetails = (User) authentication.getPrincipal();

        ConnectionFee connectionFee = connectionFeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ConnectionFee not found with id: " + id));

        ConnectionFee parent = connectionFee.getParent();

        // Deleting a top-level (parentless) fee — just soft-delete it directly
        if (parent == null) {
            connectionFee.setStatus(Status.SOFT_DELETED);
            connectionFee.setChangePerson(userDetails);
            connectionFeeRepository.save(connectionFee);
            return;
        }

        List<ConnectionFee> connectionFees = connectionFeeRepository.findAllDescendants(parent.getId());

        // If the sum of all children equals the parent's total amount, mark it as TRANSFERRED
        if (Objects.equals(connectionFeeRepository.sumTotalAmountByParentId(parent), parent.getTotalAmount())) {
            Optional<ConnectionFee> reminderFeeOpt = connectionFeeRepository.findReminderChildByParentId(parent.getId());


            if (reminderFeeOpt.isEmpty()) {
                if (parent.getTotalAmount() - connectionFee.getTotalAmount() != 0) {
                    ConnectionFee reminderFee1 = new ConnectionFee();
                    reminderFee1.setParent(parent);
                    reminderFee1.setNote("ნაშთი");
                    reminderFee1.setStatus(Status.REMINDER);
                    reminderFee1.setChangePerson(userDetails);
                    reminderFee1.setTransferPerson(userDetails);
                    reminderFee1.setExtractionTask(parent.getExtractionTask());
                    reminderFee1.setOrderN("ნაშთი");
                    reminderFee1.setPurpose("ნაშთი");
                    reminderFee1.setTotalAmount(connectionFee.getTotalAmount());
                    connectionFeeRepository.save(reminderFee1);
                }
            }
            connectionFee.setStatus(Status.SOFT_DELETED);
            parent.setStatus(Status.TRANSFERRED);
            connectionFeeRepository.save(parent);
            connectionFeeRepository.save(connectionFee);
            return;
        }

        // Check if the connectionFee is NOT a REMINDER
        if (!connectionFee.getStatus().equals(Status.REMINDER)) {
            Optional<ConnectionFee> reminderFeeOpt = connectionFeeRepository.findReminderChildByParentId(parent.getId());

            if (reminderFeeOpt.isPresent()) {
                ConnectionFee reminderFee = reminderFeeOpt.get();

                // Add the deleted child's amount to the reminder fee
                reminderFee.setTotalAmount(reminderFee.getTotalAmount() + connectionFee.getTotalAmount());

                // If the new reminder amount equals the parent's total amount, delete the reminder
                if (reminderFee.getTotalAmount().equals(parent.getTotalAmount())) {
                    parent.setWithdrawType(parent.getFirstWithdrawType());
                    parent.setRegion(connectionFee.getRegion());
                    parent.setServiceCenter(connectionFee.getServiceCenter());
                    parent.setProjectID(connectionFee.getProjectID());
                    parent.setOrderN(connectionFee.getOrderN());
                    parent.setStatus(Status.TRANSFERRED);
                    connectionFeeRepository.save(parent);
                    connectionFeeRepository.delete(reminderFee);
                } else {
                    connectionFeeRepository.save(reminderFee);
                }
            }

            // Soft delete the current connectionFee
            connectionFee.setStatus(Status.SOFT_DELETED);
            connectionFee.setChangePerson(userDetails);
            connectionFeeRepository.save(connectionFee);
        }
        // Handle case where only one child remains
        else if (connectionFees.size() == 1) {
            ConnectionFee lastChild = connectionFees.get(0);
            lastChild.setStatus(Status.SOFT_DELETED);
            lastChild.setChangePerson(userDetails);
            connectionFeeRepository.save(lastChild);

            // Restore parent fields from last child
            parent.setRegion(lastChild.getRegion());
            parent.setServiceCenter(lastChild.getServiceCenter());
            parent.setProjectID(lastChild.getProjectID());
            parent.setOrderN(lastChild.getOrderN());

            // Update the parent's status
            if (parent.getProjectID() != null) {
                parent.setStatus(Status.TRANSFER_COMPLETE);
            } else {
                parent.setStatus(Status.TRANSFERRED);
            }
            connectionFeeRepository.save(parent);
        }
    }

    @Override
    public ByteArrayInputStream createExcel(List<ConnectionFee> connectionFees) throws IOException {
        String[] baseColumns = {
                "ID", "ორდერის N", "რეგიონი", "სერვის ცენტრი", "პროექტის ნომერი", "ტიპი",
                "ჩარიცხვის თარიღი", "თანხა", "მიზანი", "აღწერა", "გადამხდელის იდენტიფიკატორი", "გარკვევის თარიღი",
                "შენიშვნა", "თანხის დაბრუნების მოთხოვნის თარიღი", "დაბრუნების თარიღი"
        };

        List<ConnectionFee> flatList = new ArrayList<>();
        for (ConnectionFee fee : connectionFees) {
            if (fee.getStatus() == Status.REMINDER) continue;
            flatList.add(fee);
            flatList.addAll(fee.getChildren().stream()
                    .filter(child -> child.getStatus() != Status.SOFT_DELETED && child.getStatus() != Status.REMINDER)
                    .toList());
        }

        int maxCanceled = flatList.stream()
                .mapToInt(fee -> fee.getCanceledProject() != null ? fee.getCanceledProject().size() : 0)
                .max()
                .orElse(0);

        List<String> allColumns = new ArrayList<>(List.of(baseColumns));
        for (int i = 1; i <= maxCanceled; i++) {
            allColumns.add("გაუქმებული პროექტი " + i);
        }
        allColumns.add("შემცვლელი");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0xEF);
        out.write(0xBB);
        out.write(0xBF);
        OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);

        writer.write(String.join(",", allColumns) + "\n");

        for (ConnectionFee fee : flatList) {
            for (int col = 0; col < allColumns.size(); col++) {
                String columnName = allColumns.get(col);
                String value;

                if (columnName.startsWith("გაუქმებული პროექტი ")) {
                    int index = Integer.parseInt(columnName.substring("გაუქმებული პროექტი ".length())) - 1;
                    List<String> canceled = fee.getCanceledProject();
                    value = (canceled != null && index < canceled.size()) ? canceled.get(index) : "";
                } else {
                    value = getCellValue(fee, columnName);
                }

                if (value != null && value.contains(",")) {
                    value = value.replace("\n", "").replace("\r", "");
                    writer.write("\"" + value.replace("\"", "\"\"") + "\"");
                } else {
                    writer.write(value != null ? value : "");
                }

                if (col < allColumns.size() - 1) {
                    writer.write(",");
                }
            }

            writer.write("\n");
        }

        writer.flush();
        return new ByteArrayInputStream(out.toByteArray());
    }


    @Cacheable(value = "excelCache", key = "#filters.toString()")
    @SneakyThrows
    @Override
    public void divideFee(Long feeId, Double[] arr) {
        log.info("Dividing fee ID: {} into {} parts (by {})", feeId, arr.length, getCurrentUsername());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User userDetails = (User) authentication.getPrincipal();

        Optional<Double> arrSum = Arrays.stream(arr).reduce(Double::sum);
        Optional<ConnectionFee> connectionFee = connectionFeeRepository.findById(feeId);
        List<ConnectionFee> feeToAdd = new ArrayList<>();
        ConnectionFee connectionFeeCopy;
        ConnectionFee connectionFee1;

        if (connectionFee.isPresent()) {
            connectionFee1 = connectionFee.get();
            if (arrSum.isPresent()) {
                Double sum = arrSum.get();
                if (sum != 0.0) {
                    Double childSum = (connectionFeeRepository.sumTotalAmountByParentId(connectionFee1) != null)
                            ? connectionFeeRepository.sumTotalAmountByParentId(connectionFee1) : 0.0;
                    if (sum <= connectionFee1.getTotalAmount() && (childSum + sum) <= connectionFee1.getTotalAmount()) {
                        Optional<ConnectionFee> reminderChildOpt = connectionFeeRepository.findReminderChildByParentId(connectionFee1.getId());
                        boolean reminderUpdated = false;
                        String childWithdrawType = null;
                        boolean useChildWithdrawType = false;
                        if (reminderChildOpt.isPresent()) {
                            ConnectionFee reminderChild = reminderChildOpt.get();
                            double reminderAmount = reminderChild.getTotalAmount();
                            double newReminderAmount = reminderAmount - sum;

                            if (newReminderAmount >= 0) {
                                reminderChild.setTotalAmount(newReminderAmount);
                                connectionFeeRepository.save(reminderChild);
                                reminderUpdated = true;
                            } else {
                                throw new Exception("Insufficient amount in Reminder child for this operation.");
                            }
                            Optional<ConnectionFee> firstNonReminder = connectionFeeRepository.findByParentIdIn(List.of(connectionFee1.getId()))
                                    .stream()
                                    .filter(c -> c.getStatus() != Status.REMINDER)
                                    .findFirst();
                            if (firstNonReminder.isPresent()) {
                                childWithdrawType = firstNonReminder.get().getWithdrawType();
                                useChildWithdrawType = true;
                            }
                        }
                        int childNum = 1;
                        double newElement = connectionFee1.getTotalAmount() - childSum - sum;
                        Double[] newArr = Arrays.copyOf(arr, arr.length + (reminderUpdated ? 0 : 1));
                        if (!reminderUpdated) {
                            newArr[newArr.length - 1] = newElement;
                        }
                        for (Double d : newArr) {
                            if (d == 0.0) {
                                continue;
                            }
                            connectionFeeCopy = new ConnectionFee(connectionFee1);
                            connectionFeeCopy.setTotalAmount(d);
                            connectionFeeCopy.setParent(connectionFee1);
                            connectionFeeCopy.setChangePerson(userDetails);
                            connectionFeeCopy.setTransferPerson(userDetails);
                            connectionFeeCopy.setOrderStatus(OrderStatus.ORDER_INCOMPLETE);
                            connectionFeeCopy.setStatus(Status.TRANSFERRED);
                            if (useChildWithdrawType) {
                                connectionFeeCopy.setWithdrawType(childWithdrawType);
                            }

                            String parentQueueNumber = connectionFee1.getQueueNumber() != null
                                    ? connectionFee1.getQueueNumber()
                                    : String.valueOf(connectionFee1.getId());
                            connectionFeeCopy.setQueueNumber(parentQueueNumber + "-" + (connectionFeeRepository.childNumberByParentId(feeId) + childNum));
                            childNum++;
                            feeToAdd.add(connectionFeeCopy);
                        }

                        boolean isLastElement = feeToAdd.get(feeToAdd.size() - 1).getTotalAmount() == newElement;
                        boolean isFullSumMatch = (sum + childSum == connectionFee1.getTotalAmount());
                        if (!reminderUpdated && !isFullSumMatch && isLastElement) {
                            ConnectionFee lastFee = feeToAdd.get(feeToAdd.size() - 1);
                            lastFee.setOrderN("ნაშთი");
                            lastFee.setDescription("ნაშთი");
                            lastFee.setPurpose("ნაშთი");
                            lastFee.setStatus(Status.REMINDER);
                            // Move REMINDER to front and reassign queue numbers
                            feeToAdd.add(0, feeToAdd.remove(feeToAdd.size() - 1));
                            String parentQN = connectionFee1.getQueueNumber() != null
                                    ? connectionFee1.getQueueNumber()
                                    : String.valueOf(connectionFee1.getId());
                            int base = connectionFeeRepository.childNumberByParentId(feeId);
                            for (int i = 0; i < feeToAdd.size(); i++) {
                                feeToAdd.get(i).setQueueNumber(parentQN + "-" + (base + i + 1));
                            }
                        } else if (isFullSumMatch) {
                            connectionFeeRepository.deleteResidualEntriesByParentId(connectionFee1.getId());
                        }
                        connectionFee1.setStatus(Status.CANCELED);
                        connectionFee1.setWithdrawType("4 (ერთანი გადახდა, გადანაწილებული რამოდენიმე პროექტის საფასურად)");
                        connectionFee1.setRegion(null);
                        connectionFee1.setServiceCenter(null);
                        connectionFee1.setProjectID(null);
                        connectionFee1.setOrderN(null);
                        connectionFeeRepository.save(connectionFee1);
                        connectionFeeRepository.saveAll(feeToAdd);
                    } else {
                        throw new Exception("Sum of elements must not be greater than parent amount");
                    }
                } else {
                    throw new Exception("Sum of array must be greater than 0");
                }
            } else {
                throw new Exception("Sum of elements must be a floating-point number");
            }
        } else {
            throw new ResourceNotFoundException("ConnectionFee not found with id: " + feeId);
        }
    }

    @Override
    public List<ConnectionFeeChildrenDTO> getFeesByParent(Long id) {
        Optional<ConnectionFee> connectionFee = connectionFeeRepository.findById(id);
        if (connectionFee.isPresent()) {
            List<ConnectionFee> fees = connectionFeeRepository.findAllDescendants(id);

            return fees.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        } else {
            throw new ResourceNotFoundException("ConnectionFee not found with id: " + id);
        }
    }

    @Override
    public List<ConnectionFee> getDownloadDataBySpec(Specification<ConnectionFee> spec) {
        return connectionFeeRepository.findAll(spec);
    }

    private ConnectionFeeChildrenDTO convertToDto(ConnectionFee connectionFee) {
        ConnectionFeeChildrenDTO dto = new ConnectionFeeChildrenDTO();
        dto.setId(connectionFee.getId());
        dto.setOrderN(connectionFee.getOrderN());
        dto.setRegion(connectionFee.getRegion());
        dto.setServiceCenter(connectionFee.getServiceCenter());
        dto.setProjectID(connectionFee.getProjectID());
        dto.setWithdrawType(connectionFee.getWithdrawType());
        dto.setClarificationDate(connectionFee.getClarificationDate());
        dto.setChangeDate(connectionFee.getChangeDate());
        dto.setTransferDate(connectionFee.getTransferDate());
        dto.setExtractionId(connectionFee.getExtractionId());
        dto.setNote(connectionFee.getNote());
        dto.setExtractionDate(connectionFee.getExtractionDate());
        dto.setTotalAmount(connectionFee.getTotalAmount());
        dto.setPurpose(connectionFee.getPurpose());
        dto.setDescription(connectionFee.getDescription());
        dto.setTax(connectionFee.getTax());

        if (connectionFee.getChildren() != null) {
            dto.setChildren(connectionFee.getChildren().stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    private String getCellValue(ConnectionFee connectionFee, String columnName) {
        return switch (columnName) {
            case "ID" -> String.valueOf(connectionFee.getId());
            case "ორდერის N" -> connectionFee.getOrderN();
            case "რეგიონი" -> connectionFee.getRegion();
            case "სერვის ცენტრი" -> connectionFee.getServiceCenter();
            case "პროექტის ნომერი" -> connectionFee.getProjectID();

            case "გარკვევის თარიღი" -> connectionFee.getClarificationDate() != null ?
                    connectionFee.getClarificationDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    : "";

            case "შეცვლის თარიღი" ->
                    connectionFee.getChangeDate() != null ? connectionFee.getChangeDate().toString() : "";

            case "შენიშვნა" -> connectionFee.getNote();

            case "დაბრუნების თარიღი" ->
                    connectionFee.getTreasuryRefundDate() != null ? connectionFee.getTreasuryRefundDate().toString() : "";

            case "გადმოტანის თარიღი" ->
                    connectionFee.getTransferDate() != null ? connectionFee.getTransferDate().toString() : "";

            case "ჩარიცხვის თარიღი" ->
                    connectionFee.getExtractionDate() != null ? connectionFee.getExtractionDate().toString() : "";

            case "თანხა" -> connectionFee.getTotalAmount() != null ? connectionFee.getTotalAmount().toString() : "";

            case "გადამხდელის იდენტიფიკატორი" -> connectionFee.getTax();
            case "მიზანი" -> connectionFee.getPurpose();
            case "აღწერა" -> connectionFee.getDescription();

            case "შემცვლელი" -> connectionFee.getChangePerson() != null
                    ? connectionFee.getChangePerson().getLastName() + " " + connectionFee.getChangePerson().getFirstName()
                    : "";

            case "მშობელი" -> connectionFee.getParent() != null ? connectionFee.getParent().getId().toString() : "";

            case "ტიპი" -> connectionFee.getWithdrawType() != null ? connectionFee.getWithdrawType() : "";

            case "თანხის დაბრუნების მოთხოვნის თარიღი" -> connectionFee.getPaymentOrderSentDate() != null ?
                    connectionFee.getPaymentOrderSentDate().toString() : "";

            default -> "";
        };
    }

    @Override
    public PagedModel<?> letDoFilter(Specification<ConnectionFee> spec, int page, int size, String sortBy, String sortDir) {
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(sortDir);
        } catch (IllegalArgumentException | NullPointerException e) {
            direction = Sort.Direction.ASC;
        }
        Sort sort = Sort.by(direction, sortBy);
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        Page<ConnectionFee> pg = connectionFeeRepository.findAll(spec, pageRequest);
        return new PagedModel<>(castToDtos(
                pg
        ));
    }

    private Page<ConnectionFeeResponseDto> castToDtos(Page<ConnectionFee> page) {
        List<ConnectionFeeResponseDto> cfDtos = page.getContent()
                .stream()
                .map(this::castToDto)
                .collect(Collectors.toList());

        return new PageImpl<>(cfDtos, page.getPageable(), page.getTotalElements());
    }

    private ConnectionFeeResponseDto castToDto(ConnectionFee cf) {
        ConnectionFeeResponseDto cfd = baseCast(cf);
        List<ConnectionFeeResponseDto> emptyDto = new ArrayList<>();
        List<ConnectionFeeResponseDto> cfDtos = new ArrayList<>();
        if (!cf.getChildren().isEmpty()) {
            ConnectionFeeResponseDto cfChild1;
            for (ConnectionFee cfChild : cf.getChildren()) {
                cfChild1 = baseCast(cfChild);
                cfChild1.setChildren(emptyDto);
                cfDtos.add(cfChild1);
            }
        }
        cfd.setChildren(cfDtos);
        return cfd;
    }

    private ConnectionFeeResponseDto baseCast(ConnectionFee cf) {
        ConnectionFeeResponseDto cfdto = ConnectionFeeResponseDto.builder()
                .id(cf.getId())
                .orderStatus(cf.getOrderStatus())
                .status(cf.getStatus())
                .orderN(cf.getOrderN())
                .region(cf.getRegion())
                .serviceCenter(cf.getServiceCenter())
                .queueNumber(cf.getQueueNumber())
                .projectID(cf.getProjectID())
                .withdrawType(cf.getWithdrawType())
                .paymentOrderSentDateStatus(cf.getPaymentOrderSentDateStatus())
                .clarificationDate(cf.getClarificationDate())
                .treasuryRefundDate(cf.getTreasuryRefundDate())
                .paymentOrderSentDate(cf.getPaymentOrderSentDate())
                .canceledOrders(cf.getCanceledOrders())
                .canceledProject(cf.getCanceledProject())
                .changeDate(cf.getChangeDate())
                .transferDate(cf.getTransferDate())
                .extractionDate(cf.getExtractionDate())
                .extractionTask(cf.getExtractionTask())
                .totalAmount(cf.getTotalAmount())
                .purpose(cf.getPurpose())
                .description(cf.getDescription())
                .tax(cf.getTax())
                .transferPerson(castUserToDto(cf.getTransferPerson()))
                .changePerson(castUserToDto(cf.getChangePerson()))
                .note(cf.getNote())
                .historyId(cf.getHistoryId())
                .build();
        return cfdto;
    }

    private UserResponseDto castUserToDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Override
    public Integer uploadHistory(MultipartFile file) throws IOException {
        log.info("Starting history upload for file: {} (by {})", file.getOriginalFilename(), getCurrentUsername());
        LocalDateTime today = LocalDateTime.now();
        ExtractionTask task;
        try {
            task = extractionTaskRepository.save(new ExtractionTask(today, file.getOriginalFilename(), FileStatus.HISTORY));
        } catch (Exception e) {
            log.error("Failed to create extraction task for history upload: {} (by {})", e.getMessage(), getCurrentUsername(), e);
            return 0;
        }

        Map<Integer, String> PAYMENT_MAPPING = new HashMap<>();
        PAYMENT_MAPPING.put(1, "1");
        PAYMENT_MAPPING.put(2, "2");
        PAYMENT_MAPPING.put(3, "3");
        PAYMENT_MAPPING.put(4, "4");
        PAYMENT_MAPPING.put(5, "5 ");
        PAYMENT_MAPPING.put(6, "6");
        PAYMENT_MAPPING.put(7, "7");
        PAYMENT_MAPPING.put(8, "8");
        PAYMENT_MAPPING.put(9, "9");
        PAYMENT_MAPPING.put(10, "10");
        PAYMENT_MAPPING.put(19, "19");
        PAYMENT_MAPPING.put(11, "11");
        PAYMENT_MAPPING.put(12, "12");
        PAYMENT_MAPPING.put(13, "13");
        PAYMENT_MAPPING.put(14, "14");
        PAYMENT_MAPPING.put(15, "15");

        Map<String, OrderStatus> ORDER_STATUS_MAPPING = new HashMap<>();
        ORDER_STATUS_MAPPING.put("გაუქმებული", OrderStatus.CANCELED);
        ORDER_STATUS_MAPPING.put("დასასრულებელი", OrderStatus.ORDER_INCOMPLETE);
        ORDER_STATUS_MAPPING.put("შევსებული", OrderStatus.ORDER_COMPLETE);
        //ORDER_STATUS_MAPPING.put("შესავსები",OrderStatus.YELLOW_AMOUNT);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User userDetails = (User) authentication.getPrincipal();

        List<ConnectionFee> connectionFees = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        int rowNum = 0;
        int errorCounter = 0;
        List<Long> erList = new ArrayList<>();
        try {
            Workbook workbook = new XSSFWorkbook(file.getInputStream());
            Sheet sheet = workbook.getSheetAt(0);

            int totalRows = 0;
            for (int i = 2; i <= sheet.getLastRowNum(); i++) { // Start from row 2 to skip headers
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // Skip phantom rows — POI tracks rows touched by formulas even if they have no data
                Cell amountCell = row.getCell(7, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                Cell idCell = row.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                if (idCell == null && amountCell == null) continue;
                if (idCell != null && idCell.getCellType() == CellType.BLANK
                        && (amountCell == null || amountCell.getCellType() == CellType.BLANK)) continue;

                totalRows++;
                ConnectionFee fee = new ConnectionFee();
                try {

                    fee.setHistoryId(getLongCellValue(row.getCell(0)));//1 აიდი
                    fee.setOrderN(getStringCellValue(row.getCell(1)));//2 ორდეირს ნომერი
                    fee.setRegion(getStringCellValue(row.getCell(2)));//3 რეგიონი
                    fee.setServiceCenter(getStringCellValue(row.getCell(3)));//4 მომსახურების ცენტრი
                    fee.setProjectID(getStringCellValue(row.getCell(4)));//5 პროექტის ნომერი
                    try {
                        Integer paymentType = (int) row.getCell(5).getNumericCellValue();//6 ტიპი
                        fee.setWithdrawType(PAYMENT_MAPPING.getOrDefault(paymentType, row.getCell(5).toString()));
                    } catch (Exception e) {
                        fee.setWithdrawType(row.getCell(5).toString());
                    }

                    //7 თარიღი
                    try {
                        LocalDate extractionDate = null;
                        if (row.getCell(6) != null && !row.getCell(6).toString().trim().isEmpty()) {
                            if (DateUtil.isCellDateFormatted(row.getCell(6))) {
                                extractionDate = row.getCell(6).getLocalDateTimeCellValue().toLocalDate();
                            } else {
                                try {
                                    extractionDate = LocalDate.parse(row.getCell(6).toString(), formatter);
                                } catch (Exception dateEx) {
                                    // Log specific error for invalid date format
                                    logRowError(row, 6, dateEx, "extractionDate");
                                    extractionDate = null; // Set to null if parsing fails
                                }
                            }
                        }

                        // Set extractionDate to the fee object
                        fee.setExtractionDate(extractionDate);

                    } catch (Exception e) {
                        // Catch any other unexpected errors
                        logRowError(row, 6, e, "extractionDate");
                        fee.setExtractionDate(null); // Set to null if there is any error
                    }

                    fee.setTotalAmount(getDoubleCellValue(row.getCell(7)));//8 ბრუნვა
                    fee.setPurpose(getStringCellValue(row.getCell(8)) != null ? getStringCellValue(row.getCell(8)) : " ");//9 დანიშნულება
                    fee.setDescription(getStringCellValue(row.getCell(9))); //10 დამატებითი ინფირმაცია
                    fee.setTax(getStringCellValue(row.getCell(10)));//11ტაქსი
                    fee.setNote(getStringCellValue(row.getCell(12)));// 13 შენიშვნა

                    // 12 Clarification Date
                    try {
                        Cell clarCell = row.getCell(11);
                        if (clarCell != null && !clarCell.toString().trim().isEmpty()) {
                            LocalDate clarificationDate;
                            if (DateUtil.isCellDateFormatted(clarCell)) {
                                clarificationDate = clarCell.getLocalDateTimeCellValue().toLocalDate();
                            } else {
                                clarificationDate = LocalDate.parse(clarCell.toString(), formatter);
                            }
                            fee.setClarificationDate(clarificationDate.atStartOfDay());
                        }
                    } catch (Exception e) {
                        logRowError(row, 11, e, "clarificationDate");
                        fee.setClarificationDate(null);
                    }

                    // 14 თანხის დაბრუნებაზე ხაზინაში მოთხოვნის გაგზავნის თარიღი
                    try {
                        Cell treasuryCell = row.getCell(13);
                        if (treasuryCell != null && !treasuryCell.toString().trim().isEmpty()) {
                            if (DateUtil.isCellDateFormatted(treasuryCell)) {
                                fee.setTreasuryRefundDate(treasuryCell.getLocalDateTimeCellValue().toLocalDate());
                            } else {
                                LocalDate treasuryRefundDate = LocalDate.parse(treasuryCell.toString(), formatter);
                                fee.setTreasuryRefundDate(treasuryRefundDate);
                            }
                        }
                    } catch (Exception e) {
                        logRowError(row, 13, e,
                                "თანხის დაბრუნებაზე ხაზინაში მოთხოვნის გაგზავნის თარიღი: " + row.getCell(13));
                        fee.setPaymentOrderSentDateStatus(row.getCell(13).toString());
                    }
                    // 15 Payment Order Sent Date
                    try {
                        Cell paymentCell = row.getCell(14);
                        if (paymentCell != null && !paymentCell.toString().trim().isEmpty()) {
                            if (DateUtil.isCellDateFormatted(paymentCell)) {
                                fee.setPaymentOrderSentDate(paymentCell.getLocalDateTimeCellValue().toLocalDate());
                            } else if (paymentCell.toString().matches("\\d{2}-[A-Za-z]{3}-\\d{4}")) {
                                fee.setPaymentOrderSentDate(LocalDate.parse(paymentCell.toString(), formatter));
                            } else {
                                fee.setPaymentOrderSentDate(null);
                            }
                        }
                    } catch (Exception e) {
                        logRowError(row, 14, e, "paymentOrderSentDate");
                    }

                    // 18 order status MAPPED 17
                    try {
                        fee.setOrderStatus(ORDER_STATUS_MAPPING.get(row.getCell(17) != null ?
                                row.getCell(17).toString() : "Unknown order status"
                        ));
                    } catch (Exception e) {
                        fee.setOrderStatus(null);
                        logRowError(row, 17, e, "setOrderStatus");
                    }

                    List<String> canceledProjects = new ArrayList<>();
                    //16 გაუქმებული პროექტი
                    canceledProjects.add(getStringCellValue(row.getCell(15)));
                    //16 გაუქმებული პროექტი
                    canceledProjects.add(getStringCellValue(row.getCell(16)));
                    fee.setCanceledProject(canceledProjects);

                    fee.setStatus(Status.TRANSFERRED);
                    fee.setTransferDate(LocalDateTime.now());
                    fee.setExtractionId(0L);
                    fee.setTransferPerson(userDetails);
                    fee.setChangePerson(userDetails);
                    fee.setExtractionTask(task);
                    connectionFees.add(fee);

                } catch (Exception e) {
                    logFullRow(row, e);
                }
            }
            connectionFeeRepository.saveAll(connectionFees);
            int uploaded = connectionFees.size();
            int failed = totalRows - uploaded;
            log.info("Successfully uploaded {}/{} connection fees from history file: {} ({} rows failed/skipped) (by {})",
                    uploaded, totalRows, file.getOriginalFilename(), failed, getCurrentUsername());
            return connectionFees.size();

        } catch (Exception e) {
            log.error("Critical error reading file: {} at row: {} (by {})", file.getOriginalFilename(), rowNum, getCurrentUsername(), e);
            try {
                // Convert List<Long> to List<String>
                List<String> stringList = erList.stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList());

                // Write the list to file
                Files.write(Paths.get("error_log.txt"), stringList, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException ioException) {
                log.error("Failed to write error list to file: {} (by {})", ioException.getMessage(), getCurrentUsername(), ioException);
            }
            return 0;
        }
    }

    @Override
    public List<ConnectionFee> getExportData(Map<String, Object> filters) {
        return connectionFeeCustomRepository.fetchConnectionFeesForExport(filters);
    }

    @Override
    public List<ConnectionFee> getFeeCustom(Map<String, Object> filters) {
        List<ConnectionFee> parents = this.connectionFeeCustomRepository.fetchConnectionFees(filters);
        List<Long> parentIds = parents.stream().map(ConnectionFee::getId).collect(Collectors.toList());
        if (!parentIds.isEmpty()) {
            List<ConnectionFee> children = connectionFeeRepository.findByParentIdIn(parentIds);
            Map<Long, List<ConnectionFee>> childrenByParentId = children.stream()
                    .collect(Collectors.groupingBy(c -> c.getParent().getId()));
            parents.forEach(parent ->
                    parent.setChildren(childrenByParentId.getOrDefault(parent.getId(), Collections.emptyList())));
        }
        return parents;
    }

    @Override
    public PagedModel<?> letDoFilterCustom(Map<String, Object> filters, int page, int size, String sortBy, String sortDir) {
        Page<ConnectionFee> pg = connectionFeeCustomRepository.fetchConnectionFeesPaged(filters, page, size, sortBy, sortDir);

        List<Long> parentIds = pg.getContent().stream()
                .map(ConnectionFee::getId)
                .collect(Collectors.toList());
        if (!parentIds.isEmpty()) {
            List<ConnectionFee> children = connectionFeeRepository.findByParentIdIn(parentIds);
            Map<Long, List<ConnectionFee>> childrenByParentId = children.stream()
                    .collect(Collectors.groupingBy(c -> c.getParent().getId()));
            pg.getContent().forEach(parent ->
                    parent.setChildren(childrenByParentId.getOrDefault(parent.getId(), Collections.emptyList())));
        }

        return new PagedModel<>(castToDtos(pg));
    }

    @Override
    public PagedModel<?> getByTaxPayerId(String taxPayerId, int page, int size) {
        Page<ConnectionFee> fees = connectionFeeRepository.findByTax(taxPayerId, PageRequest.of(page, size));
        Page<ConnectionFeeTaxPayerDto> dtoPage = fees.map(cf -> ConnectionFeeTaxPayerDto.builder()
                .id(cf.getId())
                .region(cf.getRegion())
                .serviceCenter(cf.getServiceCenter())
                .tax(cf.getTax())
                .totalAmount(cf.getTotalAmount())
                .build());
        return new PagedModel<>(dtoPage);
    }

    private void logRowError(Row row, int columnIndex, Exception e, String from) {
        log.warn("Error processing row {} at column {}: {} - ID: {} from: {} (by {})",
                row.getRowNum() + 1, columnIndex + 1, e.getMessage(), row.getCell(0), from, getCurrentUsername());
    }

    private void logFullRow(Row row, Exception e) {
        StringBuilder rowContent = new StringBuilder();
        for (int j = 0; j < row.getLastCellNum(); j++) {
            Cell cell = row.getCell(j, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            rowContent.append("Col ").append(j + 1).append(": ").append(cell.toString()).append(" | ");
        }
        log.error("Error processing row {}: {} - Row content: {} (by {})",
                row.getRowNum() + 1, e.getMessage(), rowContent.toString(), getCurrentUsername(), e);
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (int j = 0; j < row.getLastCellNum(); j++) {
            Cell cell = row.getCell(j, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            if (cell.getCellType() != CellType.BLANK && !cell.toString().trim().isEmpty()) {
                return false;
            }
            if (row.getCell(7) == null || row.getCell(7).toString().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    // Helper Methods
    private static String getStringCellValue(Cell cell) {
        if (cell == null) return null;
        CellType type = cell.getCellType() == CellType.FORMULA
                ? cell.getCachedFormulaResultType() : cell.getCellType();
        switch (type) {
            case STRING:
                return cell.getCellType() == CellType.FORMULA
                        ? cell.getRichStringCellValue().getString()
                        : cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }

    private static Long getLongCellValue(Cell cell) {
        if (cell == null) return null;
        CellType type = cell.getCellType() == CellType.FORMULA
                ? cell.getCachedFormulaResultType() : cell.getCellType();
        if (type == CellType.NUMERIC) {
            return (long) cell.getNumericCellValue();
        } else if (type == CellType.STRING) {
            try {
                String val = cell.getCellType() == CellType.FORMULA
                        ? cell.getRichStringCellValue().getString().trim()
                        : cell.getStringCellValue().trim();
                return Long.parseLong(val);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static Double getDoubleCellValue(Cell cell) {
        if (cell == null) return null;
        CellType type = cell.getCellType() == CellType.FORMULA
                ? cell.getCachedFormulaResultType() : cell.getCellType();
        if (type == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        } else if (type == CellType.STRING) {
            try {
                String val = cell.getCellType() == CellType.FORMULA
                        ? cell.getRichStringCellValue().getString().trim()
                        : cell.getStringCellValue().trim();
                return Double.parseDouble(val);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

}
