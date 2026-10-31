import { readFileSync } from 'node:fs';
import assert from 'node:assert/strict';
import { test } from 'node:test';
import ts from 'typescript';

const load = async file => {
  const source = readFileSync(new URL(`../src/lib/${file}`, import.meta.url), 'utf8');
  const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 } });
  return import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`);
};
const { shortId, humanize, recordLabel, enumTone, entityIcon, parseTemporal, plainValue } = await load('format.ts');
const { validateField } = await load('validation.ts');
const field = (name, extra = {}) => ({ name, label: name, fieldType: 'STRING', validation: { required: false }, ...extra });

test('long identifiers are shortened while plain ids stay intact', () => {
  assert.equal(shortId('e15cf53c-6177-4634-b7f2-94d718fd3a5a'), 'e15cf53c');
  assert.equal(shortId(42), '42');
});

test('constant-style enum values read as sentences', () => {
  assert.equal(humanize('OUT_OF_STOCK'), 'Out of stock');
  assert.equal(humanize('Already nice'), 'Already nice');
});

test('enum tones follow meaning before falling back to a stable hash', () => {
  assert.equal(enumTone('CANCELLED'), 'RED');
  assert.equal(enumTone('DELIVERED'), 'GREEN');
  assert.equal(enumTone('PENDING'), 'YELLOW');
  assert.equal(enumTone('customer'), enumTone('customer'));
});

test('record labels prefer human-readable fields over identifiers', () => {
  const entity = { primaryKeyField: 'id', fields: [field('id', { primaryKey: true }), field('sku')] };
  assert.equal(recordLabel(entity, { id: 1, name: 'Widget' }), 'Widget');
  assert.equal(recordLabel(entity, { id: 2, sku: 'SKU-9' }), 'SKU-9');
  assert.equal(recordLabel(entity, { id: 'e15cf53c-6177-4634-b7f2-94d718fd3a5a' }), '#e15cf53c');
});

test('default database icons are replaced using the entity name', () => {
  assert.equal(entityIcon({ icon: 'database', entityName: 'TicketComment' }), 'message-square');
  assert.equal(entityIcon({ icon: 'database', entityName: 'UserConversation' }), 'messages-square');
  assert.equal(entityIcon({ icon: 'rocket', entityName: 'User' }), 'rocket');
});

test('date-only values are not shifted by the local time zone', () => {
  const date = parseTemporal('2026-01-31', true);
  assert.deepEqual([date.getFullYear(), date.getMonth(), date.getDate()], [2026, 0, 31]);
  assert.equal(parseTemporal('not a date', false), null);
});

test('relations export their label', () => {
  assert.equal(plainValue(field('category'), { id: 3, label: 'Books' }), 'Books');
  assert.equal(plainValue(field('active', { fieldType: 'BOOLEAN' }), false), 'No');
});

test('client validation mirrors server constraints', () => {
  assert.equal(validateField(field('title', { validation: { required: true } }), '', false), 'title is required');
  assert.equal(validateField(field('price', { fieldType: 'NUMBER', validation: { required: true, min: 0.01 } }), '-5', false), 'Must be at least 0.01');
  assert.equal(validateField(field('email', { validation: { required: false, email: true } }), 'nope', false), 'Enter a valid email address');
  assert.equal(validateField(field('secret', { sensitive: true, validation: { required: true } }), '', true), null);
  assert.equal(validateField(field('owner', { fieldType: 'RELATION_MANY_TO_ONE', validation: { required: true } }), null, false), 'owner is required');
});
