import React, { useState, useEffect, useRef } from 'react';
import { useAdmin } from '../context/AdminContext';
import { fetchRecords, deleteRecord, batchDeleteRecords, executeAction } from '../api/client';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Card } from '../components/ui/card';
import {
  Table,
  TableHeader,
  TableBody,
  TableHead,
  TableRow,
  TableCell,
} from '../components/ui/table';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '../components/ui/dropdown-menu';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '../components/ui/dialog';
import {
  Loader2,
  Plus,
  Search,
  RefreshCw,
  Trash2,
  Edit2,
  ArrowUpDown,
  ArrowUp,
  ArrowDown,
  Filter,
  CheckSquare,
  Square,
  AlertTriangle,
  Zap,
  MoreHorizontal,
  X,
} from 'lucide-react';
import { FieldSelect } from '../components/ui/field-select';
import { TableSkeleton } from '../components/LoadingSkeletons';
import { cn } from '../lib/utils';

export const EntityListView: React.FC = () => {
  const { currentEntity, navigateToCreate, navigateToEdit, addToast } = useAdmin();

  if (!currentEntity) return null;

  const [records, setRecords] = useState<Record<string, any>[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(currentEntity.pageSize || 25);
  const [sortField, setSortField] = useState(currentEntity.defaultSortBy || 'id');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>(
    currentEntity.defaultSortDir?.toLowerCase() === 'asc' ? 'asc' : 'desc'
  );
  const [searchQuery, setSearchQuery] = useState('');
  const [filters, setFilters] = useState<Record<string, string>>({});
  const [showFilterDrawer, setShowFilterDrawer] = useState(false);
  const [loading, setLoading] = useState(true);

  const [pending, setPending] = useState(false);
  const mutationInFlight = useRef(false);

  // Selection for bulk actions
  const [selectedIds, setSelectedIds] = useState<any[]>([]);

  // Modals
  const [deleteModalId, setDeleteModalId] = useState<any | null>(null);
  const [bulkDeleteModalOpen, setBulkDeleteModalOpen] = useState(false);
  const [actionConfirm, setActionConfirm] = useState<{ actionId: string; name: string; confirmMsg?: string; recordId?: any } | null>(null);

  const listFields = currentEntity.fields.filter(f => f.inList);
  const filterableFields = currentEntity.fields.filter(f => f.filterable);

  const requestVersion = useRef(0);
  const loadData = async () => {
    const version = ++requestVersion.current;
    try {
      setLoading(true);
      const sortParam = `${sortField},${sortDir}`;
      const res = await fetchRecords(currentEntity.entityName, page, pageSize, sortParam, searchQuery, filters);
      if (version !== requestVersion.current) return;
      if (page > 0 && page * pageSize >= res.totalElements) { setPage(Math.max(0, Math.ceil(res.totalElements / pageSize) - 1)); return; }
      setRecords(res.content || []);
      setTotalElements(res.totalElements || 0);
      setSelectedIds([]);
    } catch (err: any) {
      if (version !== requestVersion.current) return;
      setRecords([]);
      setTotalElements(0);
      addToast(err.message || 'Failed to load records', 'error');
    } finally {
      if (version === requestVersion.current) setLoading(false);
    }
  };

  useEffect(() => {
    setPage(0);
  }, [currentEntity.entityName, searchQuery, filters, pageSize]);

  useEffect(() => {
    setLoading(true);
    setSelectedIds([]);
    const timer = window.setTimeout(loadData, 200);
    return () => { window.clearTimeout(timer); requestVersion.current++; };
  }, [currentEntity.entityName, page, pageSize, sortField, sortDir, searchQuery, filters]);

  const handleSort = (fieldName: string) => {
    if (sortField === fieldName) {
      setSortDir(prev => (prev === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortField(fieldName);
      setSortDir('asc');
    }
  };

  const handleSelectAll = () => {
    if (selectedIds.length === records.length) {
      setSelectedIds([]);
    } else {
      const pk = currentEntity.primaryKeyField;
      setSelectedIds(records.map(r => r[pk]));
    }
  };

  const handleToggleSelect = (id: any) => {
    setSelectedIds(prev => (prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]));
  };

  const confirmSingleDelete = async () => {
    if (deleteModalId == null) return;
    if (mutationInFlight.current) return;
    mutationInFlight.current = true;
    setPending(true);
    try {
      await deleteRecord(currentEntity.entityName, deleteModalId);
      addToast('Record deleted successfully');
      setDeleteModalId(null);
      loadData();
    } catch (err: any) {
      addToast(err.message || 'Failed to delete record', 'error');
    } finally {
      mutationInFlight.current = false;
      setPending(false);
    }
  };

  const confirmBulkDelete = async () => {
    if (mutationInFlight.current) return;
    mutationInFlight.current = true;
    setPending(true);
    try {
      const count = await batchDeleteRecords(currentEntity.entityName, selectedIds);
      addToast(`Successfully deleted ${count} record(s)`);
      setBulkDeleteModalOpen(false);
      setSelectedIds([]);
      loadData();
    } catch (err: any) {
      addToast(err.message || 'Batch delete failed', 'error');
    } finally {
      mutationInFlight.current = false;
      setPending(false);
    }
  };

  const triggerAction = async (actionId: string, specificIds?: any[]) => {
    if (mutationInFlight.current) return;
    mutationInFlight.current = true;
    setPending(true);
    try {
      const idsToRun = specificIds !== undefined ? specificIds : selectedIds;
      const res = await executeAction(currentEntity.entityName, actionId, idsToRun);
      addToast(typeof res.result === 'string' ? res.result : 'Action executed successfully');
      setActionConfirm(null);
      loadData();
    } catch (err: any) {
      addToast(err.message || 'Action failed', 'error');
    } finally {
      mutationInFlight.current = false;
      setPending(false);
    }
  };

  const renderCellValue = (record: Record<string, any>, field: typeof listFields[0]) => {
    const val = record[field.name];
    if (val === null || val === undefined) {
      return <span className="text-muted-foreground/60 italic">-</span>;
    }

    // Badge mapping check
    if (field.badges && field.badges.length > 0) {
      const mapped = field.badges.find(b => b.value?.toString().toLowerCase() === val.toString().toLowerCase());
      if (mapped) {
        return <Badge badgeColor={mapped.color}>{mapped.label}</Badge>;
      }
    }

    if (field.fieldType === 'BOOLEAN') {
      return val ? (
        <Badge variant="outline" className="border-emerald-500/30 text-emerald-600 dark:text-emerald-400 bg-emerald-500/10">
          Yes
        </Badge>
      ) : (
        <Badge variant="outline" className="border-muted text-muted-foreground bg-muted/30">
          No
        </Badge>
      );
    }

    if (field.fieldType === 'RELATION_MANY_TO_ONE' || field.fieldType === 'RELATION_ONE_TO_ONE') {
      if (typeof val === 'object' && val !== null) {
        return <span className="font-medium text-foreground">{val.label || `#${val.id}`}</span>;
      }
    }

    if (field.sensitive) {
      return <span className="font-mono text-xs text-muted-foreground">••••••••</span>;
    }

    return <span className="truncate max-w-xs block">{val.toString()}</span>;
  };

  const pkField = currentEntity.primaryKeyField;

  return (
    <div className="p-4 sm:p-8 max-w-7xl mx-auto space-y-6 animate-fade-in">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl break-words font-extrabold tracking-tight">
            {currentEntity.pluralName || currentEntity.displayName}
          </h1>
          <p className="text-xs text-muted-foreground mt-0.5">
            {loading ? "Loading records…" : `${totalElements} total records found`}
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <Button
            variant="outline"
            size="icon"
            onClick={loadData}
            disabled={loading}
            title="Refresh table"
            className="rounded-xl"
          >
            <RefreshCw className={cn('w-4 h-4', loading && 'animate-spin text-primary')} />
          </Button>

          {currentEntity.creatable && !currentEntity.readOnly && (
            <Button
              onClick={() => navigateToCreate(currentEntity.entityName)}
              className="gap-2 shadow-sm"
            >
              <Plus className="w-4 h-4" />
              <span>New {currentEntity.displayName}</span>
            </Button>
          )}
        </div>
      </div>

      {/* Search & Filter Toolbar */}
      <Card className="p-4 shadow-sm flex flex-col md:flex-row items-center justify-between gap-3">
        <div className="relative w-full md:w-96">
          <Search className="w-4 h-4 absolute left-3.5 top-3 text-muted-foreground" />
          <Input
            type="text"
            placeholder={`Search across ${currentEntity.displayName}...`}
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            className="pl-10 text-sm"
          />
        </div>

        {filterableFields.length > 0 && (
          <Button
            variant={showFilterDrawer || Object.values(filters).filter(Boolean).length > 0 ? 'secondary' : 'outline'}
            onClick={() => setShowFilterDrawer(prev => !prev)}
            className="gap-2 text-sm"
          >
            <Filter className="w-4 h-4" />
            <span>Filters {Object.values(filters).filter(Boolean).length > 0 && `(${Object.values(filters).filter(Boolean).length})`}</span>
          </Button>
        )}
      </Card>

      {/* Filter drawer (expandable) */}
      {showFilterDrawer && filterableFields.length > 0 && (
        <Card className="p-5 bg-muted/40 grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4 animate-slide-in">
          {filterableFields.map(field => (
            <div key={field.name} className="space-y-1.5">
              <label className="text-xs font-semibold text-muted-foreground">{field.label}</label>
              {field.fieldType === 'ENUM' && field.enumValues ? (
                <FieldSelect label={field.label} value={filters[field.name] || ''}
                  onChange={value => setFilters(prev => ({ ...prev, [field.name]: value }))}
                  placeholder="All" options={field.enumValues.map(value => ({ value, label: value }))} />
              ) : field.fieldType === 'BOOLEAN' ? (
                <FieldSelect label={field.label} value={filters[field.name] || ''}
                  onChange={value => setFilters(prev => ({ ...prev, [field.name]: value }))}
                  placeholder="All" options={[{ value: 'true', label: 'True / Yes' }, { value: 'false', label: 'False / No' }]} />
              ) : (
                <Input
                  type="text"
                  placeholder={`Filter by ${field.label}`}
                  value={filters[field.name] || ''}
                  onChange={e => setFilters(prev => ({ ...prev, [field.name]: e.target.value }))}
                  className="h-9 text-xs"
                />
              )}
            </div>
          ))}
          <div className="flex items-end">
            <Button
              variant="link"
              onClick={() => setFilters({})}
              className="text-xs font-semibold text-destructive p-0 h-9"
            >
              Reset all filters
            </Button>
          </div>
        </Card>
      )}

      {/* Bulk Actions Floating Bar */}
      {selectedIds.length > 0 && (
        <div className="p-3 px-5 rounded-2xl bg-foreground text-background flex flex-col sm:flex-row gap-3 sm:items-center justify-between shadow-xl animate-slide-in">
          <div className="text-sm font-semibold flex items-center gap-2">
            <span className="w-6 h-6 rounded-full bg-primary text-primary-foreground flex items-center justify-center text-xs font-bold">
              {selectedIds.length}
            </span>
            <span>selected</span>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            {/* Custom Admin Actions for Bulk */}
            {currentEntity.actions
              .filter(a => a.scope === 'BULK_SELECTION' || a.scope === 'BOTH')
              .map(act => (
                <Button
                  key={act.id}
                  variant="secondary"
                  size="sm"
                  onClick={() => {
                    if (act.confirmation) {
                      setActionConfirm({ actionId: act.id, name: act.name, confirmMsg: act.confirmation });
                    } else {
                      triggerAction(act.id);
                    }
                  }}
                  className="gap-1.5 text-xs font-semibold"
                >
                  <Zap className="w-3.5 h-3.5 text-amber-500" />
                  <span>{act.name}</span>
                </Button>
              ))}

            {currentEntity.deletable && !currentEntity.readOnly && (
              <Button
                variant="destructive"
                size="sm"
                onClick={() => setBulkDeleteModalOpen(true)}
                className="gap-1.5 text-xs font-semibold"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>Delete Selected</span>
              </Button>
            )}
          </div>
        </div>
      )}

      {pending && <p role="status" className="flex items-center gap-2 text-sm text-muted-foreground"><Loader2 className="h-4 w-4 animate-spin" />Processing action…</p>}
      <p className="md:hidden text-xs text-muted-foreground">Swipe the table horizontally to see all columns and actions.</p>
      {/* Main Table */}
      <Card aria-busy={loading} className="min-w-0 overflow-hidden border shadow-sm">
        <span role="status" className="sr-only">{loading ? "Loading records…" : `${totalElements} records found`}</span>
        <Table className="min-w-[640px]">
          <TableHeader>
            <TableRow>
              <TableHead className="w-12 text-center">
                <Button variant="ghost" size="icon"
                  aria-label="Select all records"
                  aria-pressed={selectedIds.length > 0 && selectedIds.length === records.length}
                  disabled={loading || records.length === 0}
                  onClick={handleSelectAll}
                  className="text-muted-foreground hover:text-foreground transition-colors"
                >
                  {selectedIds.length > 0 && selectedIds.length === records.length ? (
                    <CheckSquare className="w-4 h-4 text-primary" />
                  ) : (
                    <Square className="w-4 h-4" />
                  )}
                </Button>
              </TableHead>
              {listFields.map(field => (
                <TableHead
                  key={field.name}
                  onClick={() => field.sortable && handleSort(field.name)}
                  className={cn(field.sortable && 'cursor-pointer hover:text-foreground select-none')}
                >
                  <div className="flex flex-wrap items-center gap-1.5">
                    <span>{field.label}</span>
                    {field.sortable && (
                      <span>
                        {sortField === field.name ? (
                          sortDir === 'asc' ? <ArrowUp className="w-3.5 h-3.5 text-primary" /> : <ArrowDown className="w-3.5 h-3.5 text-primary" />
                        ) : (
                          <ArrowUpDown className="w-3 h-3 text-muted-foreground/40 hover:opacity-100" />
                        )}
                      </span>
                    )}
                  </div>
                </TableHead>
              ))}
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {loading && <TableSkeleton columns={listFields.length + 2} />}
            {!loading && records.map(record => {
              const id = record[pkField];
              const isSelected = selectedIds.includes(id);

              return (
                <TableRow
                  key={id}
                  data-state={isSelected ? 'selected' : undefined}
                >
                  <TableCell className="text-center">
                    <Button variant="ghost" size="icon"
                      aria-label={`Select record ${id}`}
                      aria-pressed={isSelected}
                      onClick={() => handleToggleSelect(id)}
                      className="text-muted-foreground hover:text-foreground transition-colors"
                    >
                      {isSelected ? <CheckSquare className="w-4 h-4 text-primary" /> : <Square className="w-4 h-4" />}
                    </Button>
                  </TableCell>

                  {listFields.map(field => (
                    <TableCell key={field.name}>
                      {renderCellValue(record, field)}
                    </TableCell>
                  ))}

                  <TableCell className="text-right whitespace-nowrap">
                    <div className="flex items-center justify-end gap-1">
                      {currentEntity.updatable && !currentEntity.readOnly && (
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => navigateToEdit(currentEntity.entityName, id)}
                          className="h-8 w-8 text-muted-foreground hover:text-primary rounded-lg"
                          title="Edit"
                        >
                          <Edit2 className="w-4 h-4" />
                        </Button>
                      )}

                      {/* Dropdown for custom single-record actions or delete */}
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8 text-muted-foreground hover:text-foreground rounded-lg"
                          >
                            <span className="sr-only">Record actions</span><MoreHorizontal className="w-4 h-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end" className="w-44">
                          {currentEntity.actions
                            .filter(a => a.scope === 'SINGLE_ROW' || a.scope === 'BOTH')
                            .map(act => (
                              <DropdownMenuItem
                                key={act.id}
                                onClick={() => {
                                  if (act.confirmation) {
                                    setActionConfirm({
                                      actionId: act.id,
                                      name: act.name,
                                      confirmMsg: act.confirmation,
                                      recordId: id,
                                    });
                                  } else {
                                    triggerAction(act.id, [id]);
                                  }
                                }}
                                className="gap-2 cursor-pointer"
                              >
                                <Zap className="w-3.5 h-3.5 text-amber-500" />
                                <span>{act.name}</span>
                              </DropdownMenuItem>
                            ))}

                          {currentEntity.actions.filter(a => a.scope === 'SINGLE_ROW' || a.scope === 'BOTH').length > 0 &&
                            currentEntity.deletable && !currentEntity.readOnly && (
                              <DropdownMenuSeparator />
                            )}

                          {currentEntity.deletable && !currentEntity.readOnly && (
                            <DropdownMenuItem
                              onClick={() => setDeleteModalId(id)}
                              className="gap-2 text-destructive focus:text-destructive cursor-pointer"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                              <span>Delete</span>
                            </DropdownMenuItem>
                          )}
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </div>
                  </TableCell>
                </TableRow>
              );
            })}

            {records.length === 0 && !loading && (
              <TableRow>
                <TableCell colSpan={listFields.length + 2} className="text-center py-12 text-muted-foreground">
                  No records found
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>

        {/* Pagination Bar */}
        <div className="p-4 border-t flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-muted-foreground">
          <div className="flex flex-wrap items-center gap-2">
            <span>Rows per page:</span>
            <div className="w-24"><FieldSelect label="Rows per page" value={String(pageSize)}
              onChange={value => setPageSize(Number(value))} required
              options={Array.from(new Set([10, 25, 50, 100, pageSize])).sort((a, b) => a - b).map(size => ({ value: String(size), label: String(size) }))} /></div>
            <span>
              Showing {totalElements === 0 ? 0 : page * pageSize + 1} to{' '}
              {Math.min((page + 1) * pageSize, totalElements)} of {totalElements}
            </span>
          </div>

          <div className="flex flex-wrap items-center gap-1.5">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPage(p => Math.max(0, p - 1))}
              disabled={loading || page === 0}
              className="h-8 text-xs font-medium"
            >
              Previous
            </Button>
            <span className="px-2 font-semibold text-foreground">
              Page {page + 1} of {Math.max(1, Math.ceil(totalElements / pageSize))}
            </span>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPage(p => p + 1)}
              disabled={loading || (page + 1) * pageSize >= totalElements}
              className="h-8 text-xs font-medium"
            >
              Next
            </Button>
          </div>
        </div>
      </Card>

      {/* Delete Confirmation Dialog */}
      <Dialog open={deleteModalId !== null} onOpenChange={open => { if (!open && !pending) setDeleteModalId(null); }}>
        <DialogContent aria-describedby={undefined}>
          <DialogHeader>
            <DialogTitle>Confirm Deletion</DialogTitle>
          </DialogHeader>
          <div className="flex items-center gap-3 text-destructive py-2">
            <AlertTriangle className="w-6 h-6 shrink-0" />
            <p className="text-sm font-medium text-foreground">
              Are you sure you want to delete this {currentEntity.displayName}? This action cannot be undone.
            </p>
          </div>
          <DialogFooter className="gap-2 sm:gap-0">
            <Button disabled={pending} variant="outline" onClick={() => setDeleteModalId(null)}>
              Cancel
            </Button>
            <Button disabled={pending} variant="destructive" onClick={confirmSingleDelete}>
              {pending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              {pending ? "Deleting…" : "Delete Record"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Bulk Delete Dialog */}
      <Dialog open={bulkDeleteModalOpen} onOpenChange={open => { if (!open && !pending) setBulkDeleteModalOpen(false); }}>
        <DialogContent aria-describedby={undefined}>
          <DialogHeader>
            <DialogTitle>Confirm Bulk Deletion</DialogTitle>
          </DialogHeader>
          <div className="flex items-center gap-3 text-destructive py-2">
            <AlertTriangle className="w-6 h-6 shrink-0" />
            <p className="text-sm font-medium text-foreground">
              Are you sure you want to permanently delete {selectedIds.length} selected record(s)?
            </p>
          </div>
          <DialogFooter className="gap-2 sm:gap-0">
            <Button disabled={pending} variant="outline" onClick={() => setBulkDeleteModalOpen(false)}>
              Cancel
            </Button>
            <Button disabled={pending} variant="destructive" onClick={confirmBulkDelete}>
              {pending ? "Deleting…" : `Delete ${selectedIds.length} Records`}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Action Confirmation Dialog */}
      <Dialog open={actionConfirm !== null} onOpenChange={open => { if (!open && !pending) setActionConfirm(null); }}>
        <DialogContent aria-describedby={undefined}>
          <DialogHeader>
            <DialogTitle>{actionConfirm?.name || 'Confirm Action'}</DialogTitle>
          </DialogHeader>
          <div className="py-2">
            <p className="text-sm font-medium text-foreground">
              {actionConfirm?.confirmMsg || `Are you sure you want to execute ${actionConfirm?.name}?`}
            </p>
          </div>
          <DialogFooter className="gap-2 sm:gap-0">
            <Button disabled={pending} variant="outline" onClick={() => setActionConfirm(null)}>
              Cancel
            </Button>
            <Button
              disabled={pending}
              onClick={() => {
                if (actionConfirm) {
                  triggerAction(
                    actionConfirm.actionId,
                    actionConfirm.recordId != null ? [actionConfirm.recordId] : undefined
                  );
                }
              }}
            >
              {pending ? "Executing…" : "Execute"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};
