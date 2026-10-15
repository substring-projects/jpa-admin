export type FieldType =
  | 'STRING'
  | 'NUMBER'
  | 'BOOLEAN'
  | 'DATE'
  | 'DATE_TIME'
  | 'ENUM'
  | 'RELATION_MANY_TO_ONE'
  | 'RELATION_ONE_TO_ONE'
  | 'RELATION_ONE_TO_MANY'
  | 'RELATION_MANY_TO_MANY'
  | 'EMBEDDED'
  | 'UNKNOWN';

export type WidgetType =
  | 'AUTO'
  | 'TEXT'
  | 'TEXTAREA'
  | 'RICH_TEXT'
  | 'COLOR_PICKER'
  | 'SLIDER'
  | 'CODE_EDITOR'
  | 'DATE_PICKER'
  | 'DATE_TIME_PICKER'
  | 'FILE_UPLOAD'
  | 'IMAGE_URL'
  | 'PASSWORD'
  | 'BOOLEAN_SWITCH';

export type BadgeColor =
  | 'DEFAULT'
  | 'PRIMARY'
  | 'BLUE'
  | 'GREEN'
  | 'YELLOW'
  | 'RED'
  | 'PURPLE'
  | 'INDIGO'
  | 'PINK'
  | 'GRAY';

export interface BadgeMetadata {
  value: string;
  color: BadgeColor;
  label: string;
}

export interface ValidationMetadata {
  required: boolean;
  min?: number;
  max?: number;
  minLength?: number;
  maxLength?: number;
  pattern?: string;
  email?: boolean;
}

export interface FieldMetadata {
  name: string;
  javaType: string;
  fieldType: FieldType;
  label: string;
  description?: string;
  order: number;
  inList: boolean;
  inForm: boolean;
  inDetail: boolean;
  readOnly: boolean;
  searchable: boolean;
  sortable: boolean;
  filterable: boolean;
  placeholder?: string;
  primaryKey: boolean;
  generatedValue: boolean;
  sensitive: boolean;
  widget: WidgetType;
  validation: ValidationMetadata;
  enumValues: string[];
  badges: BadgeMetadata[];

  lookupTargetEntity?: string;
  lookupProperty?: string;
  searchProperty?: string;
  quickCreate?: boolean;

  computed?: boolean;
  format?: string;
}

export interface InlineMetadata {
  propertyName: string;
  targetEntity: string;
  type: 'TABULAR' | 'STACKED';
  title: string;
  canAdd: boolean;
  canDelete: boolean;
  limit: number;
  mappedBy?: string;
  fields: FieldMetadata[];
}

export interface ActionMetadata {
  id: string;
  name: string;
  icon?: string;
  confirmation?: string;
  scope: 'SINGLE_ROW' | 'BULK_SELECTION' | 'BOTH';
  methodName: string;
  static: boolean;
}

export interface EntityMetadata {
  entityName: string;
  javaClassName: string;
  displayName: string;
  pluralName: string;
  group: string;
  icon: string;
  order: number;
  readOnly: boolean;
  creatable: boolean;
  updatable: boolean;
  deletable: boolean;
  exportable: boolean;
  defaultSortBy: string;
  defaultSortDir: 'ASC' | 'DESC';
  pageSize: number;
  primaryKeyField: string;

  fields: FieldMetadata[];
  inlines: InlineMetadata[];
  actions: ActionMetadata[];
  roles: string[];
}

export interface PageResult<T> {
  content: T[];
  totalElements: number;
  pageNumber: number;
  pageSize: number;
  totalPages: number;
}
