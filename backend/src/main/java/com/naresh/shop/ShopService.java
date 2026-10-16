package com.naresh.shop;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import static com.naresh.shop.Models.*;
@Service
public class ShopService {
 private final JdbcTemplate db;
 public ShopService(JdbcTemplate db){this.db=db;}
 static ResponseStatusException bad(String text){return new ResponseStatusException(HttpStatus.CONFLICT,text);}
 static long num(Map<String,Object> r,String key){return ((Number)r.get(key)).longValue();}
 static BigDecimal money(Map<String,Object> r,String key){return new BigDecimal(r.get(key).toString());}
 static boolean active(Map<String,Object> r){Object x=r.get("active");return Boolean.TRUE.equals(x)||"1".equals(x.toString());}
 Map<String,Object> one(String sql,Object...args){var rows=db.queryForList(sql,args);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Record not found");return rows.get(0);}
 public List<Map<String,Object>> products(){return db.queryForList("SELECT * FROM product ORDER BY active DESC,name");}
 @Transactional public Map<String,Object> createProduct(ProductInput p){
  db.update("INSERT INTO product(sku,name,category,kind,cost,price,tax_rate,reorder_level,active) VALUES(?,?,?,?,?,?,?,?,?)",p.sku().trim(),p.name().trim(),p.category().trim(),p.kind(),p.cost(),p.price(),p.taxRate(),p.reorderLevel(),p.active());
  return one("SELECT * FROM product WHERE sku=?",p.sku().trim());
 }
 @Transactional public Map<String,Object> updateProduct(long id,ProductInput p){
  var old=one("SELECT * FROM product WHERE id=? FOR UPDATE",id);
  if(!old.get("kind").equals(p.kind()))throw bad("Product/service type cannot be changed; create a separate item");
  if(money(old,"cost").compareTo(p.cost())!=0&&num(old,"stock")>0&&p.kind().equals("PRODUCT"))throw bad("Use a stock receipt to update purchase cost while stock is on hand");
  int n=db.update("UPDATE product SET sku=?,name=?,category=?,cost=?,price=?,tax_rate=?,reorder_level=?,active=?,version=version+1 WHERE id=? AND version=?",p.sku().trim(),p.name().trim(),p.category().trim(),p.cost(),p.price(),p.taxRate(),p.reorderLevel(),p.active(),id,p.version());
  if(n==0)throw bad("Product changed. Refresh and try again");return one("SELECT * FROM product WHERE id=?",id);
 }
 @Transactional public void deleteProduct(long id){
  var p=one("SELECT * FROM product WHERE id=? FOR UPDATE",id);
  if(num(p,"stock")!=0)throw bad("Adjust remaining stock to zero before deleting");
  if(db.queryForObject("SELECT COUNT(*) FROM invoice_line WHERE product_id=?",Long.class,id)>0||db.queryForObject("SELECT COUNT(*) FROM stock_movement WHERE product_id=?",Long.class,id)>0)throw bad("This item has history. Archive it instead");
  db.update("DELETE FROM product WHERE id=?",id);
 }
 @Transactional public Map<String,Object> adjust(long id,StockInput input){
  var p=one("SELECT * FROM product WHERE id=? FOR UPDATE",id);
  if(!p.get("kind").equals("PRODUCT"))throw bad("Printing services do not hold stock");
  if(input.delta()==0)throw bad("Enter a non-zero adjustment");
  long balance=num(p,"stock")+input.delta();if(balance<0||balance>10000000)throw bad("Stock balance is outside allowed range");
  BigDecimal cost=money(p,"cost");
  if(input.unitCost()!=null){if(input.delta()<0)throw bad("Unit purchase cost applies only to receipts");cost=Money.weightedCost((int)num(p,"stock"),cost,input.delta(),input.unitCost());}
  db.update("UPDATE product SET stock=?,cost=?,version=version+1 WHERE id=?",balance,cost,id);
  movement(id,input.delta(),balance,input.reason(),null);
  return one("SELECT * FROM product WHERE id=?",id);
 }
 void movement(long id,int delta,long balance,String reason,String invoice){db.update("INSERT INTO stock_movement(product_id,delta,balance,reason,invoice_id) VALUES(?,?,?,?,?)",id,delta,balance,reason,invoice);}
 public List<Map<String,Object>> movements(long id){return db.queryForList("SELECT * FROM stock_movement WHERE product_id=? ORDER BY id DESC LIMIT 500",id);}
 public Map<String,Object> bill(String id){var row=one("SELECT * FROM invoice WHERE id=?",id);row.put("items",db.queryForList("SELECT * FROM invoice_line WHERE invoice_id=? ORDER BY id",id));return row;}
 public List<Map<String,Object>> bills(LocalDate from,LocalDate to){range(from,to);return db.queryForList("SELECT * FROM invoice WHERE (business_date BETWEEN ? AND ?) OR status='DRAFT' ORDER BY number DESC LIMIT 1000",from,to);}
 @Transactional public Map<String,Object> saveDraft(String id,BillInput input){
  UUID.fromString(id);
  var existing=db.queryForList("SELECT * FROM invoice WHERE id=? FOR UPDATE",id);
  if(existing.isEmpty()){
   if(input.version()!=0)throw bad("Draft does not exist");
   db.update("INSERT INTO invoice(id,customer,phone,payment) VALUES(?,?,?,?)",id,input.customer(),input.phone(),input.payment());
  }else if(!existing.get(0).get("status").equals("DRAFT")||num(existing.get(0),"version")!=input.version())throw bad("Bill changed or was issued. Refresh before editing");
  var ids=new HashSet<Long>();for(var i:input.items())if(!ids.add(i.productId()))throw bad("Combine duplicate products into one bill line");
  db.update("DELETE FROM invoice_line WHERE invoice_id=?",id);
  BigDecimal sub=BigDecimal.ZERO,disc=BigDecimal.ZERO,tax=BigDecimal.ZERO,total=BigDecimal.ZERO,cost=BigDecimal.ZERO;
  for(var i:input.items()){
   var p=one("SELECT * FROM product WHERE id=?",i.productId());if(!active(p))throw bad("An item is archived");
   var m=Money.line(money(p,"price"),money(p,"cost"),i.quantity(),i.discount(),money(p,"tax_rate"));
   sub=sub.add(m.subtotal());disc=disc.add(m.discount());tax=tax.add(m.tax());total=total.add(m.total());cost=cost.add(m.cost());
   db.update("INSERT INTO invoice_line(invoice_id,product_id,name,sku,kind,quantity,price,cost,discount,tax_rate,tax,total) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",id,i.productId(),p.get("name"),p.get("sku"),p.get("kind"),i.quantity(),p.get("price"),p.get("cost"),i.discount(),p.get("tax_rate"),m.tax(),m.total());
  }
  if(total.compareTo(new BigDecimal("999999999999.99"))>0)throw bad("Bill total exceeds supported amount");
  db.update("UPDATE invoice SET customer=?,phone=?,payment=?,subtotal=?,discount=?,tax=?,total=?,cost=?,version=version+1 WHERE id=?",input.customer(),input.phone(),input.payment(),sub,disc,tax,total,cost,id);
  return bill(id);
 }
 @Transactional public Map<String,Object> post(String id,long version){
  var b=one("SELECT * FROM invoice WHERE id=? FOR UPDATE",id);
  if(b.get("status").equals("PAID"))return bill(id);
  if(!b.get("status").equals("DRAFT"))throw bad("Only draft bills can be issued");
  if(num(b,"version")!=version)throw bad("Draft changed. Review it before issuing");
  var lines=db.queryForList("SELECT * FROM invoice_line WHERE invoice_id=? ORDER BY product_id",id);
  if(lines.isEmpty())throw bad("A bill needs at least one item");
  BigDecimal totalCost=BigDecimal.ZERO;
  for(var l:lines){
   long productId=num(l,"product_id");var p=one("SELECT * FROM product WHERE id=? FOR UPDATE",productId);
   if(!active(p))throw bad("Item is archived: "+p.get("name"));int qty=(int)num(l,"quantity");
   if(p.get("kind").equals("PRODUCT")){
    long balance=num(p,"stock")-qty;if(balance<0)throw bad("Not enough stock: "+p.get("name"));
    db.update("UPDATE product SET stock=?,version=version+1 WHERE id=?",balance,productId);movement(productId,-qty,balance,"Sale",id);
   }
   db.update("UPDATE invoice_line SET cost=? WHERE id=?",p.get("cost"),l.get("id"));
   totalCost=totalCost.add(money(p,"cost").multiply(BigDecimal.valueOf(qty)));
  }
  db.update("UPDATE invoice SET status='PAID',business_date=?,posted_at=CURRENT_TIMESTAMP(6),cost=?,version=version+1 WHERE id=?",LocalDate.now(ZoneId.of("Asia/Kolkata")),Money.round(totalCost),id);
  return bill(id);
 }
 @Transactional public Map<String,Object> voidBill(String id,String reason){
  var b=one("SELECT * FROM invoice WHERE id=? FOR UPDATE",id);
  if(b.get("status").equals("VOID"))return bill(id);
  if(!b.get("status").equals("PAID"))throw bad("Only issued bills can be cancelled");
  for(var l:db.queryForList("SELECT * FROM invoice_line WHERE invoice_id=? ORDER BY product_id",id)){
   if(!l.get("kind").equals("PRODUCT"))continue;
   long productId=num(l,"product_id");var p=one("SELECT * FROM product WHERE id=? FOR UPDATE",productId);int qty=(int)num(l,"quantity");long balance=num(p,"stock")+qty;
   BigDecimal cost=Money.weightedCost((int)num(p,"stock"),money(p,"cost"),qty,money(l,"cost"));
   db.update("UPDATE product SET stock=?,cost=?,version=version+1 WHERE id=?",balance,cost,productId);movement(productId,qty,balance,"Bill cancelled: "+reason,id);
  }
  db.update("UPDATE invoice SET status='VOID',void_reason=?,voided_at=CURRENT_TIMESTAMP(6),version=version+1 WHERE id=?",reason,id);return bill(id);
 }
 @Transactional public void deleteDraft(String id,long version){
  if(db.update("DELETE FROM invoice WHERE id=? AND status='DRAFT' AND version=?",id,version)==0)throw bad("Only an unchanged draft can be deleted");
 }
 public List<Map<String,Object>> expenses(LocalDate from,LocalDate to){range(from,to);return db.queryForList("SELECT * FROM expense WHERE business_date BETWEEN ? AND ? ORDER BY business_date DESC,id DESC",from,to);}
 @Transactional public void saveExpense(Long id,ExpenseInput e){
  if(id==null)db.update("INSERT INTO expense(business_date,category,description,amount) VALUES(?,?,?,?)",e.businessDate(),e.category(),e.description(),e.amount());
  else if(db.update("UPDATE expense SET business_date=?,category=?,description=?,amount=?,version=version+1 WHERE id=? AND version=?",e.businessDate(),e.category(),e.description(),e.amount(),id,e.version())==0)throw bad("Expense changed. Refresh and retry");
 }
 public void deleteExpense(long id,long version){if(db.update("DELETE FROM expense WHERE id=? AND version=?",id,version)==0)throw bad("Expense changed or was deleted");}
 static void range(LocalDate from,LocalDate to){if(to.isBefore(from)||to.isAfter(from.plusYears(3)))throw bad("Choose a date range up to 3 years");}
 @Transactional(readOnly=true, isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
 public Map<String,Object> report(LocalDate from,LocalDate to,String group){
  range(from,to);if(!Set.of("DAY","WEEK","MONTH").contains(group))throw bad("Invalid report grouping");
  var rows=new TreeMap<LocalDate,Map<String,Object>>();
  for(LocalDate d=from;!d.isAfter(to);d=d.plusDays(1))rows.computeIfAbsent(bucket(d,group),ShopService::empty);
  for(var r:db.queryForList("SELECT business_date,COUNT(*) bills,SUM(subtotal-discount) revenue,SUM(tax) tax,SUM(total) collected,SUM(cost) cost FROM invoice WHERE status='PAID' AND business_date BETWEEN ? AND ? GROUP BY business_date",from,to)){
   var row=rows.get(bucket(LocalDate.parse(r.get("business_date").toString()),group));row.put("bills",num(row,"bills")+num(r,"bills"));
   for(String k:List.of("revenue","tax","collected","cost"))row.put(k,money(row,k).add(money(r,k)));
  }
  for(var r:db.queryForList("SELECT business_date,SUM(amount) expenses FROM expense WHERE business_date BETWEEN ? AND ? GROUP BY business_date",from,to)){
   var row=rows.get(bucket(LocalDate.parse(r.get("business_date").toString()),group));row.put("expenses",money(row,"expenses").add(money(r,"expenses")));
  }
  var summary=empty(from);for(var r:rows.values()){finish(r);summary.put("bills",num(summary,"bills")+num(r,"bills"));for(String k:List.of("revenue","tax","collected","cost","expenses"))summary.put(k,money(summary,k).add(money(r,k)));}finish(summary);
  var top=db.queryForList("SELECT l.product_id,l.name,SUM(l.quantity) units,SUM(l.total-l.tax) revenue,SUM(l.total-l.tax-l.cost*l.quantity) profit FROM invoice_line l JOIN invoice i ON i.id=l.invoice_id WHERE i.status='PAID' AND i.business_date BETWEEN ? AND ? GROUP BY l.product_id,l.name ORDER BY revenue DESC LIMIT 20",from,to);
  return Map.of("summary",summary,"rows",rows.values(),"topProducts",top,"from",from,"to",to,"group",group);
 }
 static LocalDate bucket(LocalDate d,String group){return group.equals("MONTH")?d.withDayOfMonth(1):group.equals("WEEK")?d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)):d;}
 static Map<String,Object> empty(LocalDate d){var m=new LinkedHashMap<String,Object>();m.put("period",d);m.put("bills",0L);for(String k:List.of("revenue","tax","collected","cost","expenses"))m.put(k,BigDecimal.ZERO);return m;}
 static void finish(Map<String,Object> r){r.put("grossProfit",money(r,"revenue").subtract(money(r,"cost")));r.put("netProfit",money(r,"grossProfit").subtract(money(r,"expenses")));}
}
