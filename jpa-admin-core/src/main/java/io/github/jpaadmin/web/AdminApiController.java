package io.github.jpaadmin.web;

import io.github.jpaadmin.metadata.EntityMetadata;
import io.github.jpaadmin.metadata.JpaMetamodelScanner;
import io.github.jpaadmin.service.DynamicCrudService;
import io.github.jpaadmin.service.PageResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/admin/api")
public class AdminApiController {

    private final JpaMetamodelScanner scanner;
    private final DynamicCrudService crudService;

    public AdminApiController(JpaMetamodelScanner scanner, DynamicCrudService crudService) {
        this.scanner = scanner;
        this.crudService = crudService;
    }

    @GetMapping("/metadata")
    public ResponseEntity<Collection<EntityMetadata>> getMetadata() {
        return ResponseEntity.ok(scanner.getAllMetadata());
    }

    @GetMapping("/{entity}")
    public ResponseEntity<PageResult<Map<String, Object>>> list(
            @PathVariable String entity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
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

        Pageable pageable = PageRequest.of(page, size, sorting);

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
        int count = crudService.batchDelete(entity, ids);
        return ResponseEntity.ok(Map.of("deleted", count));
    }

    @GetMapping("/{entity}/lookup")
    public ResponseEntity<List<Map<String, Object>>> lookup(
            @PathVariable String entity,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(crudService.lookup(entity, q, limit));
    }

    @PostMapping("/{entity}/actions/{actionId}")
    public ResponseEntity<Map<String, Object>> executeAction(
            @PathVariable String entity,
            @PathVariable String actionId,
            @RequestBody List<Object> ids) {
        Object result = crudService.executeAction(entity, actionId, ids);
        return ResponseEntity.ok(Map.of("success", true, "result", result != null ? result : "Action completed successfully"));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralError(Exception e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage() != null ? e.getMessage() : "An error occurred"));
    }
}
