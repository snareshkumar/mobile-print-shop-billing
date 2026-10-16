package com.naresh.shop;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.naresh.shop.Models.*;
@SpringBootTest(properties={"shop.password=test-only-password-at-least-16"})
@Testcontainers
class ShopIntegrationTest {
 @Container static MySQLContainer<?> mysql=new MySQLContainer<>("mysql:8.4");
 @DynamicPropertySource static void properties(DynamicPropertyRegistry p){p.add("spring.datasource.url",mysql::getJdbcUrl);p.add("spring.datasource.username",mysql::getUsername);p.add("spring.datasource.password",mysql::getPassword);}
 @Autowired ShopService s;@Autowired JdbcTemplate db;
 BigDecimal bd(String x){return new BigDecimal(x);}
 long product(String kind){return ShopService.num(s.createProduct(new ProductInput(UUID.randomUUID().toString(),"Cable", "Accessories",kind,bd("60.00"),bd("100.00"),bd("0.00"),2,true,0)),"id");}
 Map<String,Object> draft(long pid,int qty){return s.saveDraft(UUID.randomUUID().toString(),new BillInput("Walk-in","","UPI",List.of(new ItemInput(pid,qty,BigDecimal.ZERO)),0));}
 @BeforeEach void clear(){db.update("DELETE FROM stock_movement");db.update("DELETE FROM invoice_line");db.update("DELETE FROM invoice");db.update("DELETE FROM product");db.update("DELETE FROM expense");}
 @Test void issuedBillIsIdempotentAndCancellationRestoresStock(){long p=product("PRODUCT");s.adjust(p,new StockInput(5,"Initial",null));var d=draft(p,2);String id=d.get("id").toString();long v=ShopService.num(d,"version");s.post(id,v);s.post(id,v);assertEquals(3,ShopService.num(s.products().get(0),"stock"));assertEquals(bd("120.00"),s.bill(id).get("cost"));s.voidBill(id,"Customer returned all items");s.voidBill(id,"Repeated request");assertEquals(5,ShopService.num(s.products().get(0),"stock"));}
 @Test void insufficientStockRollsBackWholeBill(){long a=product("PRODUCT"),b=product("PRODUCT");s.adjust(a,new StockInput(5,"Initial",null));var d=s.saveDraft(UUID.randomUUID().toString(),new BillInput("Walk-in","","CASH",List.of(new ItemInput(a,2,BigDecimal.ZERO),new ItemInput(b,1,BigDecimal.ZERO)),0));assertThrows(RuntimeException.class,()->s.post(d.get("id").toString(),ShopService.num(d,"version")));assertEquals(5,db.queryForObject("SELECT stock FROM product WHERE id=?",Integer.class,a));assertEquals("DRAFT",s.bill(d.get("id").toString()).get("status"));}
 @Test void profitUsesCostAtSaleAndDeductsExpenses(){long p=product("PRODUCT");s.adjust(p,new StockInput(2,"Initial",null));var d=draft(p,2);s.post(d.get("id").toString(),ShopService.num(d,"version"));s.adjust(p,new StockInput(2,"New price",bd("90")));LocalDate now=LocalDate.now(ZoneId.of("Asia/Kolkata"));s.saveExpense(null,new ExpenseInput(now,"Electricity","Shop bill",bd("20"),0));var report=s.report(now,now,"MONTH");var sum=(Map<?,?>)report.get("summary");assertEquals(bd("80.00"),sum.get("grossProfit"));assertEquals(bd("60.00"),sum.get("netProfit"));}
 @Test void printingDoesNotDeductStock(){long p=product("SERVICE");var d=draft(p,10);s.post(d.get("id").toString(),ShopService.num(d,"version"));assertEquals(0,ShopService.num(s.products().get(0),"stock"));}
 @Test void concurrentSalesCannotOversell() throws Exception {long p=product("PRODUCT");s.adjust(p,new StockInput(1,"One unit",null));var a=draft(p,1);var b=draft(p,1);ExecutorService pool=Executors.newFixedThreadPool(2);try{CountDownLatch start=new CountDownLatch(1);List<Future<Boolean>> jobs=new ArrayList<>();for(var d:List.of(a,b))jobs.add(pool.submit(()->{start.await();try{s.post(d.get("id").toString(),ShopService.num(d,"version"));return true;}catch(RuntimeException e){return false;}}));start.countDown();int success=0;for(var job:jobs)if(job.get())success++;assertEquals(1,success);assertEquals(0,ShopService.num(s.products().get(0),"stock"));}finally{pool.shutdownNow();}}
}
