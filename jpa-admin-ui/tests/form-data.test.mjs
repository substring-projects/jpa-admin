import { readFileSync } from 'node:fs';
import assert from 'node:assert/strict';
import { test } from 'node:test';
import ts from 'typescript';

const source = readFileSync(new URL('../src/lib/form-data.ts', import.meta.url), 'utf8');
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 } });
const { buildFormPayload, editableFormFields } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`);
const field = (name, extra = {}) => ({ name, fieldType: 'STRING', inForm: true, ...extra });

test('editing preserves an unchanged secret and excludes protected or unrelated data', () => {
  const fields = [field('name'), field('password', { sensitive: true }), field('id', { primaryKey: true }), field('createdAt', { readOnly: true }), field('total', { computed: true })];
  assert.deepEqual(buildFormPayload(fields, { name: 'Updated', password: '', id: 42, createdAt: '2026-01-01', total: 99, children: [{}] }, true), { name: 'Updated' });
  assert.deepEqual(buildFormPayload(fields, { password: 'replacement' }, true), { password: 'replacement' });
});

test('zero, false, null and manually assigned create IDs survive serialization', () => {
  const fields = [field('id', { primaryKey: true }), field('count'), field('enabled'), field('category'), field('description')];
  assert.deepEqual(buildFormPayload(fields, { id: 0, count: 0, enabled: false, category: null, description: '' }, false), { id: 0, count: 0, enabled: false, category: null, description: null });
});

test('unsupported nested fields and generated IDs are excluded from scalar forms', () => {
  const fields = [field('id', { primaryKey: true, generatedValue: true }), field('name'), field('items'), field('tags', { fieldType: 'RELATION_MANY_TO_MANY' }), field('address', { fieldType: 'EMBEDDED' }), field('hidden', { inForm: false })];
  assert.deepEqual(editableFormFields({ fields, inlines: [{ propertyName: 'items' }] }).map(f => f.name), ['name']);
});
