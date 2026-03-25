import test from 'node:test';
import assert from 'node:assert/strict';

import {
  extractOrders,
  isFinalOrderStatus,
  normalizeStationCode,
  parsePassengerIds,
} from '../src/utils.js';

test('normalizeStationCode should trim and uppercase', () => {
  assert.equal(normalizeStationCode(' bjp '), 'BJP');
  assert.equal(normalizeStationCode(''), '');
});

test('parsePassengerIds should parse csv and filter empty item', () => {
  assert.deepEqual(parsePassengerIds('P1, P2,, P3'), ['P1', 'P2', 'P3']);
  assert.deepEqual(parsePassengerIds(''), []);
});

test('extractOrders should support records/orders/array payload', () => {
  assert.deepEqual(extractOrders([{ orderId: '1' }]), [{ orderId: '1' }]);
  assert.deepEqual(extractOrders({ records: [{ orderId: '2' }] }), [{ orderId: '2' }]);
  assert.deepEqual(extractOrders({ orders: [{ orderId: '3' }] }), [{ orderId: '3' }]);
  assert.deepEqual(extractOrders(null), []);
});

test('isFinalOrderStatus should identify final states', () => {
  assert.equal(isFinalOrderStatus(4), true);
  assert.equal(isFinalOrderStatus('5'), true);
  assert.equal(isFinalOrderStatus(3), false);
});
