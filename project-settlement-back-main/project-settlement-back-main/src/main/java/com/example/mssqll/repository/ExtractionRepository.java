package com.example.mssqll.repository;

import com.example.mssqll.models.Extraction;
import com.example.mssqll.models.ExtractionTask;
import com.example.mssqll.models.Status;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExtractionRepository extends JpaRepository<Extraction, Long>, JpaSpecificationExecutor<Extraction> {
    Page<Extraction> findByStatus(Status status, Pageable pageable);

    Page<Extraction> findByExtractionTaskIdAndStatus(Long ExtractionTaskId, Status status, Pageable pageable);

    Page<Extraction> findByExtractionTaskId(Long ExtractionTaskId, Pageable pageable);

    List<Extraction> findByExtractionTask(ExtractionTask extractionTask);

    @Query("SELECT SUM(e.totalAmount) FROM Extraction e WHERE e.extractionTask = :extractionTask")
    Long sumTotalAmountByExtractionTask(@Param("extractionTask") ExtractionTask extractionTask);

    @Query("SELECT SUM(e.totalAmount) FROM Extraction e")
    Long sumTotalAmount();

    Long countAllByStatus(Status status);

    @Query("SELECT SUM(e.totalAmount) FROM Extraction e WHERE e.status = :status")
    Long sumTotalAmountByStatus(@Param("status") Status status);

    Long countAllByExtractionTaskId(Long ExtractionTaskId);

    @Query("SELECT SUM(e.totalAmount) FROM Extraction e WHERE e.extractionTask = :extractionTask AND e.status = :status")
    Long sumTotalAmountByExtractionTaskAndStatus(@Param("extractionTask") ExtractionTask extractionTask, @Param("status") Status status);

    @Query("SELECT COUNT(e) FROM Extraction e WHERE e.extractionTask = :extractionTask AND e.status = 'WARNING'")
    Long countWarningsByFileId(@Param("extractionTask") ExtractionTask extractionTask);

    @Query("SELECT COUNT(e) FROM Extraction e WHERE e.extractionTask = :extractionTask AND e.status = 'GOOD'")
    Long countOkByFileId(@Param("extractionTask") ExtractionTask extractionTask);

    @Transactional
    @Modifying
    @Query("DELETE FROM Extraction e WHERE e.extractionTask = :extractionTask")
    void deleteByExtractionTask(@Param("extractionTask") ExtractionTask extractionTask);

    @Query("SELECT e FROM Extraction e WHERE e.date BETWEEN :startDate AND :endDate")
    Page<Extraction> findAllByDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query("SELECT  COUNT(e) FROM Extraction e WHERE e.date BETWEEN :startDate AND :endDate AND e.status = 'WARNING'")
    Long countWarningsByDate(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT  COUNT(e) FROM Extraction e WHERE e.date BETWEEN :startDate AND :endDate AND e.status = 'GOOD'")
    Long countOksByDate(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(e.totalAmount) FROM Extraction e WHERE e.date BETWEEN :startDate AND :endDate")
    Long sumByDate(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    Page<Extraction> findByTotalAmount(Long totalAmount, Pageable pageable);

    @Query("SELECT e from Extraction e where e.totalAmount = :total AND e.status = :status")
    Page<Extraction> findByTotalAmountAndStatus(@Param("total") Long total, Pageable pageable, @Param("status") Status status);

    @Query("SELECT COUNT(e) from Extraction e where e.totalAmount = :total AND e.status = :status")
    Long countByTotalAmountAndStatus(@Param("total") Long total, @Param("status") Status status);

    @Query("SELECT SUM(e.totalAmount) from Extraction e where e.totalAmount = :total ")
    Long sumByTotalAmount(@Param("total") Long total);

    @Query("SELECT SUM(e.totalAmount) from Extraction e where e.totalAmount = :total AND e.status = :status")
    Long sumByTotalAmountAndStatus(@Param("total") Long total, @Param("status") Status status);

}
