# Acceptance checklist

Run against an empty test database. These are verification steps, not claims that the full app has already been tested.

1. Open the app. Incorrect credentials must fail. `/api/products` without Authorization must return 401. `/api/health` can be public.
2. Create cable `TEST-CABLE`, cost 60, price 100, tax 18, stock type. Create printing service `TEST-PRINT`, cost 0.70, price 2.50, tax 0.
3. Receive 10 cables at cost 60. Stock must be 10; movement history must show +10 and a reason.
4. Save a bill draft for 2 cables with total line discount 20. Draft subtotal must be 200, tax 32.40 and total 212.40. Stock must remain 10.
5. Edit draft quantity then restore it to 2. Issue after recording payment. Stock must be 8 and gross profit must be 60 (180 net sales − 120 costs).
6. Re-send the same bill issuance request: stock must remain 8, with no duplicate sale movement.
7. Add an expense of 20 dated today. Today's net profit must be 40.
8. Receive 2 more cables at cost 90. Average cost must be 66 (8×60 + 2×90)/10. Earlier bill cost must remain 120 and earlier profit must remain unchanged.
9. Bill 17 print pages: total must be 42.50; cost must be 11.90. Product stock is unaffected.
10. Try to sell more cables than available. Entire issuance must fail, with no partial deductions from any other line.
11. Open two sessions and try to sell the last available unit. Only one transaction should succeed.
12. Cancel the original cable bill with a reason. Exactly 2 cables return to stock. Repeating cancellation must not restore stock again. The bill remains visible as VOID, excluded from its original-date sales report.
13. Delete an unused product successfully. Deleting a product with movements or invoice history must fail; archive it instead.
14. Edit/delete an expense; reports must update. A stale version must produce a conflict rather than overwrite newer data.
15. Daily, weekly and monthly reports must include zero-sales periods and expense-only periods. Weekly groups start Monday. Export CSV and compare totals.
16. Print a receipt or save PDF. The printout must contain the shop, customer, number, lines, totals and status, without navigation or buttons.
17. Test desktop and mobile layouts, modal scrolling, input labels, keyboard focus, empty states, API failures and retry messages.
18. Restart app and database containers without deleting the volume. Products, bills and expenses must survive.

Partial returns, credit bills and statutory GST invoices are outside this version's scope.
