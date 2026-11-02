package com.substringtechnologies.jpaadmin.web;

import com.substringtechnologies.jpaadmin.metadata.EntityMetadata;
import com.substringtechnologies.jpaadmin.metadata.JpaMetamodelScanner;
import com.substringtechnologies.jpaadmin.service.AdminActionException;
import com.substringtechnologies.jpaadmin.service.AdminValidationException;
import com.substringtechnologies.jpaadmin.service.DynamicCrudService;
import com.substringtechnologies.jpaadmin.service.PageResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("${spring.jpa.admin.path:/admin}/api")
public class AdminApiController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AdminApiController.class);
    /** Largest page, lookup or batch the API serves in one request; CSV export pages through at this size. */
    public static final int MAX_PAGE_SIZE = 500;
    private static final int MAX_LOOKUP_RESULTS = 100;

    private final com.substringtechnologies.jpaadmin.security.AdminAccess access;
    private final JpaMetamodelScanner scanner;
    private final DynamicCrudService crudService;

    public AdminApiController(JpaMetamodelScanner scanner, DynamicCrudService crudService, com.substringtechnologies.jpaadmin.security.AdminAccess access) {
        this.access = access;
        this.scanner = scanner;
        this.crudService = crudService;
    }

    @GetMapping("/metadata")
    public ResponseEntity<Collection<EntityMetadata>> getMetadata() {
        return ResponseEntity.ok(access.metadata());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(Map.of("counts", crudService.countAll()));
    }

    @GetMapping("/stats/timeline")
    public ResponseEntity<Map<String, Object>> getTimeline(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(crudService.creationTimeline(clamp(days, 1, 365)));
    }

    @GetMapping("/{entity}")
    public ResponseEntity<PageResult<Map<String, Object>>> list(
            @PathVariable String entity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String search,
            @RequestParam Map<String, String> allParams) {

        Sort sorting = Sort.unsorted();
        if (sort != null && !sort.trim().isEmpty()) {
            String[] parts = sort.split(",");
            String prop = parts[0].trim();
            Sort.Direction dir = (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim()))
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            sorting = Sort.by(dir, prop);
        }

        EntityMetadata metadata = scanner.getMetadata(entity);
        int pageSize = size != null ? size : metadata != null ? metadata.getPageSize() : 25;
        Pageable pageable = PageRequest.of(Math.max(0, page), clamp(pageSize, 1, MAX_PAGE_SIZE), sorting);

        String searchQuery = (q != null && !q.trim().isEmpty()) ? q : search;

        // Filter out framework query params
        Map<String, String> filters = new HashMap<>(allParams);
        filters.remove("page");
        filters.remove("size");
        filters.remove("sort");
        filters.remove("q");
        filters.remove("search");

        return ResponseEntity.ok(crudService.listRecords(entity, pageable, searchQuery, filters));
    }

    @GetMapping("/{entity}/{id}")
    public ResponseEntity<Map<String, Object>> get(
            @PathVariable String entity,
            @PathVariable Object id) {
        return ResponseEntity.ok(crudService.getRecord(entity, id));
    }

    @PostMapping("/{entity}")
    public ResponseEntity<Map<String, Object>> create(
            @PathVariable String entity,
            @RequestBody Map<String, Object> payload) {
        return ResponseEntity.status(HttpStatus.CREATED).body(crudService.createRecord(entity, payload));
    }

    @PutMapping("/{entity}/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable String entity,
            @PathVariable Object id,
            @RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(crudService.updateRecord(entity, id, payload));
    }

    @DeleteMapping("/{entity}/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String entity,
            @PathVariable Object id) {
        crudService.deleteRecord(entity, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{entity}/batch-delete")
    public ResponseEntity<Map<String, Object>> batchDelete(
            @PathVariable String entity,
            @RequestBody List<Object> ids) {
        requireBatchSize(ids);
        int count = crudService.batchDelete(entity, ids);
        return ResponseEntity.ok(Map.of("deleted", count));
    }

    @GetMapping("/{entity}/lookup")
    public ResponseEntity<List<Map<String, Object>>> lookup(
            @PathVariable String entity,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String searchProperty,
            @RequestParam(required = false) String labelProperty) {
        return ResponseEntity.ok(crudService.lookup(entity, q, clamp(limit, 1, MAX_LOOKUP_RESULTS), searchProperty, labelProperty));
    }

    @PostMapping("/{entity}/actions/{actionId}")
    public ResponseEntity<Map<String, Object>> executeAction(
            @PathVariable String entity,
            @PathVariable String actionId,
            @RequestBody List<Object> ids) {
        requireBatchSize(ids);
        Object result = crudService.executeAction(entity, actionId, ids);
        return ResponseEntity.ok(Map.of("success", true, "result", result != null ? result : "Action completed successfully"));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private static void requireBatchSize(List<Object> ids) {
        if (ids.size() > MAX_PAGE_SIZE) throw new IllegalArgumentException("Select at most " + MAX_PAGE_SIZE + " records at a time");
    }

    @ExceptionHandler(com.substringtechnologies.jpaadmin.security.AdminAccessDeniedException.class)
    public ResponseEntity<Map<String, String>> denied() {
        return ResponseEntity.status(403).body(Map.of("code", "ACCESS_DENIED", "error", "You do not have permission to perform this operation."));
    }

    @ExceptionHandler(AdminValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(AdminValidationException e) {
        return ResponseEntity.unprocessableEntity().body(Map.of("error", e.getMessage(), "fieldErrors", e.getFieldErrors()));
    }

    /** Database constraint failures (duplicate unique values, missing references) without leaking SQL to the client. */
    @ExceptionHandler({org.springframework.dao.DataIntegrityViolationException.class, org.hibernate.exception.ConstraintViolationException.class})
    public ResponseEntity<Map<String, String>> handleConflict(Exception e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error",
                "This change conflicts with existing data. A value that must be unique may already be in use, or a related record may be missing or still referenced."));
    }

    @ExceptionHandler(jakarta.persistence.PersistenceException.class)
    public ResponseEntity<Map<String, String>> handlePersistence(jakarta.persistence.PersistenceException e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException) return handleConflict(e);
        }
        return handleGeneralError(e);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(AdminActionException.class)
    public ResponseEntity<Map<String, String>> handleAction(AdminActionException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }

    /** Requests the service rejects with a message written for the admin user. */
    @ExceptionHandler({IllegalArgumentException.class, UnsupportedOperationException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage() != null ? e.getMessage() : "The request is not valid."));
    }

    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> handleMalformed(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("error", "The request is not valid."));
    }

    /** Server faults are logged in full; the client gets a generic message, never internal details. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralError(Exception e) {
        if (e instanceof org.springframework.web.ErrorResponse response) {
            // Framework request errors (malformed JSON, wrong method, missing parameter) keep their 4xx status.
            return ResponseEntity.status(response.getStatusCode()).body(Map.of("error", "The request is not valid."));
        }
        log.error("JPA Admin request failed", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Something went wrong on the server. Please try again."));
    }
}
