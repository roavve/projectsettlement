package com.example.mssqll.service.impl;

import com.example.mssqll.models.*;
import com.example.mssqll.repository.ConnectionFeeCustomRepository;
import com.example.mssqll.repository.ConnectionFeeRepository;
import com.example.mssqll.repository.ExtractionRepository;
import com.example.mssqll.repository.ExtractionTaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveFeeTest {

    @Mock
    private ConnectionFeeRepository connectionFeeRepository;
    @Mock
    private ExtractionRepository extractionRepository;
    @Mock
    private ExtractionTaskRepository extractionTaskRepository;
    @Mock
    private ConnectionFeeCustomRepository connectionFeeCustomRepository;

    @InjectMocks
    private ConnectionFeeServiceImpl service;

    private User testUser;
    private ExtractionTask task;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .role(Role.ROLE_ADMIN)
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));

        task = new ExtractionTask();
        task.setId(10L);
        task.setFileName("test_file.xlsx");
        task.setStatus(FileStatus.GOOD);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void saveFee_allRowsTransferred_returnsAllConnectionFees() {
        // 3 source extractions
        List<Extraction> extractions = List.of(
                buildExtraction(101L, "123456789", 1500.00, "Purpose A", "Description A", LocalDate.of(2024, 1, 15)),
                buildExtraction(102L, "987654321", 2750.50, "Purpose B", "Description B", LocalDate.of(2024, 1, 16)),
                buildExtraction(103L, "111222333", 500.00,  "Purpose C", "Description C", LocalDate.of(2024, 1, 17))
        );

        when(extractionTaskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(extractionTaskRepository.save(any())).thenReturn(task);
        when(extractionRepository.findByExtractionTask(task)).thenReturn(extractions);
        when(connectionFeeRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<ConnectionFee> fees = invocation.getArgument(0);
            // Simulate DB assigning IDs
            long idSeq = 200L;
            for (ConnectionFee fee : fees) {
                fee.setId(idSeq++);
            }
            return fees;
        });

        List<ConnectionFee> result = service.saveFee(10L);

        assertThat(result).hasSize(3);
        assertThat(result).extracting(ConnectionFee::getExtractionId)
                .containsExactlyInAnyOrder(101L, 102L, 103L);
        assertThat(result).extracting(ConnectionFee::getStatus)
                .allMatch(s -> s == Status.TRANSFERRED);
        assertThat(result).extracting(ConnectionFee::getTax)
                .containsExactlyInAnyOrder("123456789", "987654321", "111222333");
    }

    // -------------------------------------------------------------------------
    // Failure scenarios
    // -------------------------------------------------------------------------

    @Test
    void saveFee_saveAllReturnsMissingRows_throwsAndRollsBack() {
        // 3 extractions mapped, but DB only saves 2 (row 103 silently dropped)
        List<Extraction> extractions = List.of(
                buildExtraction(101L, "123456789", 1500.00, "Purpose A", "Description A", LocalDate.of(2024, 1, 15)),
                buildExtraction(102L, "987654321", 2750.50, "Purpose B", "Description B", LocalDate.of(2024, 1, 16)),
                buildExtraction(103L, "111222333", 500.00,  "Purpose C", "Description C", LocalDate.of(2024, 1, 17))
        );

        when(extractionTaskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(extractionTaskRepository.save(any())).thenReturn(task);
        when(extractionRepository.findByExtractionTask(task)).thenReturn(extractions);
        when(connectionFeeRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<ConnectionFee> fees = invocation.getArgument(0);
            // Simulate only 2 out of 3 rows persisted (extractionId 103 missing)
            long idSeq = 200L;
            List<ConnectionFee> saved = new java.util.ArrayList<>();
            for (ConnectionFee fee : fees) {
                if (!Long.valueOf(103L).equals(fee.getExtractionId())) {
                    fee.setId(idSeq++);
                    saved.add(fee);
                }
            }
            return saved;
        });

        assertThatThrownBy(() -> service.saveFee(10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Transfer count mismatch")
                .hasMessageContaining("expected 3")
                .hasMessageContaining("only 2 were saved")
                .hasMessageContaining("103"); // missing extractionId logged in message
    }

    @Test
    void saveFee_saveAllThrowsException_throwsAndRollsBack() {
        // saveAll itself throws (e.g. DB constraint violation)
        List<Extraction> extractions = List.of(
                buildExtraction(101L, "123456789", 1500.00, "Purpose A", "Description A", LocalDate.of(2024, 1, 15)),
                buildExtraction(102L, "987654321", 2750.50, "Purpose B", "Description B", LocalDate.of(2024, 1, 16)),
                buildExtraction(103L, "111222333", 500.00,  "Purpose C", "Description C", LocalDate.of(2024, 1, 17))
        );

        when(extractionTaskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(extractionTaskRepository.save(any())).thenReturn(task);
        when(extractionRepository.findByExtractionTask(task)).thenReturn(extractions);
        when(connectionFeeRepository.saveAll(anyList()))
                .thenThrow(new RuntimeException("DB constraint violation on extraction_id"));

        assertThatThrownBy(() -> service.saveFee(10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB constraint violation");
    }

    @Test
    void saveFee_alreadyTransferred_throwsFileAlreadyTransferredException() {
        task.setStatus(FileStatus.TRANSFERRED_GOOD);
        when(extractionTaskRepository.findById(10L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.saveFee(10L))
                .isInstanceOf(com.example.mssqll.utiles.exceptions.FileAlreadyTransferredException.class)
                .hasMessageContaining("already transferred");
    }

    @Test
    void saveFee_taskNotFound_throwsResourceNotFoundException() {
        when(extractionTaskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.saveFee(99L))
                .isInstanceOf(com.example.mssqll.utiles.exceptions.ResourceNotFoundException.class)
                .hasMessageContaining("Extraction task not found");
    }

    @Test
    void saveFee_noExtractions_returnsEmptyList() {
        when(extractionTaskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(extractionRepository.findByExtractionTask(task)).thenReturn(List.of());

        List<ConnectionFee> result = service.saveFee(10L);

        assertThat(result).isEmpty();
    }

    private Extraction buildExtraction(Long id, String tax, Double amount,
                                       String purpose, String description, LocalDate date) {
        Extraction e = new Extraction();
        e.setId(id);
        e.setTax(tax);
        e.setTotalAmount(amount);
        e.setPurpose(purpose);
        e.setDescription(description);
        e.setDate(date);
        e.setExtractionTask(task);
        e.setStatus(Status.TRANSFERRED);
        return e;
    }
}

