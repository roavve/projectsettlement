package com.example.mssqll.repository;

import com.example.mssqll.models.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Repository
public class ConnectionFeeCustomRepository {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Map<String, String> SORT_COLUMN_MAP = Map.of(
            "transferDate", "cf.transfer_date",
            "changeDate", "cf.change_date",
            "clarificationDate", "cf.clarification_date",
            "totalAmount", "cf.total_amount",
            "region", "cf.region",
            "orderN", "cf.ordern",
            "status", "cf.status",
            "extractionDate", "cf.extraction_date"
    );

    private static final String BASE_FROM = "FROM connection_fees cf\n " +
            "LEFT JOIN extraction_task et ON cf.extraction_task_id = et.id\n " +
            "LEFT JOIN users tp ON cf.transfer_person = tp.id\n " +
            "LEFT JOIN users cp ON cf.change_person = cp.id\n " +
            "LEFT JOIN connection_fee_canceled_project cfp ON cf.id = cfp.connection_fee_id\n ";

    private static final String GROUP_BY = " GROUP BY cp.last_name, et.id, et.date, cp.id, tp.first_name, tp.last_name, cp.first_name,\n" +
            "         cp.role, cp.created_at, cp.updated_at,\n" +
            "         tp.email, cp.email, et.file_name, et.send_date, cf.id, change_date,\n" +
            "         clarification_date, description, extraction_date, extraction_id, first_withdraw_type,\n" +
            "         history_id, note, ordern, order_status, payment_order_sent_date, payment_order_sent_date_status, project_id, purpose,\n" +
            "         queue_number, region, cf.status, tax_id, total_amount, transfer_date,\n" +
            "         treasury_refund_date, withdraw_type, change_person, extraction_task_id, parent_id, transfer_person,\n" +
            "         service_center, extraction_id, tp.updated_at, tp.created_at, et.status, tp.role, tp.id";

    public List<ConnectionFee> fetchConnectionFeesForExport(Map<String, Object> filters) {
        WhereResult where = buildWhereClause(filters);
        String filterWhereClause = where.clauses.isEmpty() ? "" : "WHERE " + String.join(" AND ", where.clauses);


        StringBuilder sql = new StringBuilder()
                .append("SELECT cf.*, ")
                .append("et.id AS et_id, et.date AS et_date, et.send_date AS et_send_date, et.file_name AS et_file_name, et.status AS et_status,\n ")
                .append("tp.id AS tp_id, tp.first_name AS tp_first_name, tp.last_name AS tp_last_name, tp.email AS tp_email, tp.role AS tp_role, tp.created_at AS tp_created_at, tp.updated_at AS tp_updated_at,\n ")
                .append("cp.id AS cp_id, cp.first_name AS cp_first_name, cp.last_name AS cp_last_name, cp.email AS cp_email, cp.role AS cp_role, cp.created_at AS cp_created_at, cp.updated_at AS cp_updated_at,\n ")
                .append("NULL AS canceled_projects\n ")
                .append("FROM connection_fees cf\n ")
                .append("LEFT JOIN extraction_task et ON cf.extraction_task_id = et.id\n ")
                .append("LEFT JOIN users tp ON cf.transfer_person = tp.id\n ")
                .append("LEFT JOIN users cp ON cf.change_person = cp.id\n ")
                .append(filterWhereClause);

        List<ConnectionFee> fees = jdbcTemplate.query(sql.toString(), where.params.toArray(), connectionFeeRowMapper());
        attachAllCanceledProjects(fees);
        return fees;
    }

    public List<ConnectionFee> fetchConnectionFees(Map<String, Object> filters) {
        WhereResult where = buildWhereClause(filters);
        String filterWhereClause = where.clauses.isEmpty() ? "" : "WHERE " + String.join(" AND ", where.clauses);

        // CTE: find root parent IDs for all rows (parent or child) that match the filter
        String cte = "WITH matching_ids AS (\n" +
                "    SELECT DISTINCT COALESCE(cf.parent_id, cf.id) AS root_id\n" +
                "    " + BASE_FROM +
                "    " + filterWhereClause + "\n" +
                ")\n";

        StringBuilder sql = new StringBuilder(cte)
                .append("SELECT cf.*, ")
                .append("et.id AS et_id, et.date AS et_date, et.send_date AS et_send_date, et.file_name AS et_file_name, et.status AS et_status,\n ")
                .append("tp.id AS tp_id, tp.first_name AS tp_first_name, tp.last_name AS tp_last_name, tp.email AS tp_email, tp.role AS tp_role, tp.created_at AS tp_created_at, tp.updated_at AS tp_updated_at,\n ")
                .append("cp.id AS cp_id, cp.first_name AS cp_first_name, cp.last_name AS cp_last_name, cp.email AS cp_email, cp.role AS cp_role, cp.created_at AS cp_created_at, cp.updated_at AS cp_updated_at,\n ")
                .append("STRING_AGG(CONVERT(VARCHAR, cfp.canceled_project), ', ') AS canceled_projects\n ")
                .append(BASE_FROM)
                .append("WHERE cf.id IN (SELECT root_id FROM matching_ids) AND cf.status != 'SOFT_DELETED'\n")
                .append(GROUP_BY);

        return jdbcTemplate.query(sql.toString(), where.params.toArray(), connectionFeeRowMapper());
    }

    public Page<ConnectionFee> fetchConnectionFeesPaged(Map<String, Object> filters, int page, int size, String sortBy, String sortDir) {
        WhereResult where = buildWhereClause(filters);
        String filterWhereClause = where.clauses.isEmpty() ? "" : "WHERE " + String.join(" AND ", where.clauses);

        String cte = "WITH matching_ids AS (\n" +
                "    SELECT DISTINCT COALESCE(cf.parent_id, cf.id) AS root_id\n" +
                "    " + BASE_FROM +
                "    " + filterWhereClause + "\n" +
                ")\n";

        String countSql = cte + "SELECT COUNT(*) FROM matching_ids";
        Integer totalCount = jdbcTemplate.queryForObject(countSql, Integer.class, where.params.toArray());
        long total = totalCount != null ? totalCount : 0;

        String sortColumn = SORT_COLUMN_MAP.getOrDefault(sortBy, "cf.transfer_date");
        String direction = "ASC".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";

        // canceled_project is fetched separately (see attachCanceledProjects).
        // Aggregating it here forced a GROUP BY over ~45 columns, which made SQL Server
        // sort all 234k rows before taking 20 — measured at ~1.7s of the ~2.6s total.
        StringBuilder sql = new StringBuilder(cte)
                .append("SELECT cf.*, ")
                .append("et.id AS et_id, et.date AS et_date, et.send_date AS et_send_date, et.file_name AS et_file_name, et.status AS et_status,\n ")
                .append("tp.id AS tp_id, tp.first_name AS tp_first_name, tp.last_name AS tp_last_name, tp.email AS tp_email, tp.role AS tp_role, tp.created_at AS tp_created_at, tp.updated_at AS tp_updated_at,\n ")
                .append("cp.id AS cp_id, cp.first_name AS cp_first_name, cp.last_name AS cp_last_name, cp.email AS cp_email, cp.role AS cp_role, cp.created_at AS cp_created_at, cp.updated_at AS cp_updated_at,\n ")
                .append("NULL AS canceled_projects\n ")
                .append("FROM connection_fees cf\n ")
                .append("LEFT JOIN extraction_task et ON cf.extraction_task_id = et.id\n ")
                .append("LEFT JOIN users tp ON cf.transfer_person = tp.id\n ")
                .append("LEFT JOIN users cp ON cf.change_person = cp.id\n ")
                .append("WHERE cf.id IN (SELECT root_id FROM matching_ids) AND cf.status != 'SOFT_DELETED'\n")
                .append(" ORDER BY ").append(sortColumn).append(" ").append(direction)
                .append(" OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

        List<Object> pageParams = new ArrayList<>(where.params);
        pageParams.add((long) page * size);
        pageParams.add(size);

        List<ConnectionFee> fees = jdbcTemplate.query(sql.toString(), pageParams.toArray(), connectionFeeRowMapper());
        attachCanceledProjects(fees);
        return new PageImpl<>(fees, PageRequest.of(page, size), total);
    }
    private void attachCanceledProjects(List<ConnectionFee> fees) {
        if (fees.isEmpty()) return;

        String inClause = String.join(",", Collections.nCopies(fees.size(), "?"));
        String sql = "SELECT connection_fee_id, canceled_project FROM connection_fee_canceled_project " +
                "WHERE connection_fee_id IN (" + inClause + ")";

        Object[] ids = fees.stream().map(ConnectionFee::getId).toArray();

        Map<Long, List<String>> byFeeId = new HashMap<>();
        jdbcTemplate.query(sql, ids, rs -> {
            byFeeId.computeIfAbsent(rs.getLong("connection_fee_id"), k -> new ArrayList<>())
                    .add(rs.getString("canceled_project"));
        });

        for (ConnectionFee fee : fees) {
            fee.setCanceledProject(byFeeId.getOrDefault(fee.getId(), Collections.emptyList()));
        }
    }
    private WhereResult buildWhereClause(Map<String, Object> filters) {
        WhereResult result = new WhereResult();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            if (val == null) continue;

            switch (key) {
                case "orderN":
                    if (val instanceof List<?> list) {
                        List<String> parts = new ArrayList<>();
                        for (Object item : list) {
                            parts.add("cf.ordern LIKE ?");
                            result.params.add("%" + item + "%");
                        }
                        if (!parts.isEmpty()) result.clauses.add("(" + String.join(" OR ", parts) + ")");
                    } else {
                        result.clauses.add("cf.ordern LIKE ?");
                        result.params.add("%" + val + "%");
                    }
                    break;
                case "region":
                    result.clauses.add("cf.region = ?");
                    result.params.add(val.toString());
                    break;
                case "serviceCenter":
                    result.clauses.add("cf.service_center = ?");
                    result.params.add(val.toString());
                    break;
                case "projectID":
                    result.clauses.add("cf.project_id LIKE ?");
                    result.params.add("%" + val + "%");
                    break;
                case "withdrawType":
                    if (val instanceof List<?> list) {
                        List<String> parts = new ArrayList<>();
                        boolean hasNull = false;
                        for (Object item : list) {
                            if (item == null || "null".equalsIgnoreCase(item.toString())) {
                                hasNull = true;
                            } else {
                                parts.add("cf.withdraw_type LIKE ?");
                                result.params.add(item.toString());
                            }
                        }
                        if (hasNull) parts.add("cf.withdraw_type IS NULL");
                        if (!parts.isEmpty()) result.clauses.add("(" + String.join(" OR ", parts) + ")");
                    } else {
                        result.clauses.add("cf.withdraw_type LIKE ?");
                        result.params.add("%" + val + "%");
                    }
                    break;
                case "purpose":
                    result.clauses.add("cf.purpose LIKE ?");
                    result.params.add("%" + val + "%");
                    break;
                case "description":
                    result.clauses.add("cf.description LIKE ?");
                    result.params.add("%" + val + "%");
                    break;
                case "note":
                    result.clauses.add("cf.note LIKE ?");
                    result.params.add("%" + val + "%");
                    break;
                case "tax":
                    result.clauses.add("cf.tax_id LIKE ?");
                    result.params.add("%" + val + "%");
                    break;
                case "status":
                    result.clauses.add("cf.status = ?");
                    result.params.add(val.toString());
                    break;
                case "clarificationDateStart":
                    result.clauses.add("cf.clarification_date >= ?");
                    result.params.add(LocalDateTime.parse(val.toString().replace("T", " "), formatter));
                    break;
                case "clarificationDateEnd":
                    result.clauses.add("cf.clarification_date <= ?");
                    result.params.add(LocalDateTime.parse(val.toString().replace("T", " "), formatter));
                    break;
                case "changeDateStart":
                    result.clauses.add("cf.change_date >= ?");
                    result.params.add(LocalDateTime.parse(val.toString(), formatter));
                    break;
                case "changeDateEnd":
                    result.clauses.add("cf.change_date <= ?");
                    result.params.add(LocalDateTime.parse(val.toString(), formatter));
                    break;
                case "transferDateStart":
                    result.clauses.add("cf.transfer_date >= ?");
                    result.params.add(LocalDateTime.parse(val.toString(), formatter));
                    break;
                case "transferDateEnd":
                    result.clauses.add("cf.transfer_date <= ?");
                    result.params.add(LocalDateTime.parse(val.toString(), formatter));
                    break;
                case "extractionDateStart":
                    result.clauses.add("cf.extraction_date >= ?");
                    result.params.add(LocalDate.parse(val.toString()));
                    break;
                case "extractionDateEnd":
                    result.clauses.add("cf.extraction_date <= ?");
                    result.params.add(LocalDate.parse(val.toString()));
                    break;
                case "totalAmountStart":
                    result.clauses.add("cf.total_amount >= ?");
                    result.params.add(Double.parseDouble(val.toString()));
                    break;
                case "totalAmountEnd":
                    result.clauses.add("cf.total_amount <= ?");
                    result.params.add(Double.parseDouble(val.toString()));
                    break;
                case "orderStatus":
                    result.clauses.add("cf.order_status = ?");
                    try {
                        OrderStatus status = OrderStatus.valueOf(val.toString().toUpperCase());
                        result.params.add(status.ordinal());
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid orderStatus: " + val);
                    }
                    break;
                case "id":
                    result.clauses.add("cf.id = ?");
                    result.params.add(Long.parseLong(val.toString()));
                    break;
                case "extractionId":
                    result.clauses.add("cf.extraction_id = ?");
                    result.params.add(Long.parseLong(val.toString()));
                    break;
                case "history":
                    result.clauses.add("cf.history_id = ?");
                    result.params.add(Long.parseLong(val.toString()));
                    break;
                case "file":
                    result.clauses.add("et.file_name LIKE ?");
                    result.params.add("%" + val + "%");
                    break;
                case "change_person":
                    result.clauses.add("cf.change_person = ?");
                    result.params.add(val.toString());
                    break;
                case "download":
                    result.clauses.add("cf.status != 'REMINDER'");
                    break;
            }
        }

        if (!filters.containsKey("status")) {
            result.clauses.add("cf.status != 'SOFT_DELETED'");
        }

        return result;
    }

    private static class WhereResult {
        final List<String> clauses = new ArrayList<>();
        final List<Object> params = new ArrayList<>();
    }

    private RowMapper<ConnectionFee> connectionFeeRowMapper() {
        return (rs, rowNum) -> {
            ConnectionFee fee = new ConnectionFee();
            fee.setId(rs.getLong("id"));
            int orderStatusOrdinal = rs.getInt("order_status");
            fee.setOrderStatus(rs.wasNull() ? null : safeFromOrdinal(orderStatusOrdinal));
            fee.setStatus(Status.valueOf(rs.getString("status")));
            fee.setOrderN(rs.getString("orderN"));
            fee.setRegion(rs.getString("region"));
            fee.setServiceCenter(rs.getString("service_center"));
            fee.setQueueNumber(rs.getString("queue_number"));
            fee.setProjectID(rs.getString("project_id"));
            fee.setWithdrawType(rs.getString("withdraw_type"));
            fee.setClarificationDate(getLocalDateTime(rs, "clarification_date"));
            fee.setChangeDate(getLocalDateTime(rs, "change_date"));
            fee.setTransferDate(getLocalDateTime(rs, "transfer_date"));
            fee.setExtractionId(getNullableLong(rs, "extraction_id"));
            fee.setNote(rs.getString("note"));
            fee.setExtractionDate(getLocalDate(rs, "extraction_date"));
            fee.setTotalAmount(rs.getDouble("total_amount"));
            fee.setPurpose(rs.getString("purpose"));
            fee.setDescription(rs.getString("description"));
            fee.setTax(rs.getString("tax_id"));
            fee.setHistoryId(getNullableLong(rs, "history_id"));
            fee.setTreasuryRefundDate(getLocalDate(rs, "treasury_refund_date"));
            fee.setPaymentOrderSentDate(getLocalDate(rs, "payment_order_sent_date"));
            String canceledProjectsStr = rs.getString("canceled_projects");
            fee.setCanceledProject(canceledProjectsStr != null
                    ? List.of(canceledProjectsStr.split(","))
                    : Collections.emptyList());
            // ExtractionTask
            if (rs.getObject("et_id") != null) {
                ExtractionTask task = new ExtractionTask();
                task.setId(rs.getLong("et_id"));
                task.setDate(getLocalDateTime(rs, "et_date"));
                task.setSendDate(getLocalDateTime(rs, "et_send_date"));
                task.setFileName(rs.getString("et_file_name"));
                task.setStatus(FileStatus.valueOf(rs.getString("et_status")));
                fee.setExtractionTask(task);
            }

            // Transfer Person
            if (rs.getObject("tp_id") != null) {
                User transferPerson = new User();
                transferPerson.setId(rs.getLong("tp_id"));
                transferPerson.setFirstName(rs.getString("tp_first_name"));
                transferPerson.setLastName(rs.getString("tp_last_name"));
                transferPerson.setEmail(rs.getString("tp_email"));
                transferPerson.setRole(Role.valueOf(rs.getString("tp_role")));
                transferPerson.setCreatedAt(getLocalDateTime(rs, "tp_created_at"));
                transferPerson.setUpdatedAt(getLocalDateTime(rs, "tp_updated_at"));
                fee.setTransferPerson(transferPerson);
            }

            // Change Person
            if (rs.getObject("cp_id") != null) {
                User changePerson = new User();
                changePerson.setId(rs.getLong("cp_id"));
                changePerson.setFirstName(rs.getString("cp_first_name"));
                changePerson.setLastName(rs.getString("cp_last_name"));
                changePerson.setEmail(rs.getString("cp_email"));
                changePerson.setRole(Role.valueOf(rs.getString("cp_role")));
                changePerson.setCreatedAt(getLocalDateTime(rs, "cp_created_at"));
                changePerson.setUpdatedAt(getLocalDateTime(rs, "cp_updated_at"));
                fee.setChangePerson(changePerson);
            }

            // Default empty lists
            fee.setCanceledOrders(Collections.emptyList());
            fee.setChildren(Collections.emptyList());

            return fee;
        };
    }

    private LocalDateTime getLocalDateTime(ResultSet rs, String columnLabel) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnLabel);
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
    private void attachAllCanceledProjects(List<ConnectionFee> fees) {
        if (fees.isEmpty()) return;

        Map<Long, List<String>> byFeeId = new HashMap<>();
        jdbcTemplate.query(
                "SELECT connection_fee_id, canceled_project FROM connection_fee_canceled_project",
                rs -> {
                    byFeeId.computeIfAbsent(rs.getLong("connection_fee_id"), k -> new ArrayList<>())
                            .add(rs.getString("canceled_project"));
                });

        for (ConnectionFee fee : fees) {
            fee.setCanceledProject(byFeeId.getOrDefault(fee.getId(), Collections.emptyList()));
        }
    }
    private LocalDate getLocalDate(ResultSet rs, String columnLabel) throws SQLException {
        Date date = rs.getDate(columnLabel);
        return date != null ? date.toLocalDate() : null;
    }

    private Long getNullableLong(ResultSet rs, String columnLabel) throws SQLException {
        Object value = rs.getObject(columnLabel);
        return value != null ? ((Number) value).longValue() : null;
    }

    private static OrderStatus safeFromOrdinal(int ordinal) {
        OrderStatus[] values = OrderStatus.values();
        return (ordinal >= 0 && ordinal < values.length) ? values[ordinal] : null;
    }

    private static OrderStatus safeFromName(String name) {
        if (name == null || name.isBlank()) return null;
        try {
            return OrderStatus.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Setter
    @Getter
    public static class CanceledProject {
        private Long id;
        private String projectNumber;
    }
}
