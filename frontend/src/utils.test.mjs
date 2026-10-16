import test from 'node:test';
import assert from 'node:assert/strict';
import {estimate,periodRange,csv,billNo} from './utils.mjs';
test('discount applied before line tax',()=>assert.deepEqual(estimate([{price:100,quantity:2,discount:20,tax_rate:18}]),{subtotal:200,discount:20,tax:32.4,total:212.4}));
test('printing quantity uses page count',()=>assert.equal(estimate([{price:2.5,quantity:17,discount:0,tax_rate:0}]).total,42.5));
test('week begins Monday across month boundary',()=>assert.deepEqual(periodRange('week','2026-11-01'),{from:'2026-10-26',to:'2026-11-01'}));
test('month range starts first day',()=>assert.equal(periodRange('month','2026-10-15').from,'2026-10-01'));
test('CSV escapes commas quotes and spreadsheet formulas',()=>{const s=csv([{name:'=CMD()',note:'A,"B"'}]);assert.ok(s.includes('"\'=CMD()"'));assert.ok(s.includes('"A,""B"""'));});
test('invoice numbers formatted',()=>assert.equal(billNo(12),'NP-000012'));
