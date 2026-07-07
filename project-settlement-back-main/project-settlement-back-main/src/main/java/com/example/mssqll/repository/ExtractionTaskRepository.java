package com.example.mssqll.repository;

import com.example.mssqll.models.ExtractionTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExtractionTaskRepository extends JpaRepository<ExtractionTask, Long> {
     @Query("SELECT e FROM ExtractionTask e WHERE e.fileName LIKE %:fileName%")
     List<ExtractionTask> findByFileName(@Param("fileName") String fileName);
     @Query("SELECT e FROM ExtractionTask e WHERE e.status <> com.example.mssqll.models.FileStatus.SOFT_DELETED order by e.date desc")
     Page<ExtractionTask> findAllByStatusDelete(Pageable pageable);
}
