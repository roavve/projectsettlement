# SaveFeeTest — Unit Tests for `ConnectionFeeServiceImpl.saveFee`

## Overview

Unit tests for the `saveFee(Long extractionTask)` method, which transfers `Extraction` rows into `ConnectionFee` records.
Uses **Mockito** (no Spring context) with a manually configured `SecurityContextHolder`.

**File:** `src/test/java/com/example/mssqll/service/impl/SaveFeeTest.java`

---

## Run Commands

```bash
# All tests in the class
mvn test -Dtest=SaveFeeTest

# Individual tests
mvn test -Dtest="SaveFeeTest#saveFee_allRowsTransferred_returnsAllConnectionFees"
mvn test -Dtest="SaveFeeTest#saveFee_saveAllReturnsMissingRows_throwsAndRollsBack"
mvn test -Dtest="SaveFeeTest#saveFee_saveAllThrowsException_throwsAndRollsBack"
mvn test -Dtest="SaveFeeTest#saveFee_alreadyTransferred_throwsFileAlreadyTransferredException"
mvn test -Dtest="SaveFeeTest#saveFee_taskNotFound_throwsResourceNotFoundException"
mvn test -Dtest="SaveFeeTest#saveFee_noExtractions_returnsEmptyList"
```

---

## Test Setup

| Component | Detail |
|-----------|--------|
| Framework | JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`) |
| Mocked | `ConnectionFeeRepository`, `ExtractionRepository`, `ExtractionTaskRepository`, `ConnectionFeeCustomRepository` |
| Auth | `SecurityContextHolder` set manually with `ROLE_ADMIN` user (`test@example.com`) |
| Task fixture | `id=10`, `fileName=test_file.xlsx`, `status=GOOD` |
| Cleanup | `SecurityContextHolder.clearContext()` after each test |

---

## Tests

### 1. `saveFee_allRowsTransferred_returnsAllConnectionFees` — Happy Path

**Scenario:** 3 extractions, all 3 successfully saved to `connection_fees`.

**Input extractions:**

| id  | tax       | amount  | date       | purpose   |
|-----|-----------|---------|------------|-----------|
| 101 | 123456789 | 1500.00 | 2024-01-15 | Purpose A |
| 102 | 987654321 | 2750.50 | 2024-01-16 | Purpose B |
| 103 | 111222333 | 500.00  | 2024-01-17 | Purpose C |

**Mock behaviour:** `saveAll` returns all 3 fees with assigned IDs (200, 201, 202).

**Assertions:**
- Result size = 3
- `extractionId` values are 101, 102, 103
- All fees have `status = TRANSFERRED`
- Tax values match source extractions

**Expected logs:**
```
=== START saveFee: extractionTaskId=10, user=test@example.com ===
Found 3 extractions for taskId=10 (file='test_file.xlsx')
  [1] id=101, tax='123456789', amount=1500.0 ...
  [2] id=102, tax='987654321', amount=2750.5 ...
  [3] id=103, tax='111222333', amount=500.0  ...
ExtractionTask status updated: GOOD -> TRANSFERRED_GOOD (id=10)
  Mapped [1/3]: extractionId=101 ...
  Mapped [2/3]: extractionId=102 ...
  Mapped [3/3]: extractionId=103 ...
All 3 extractions mapped, calling saveAll (taskId=10)
=== END saveFee: saved=3/3 connection fees, taskId=10 ===
```

---

### 2. `saveFee_saveAllReturnsMissingRows_throwsAndRollsBack` — Count Mismatch

**Scenario:** 3 extractions mapped correctly, but the DB silently returns only 2 rows (extraction 103 is missing from the `saveAll` result).

**Mock behaviour:** `saveAll` returns a list without the fee for `extractionId=103`.

**Assertions:** Throws `RuntimeException` with message containing:
- `"Transfer count mismatch"`
- `"expected 3"`
- `"only 2 were saved"`
- `"103"` (the missing extraction ID)

**Expected logs:**
```
ERROR ROLLING BACK — count mismatch: expected=3, saved=2, missingExtractionIds=[103]
      (taskId=10, file='test_file.xlsx', by=test@example.com)
```

---

### 3. `saveFee_saveAllThrowsException_throwsAndRollsBack` — DB Exception

**Scenario:** All 3 rows map correctly, but `saveAll` throws a `RuntimeException` (e.g. DB constraint violation).

**Mock behaviour:** `saveAll` throws `RuntimeException("DB constraint violation on extraction_id")`.

**Assertions:** Throws `RuntimeException` with message containing `"DB constraint violation"`.

**Expected logs:**
```
ERROR ROLLING BACK — saveAll threw exception after mapping 3 fees
      (taskId=10, file='test_file.xlsx', by=test@example.com):
      DB constraint violation on extraction_id
```

---

### 4. `saveFee_alreadyTransferred_throwsFileAlreadyTransferredException` — Double Transfer Guard

**Scenario:** The `ExtractionTask` already has status `TRANSFERRED_GOOD` — attempting to transfer it again.

**Mock behaviour:** Task returned with `status=TRANSFERRED_GOOD`.

**Assertions:** Throws `FileAlreadyTransferredException` with message containing `"already transferred"`.

**Expected logs:**
```
WARN Attempted to transfer already transferred file with ID: 10 status=TRANSFERRED_GOOD
     (by test@example.com)
```

---

### 5. `saveFee_taskNotFound_throwsResourceNotFoundException` — Missing Task

**Scenario:** The provided `extractionTaskId` (99) does not exist in the database.

**Mock behaviour:** `extractionTaskRepository.findById(99L)` returns `Optional.empty()`.

**Assertions:** Throws `ResourceNotFoundException` with message containing `"Extraction task not found"`.

**Expected logs:**
```
ERROR Extraction task not found with ID: 99 (by test@example.com)
```

---

### 6. `saveFee_noExtractions_returnsEmptyList` — Empty Source

**Scenario:** The `ExtractionTask` exists and is valid, but has no associated `Extraction` rows.

**Mock behaviour:** `extractionRepository.findByExtractionTask(task)` returns an empty list.

**Assertions:** Result is an empty list. `saveAll` is never called.

**Expected logs:**
```
Found 0 extractions for taskId=10 (file='test_file.xlsx')
```

---

## Logging Configuration

Tests use `src/test/resources/logback-test.xml` which enables `DEBUG` level only for `ConnectionFeeServiceImpl`, keeping test output focused:

```xml
<logger name="com.example.mssqll.service.impl.ConnectionFeeServiceImpl" level="DEBUG"/>
```

To suppress all logs during a test run, set the level to `OFF` temporarily.
