package com.naresh.shop;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import static com.naresh.shop.Models.*;
@RestController
@RequestMapping("/api")
class Api {
 private final ShopService s;
 @Value("${shop.name}") String name;@Value("${shop.address}") String address;@Value("${shop.phone}") String phone;
 Api(ShopService s){this.s=s;}
 @GetMapping("/health") Map<String,String> health(){return Map.of("status","UP");}
 @GetMapping("/me") Map<String,String> me(){return Map.of("name",name,"address",address,"phone",phone,"currency","INR","timezone","Asia/Kolkata");}
 @GetMapping("/products") Object products(){return s.products();}
 @PostMapping("/products") @ResponseStatus(HttpStatus.CREATED) Object createProduct(@Valid @RequestBody ProductInput p){return s.createProduct(p);}
 @PutMapping("/products/{id}") Object updateProduct(@PathVariable long id,@Valid @RequestBody ProductInput p){return s.updateProduct(id,p);}
 @DeleteMapping("/products/{id}") void deleteProduct(@PathVariable long id){s.deleteProduct(id);}
 @PostMapping("/products/{id}/stock") Object stock(@PathVariable long id,@Valid @RequestBody StockInput p){return s.adjust(id,p);}
 @GetMapping("/products/{id}/movements") Object movements(@PathVariable long id){return s.movements(id);}
 @GetMapping("/invoices") Object bills(@RequestParam LocalDate from,@RequestParam LocalDate to){return s.bills(from,to);}
 @GetMapping("/invoices/{id}") Object bill(@PathVariable String id){return s.bill(id);}
 @PutMapping("/invoices/{id}") Object save(@PathVariable String id,@Valid @RequestBody BillInput b){return s.saveDraft(id,b);}
 @PostMapping("/invoices/{id}/post") Object post(@PathVariable String id,@RequestParam long version){return s.post(id,version);}
 @PostMapping("/invoices/{id}/void") Object cancel(@PathVariable String id,@Valid @RequestBody VoidInput v){return s.voidBill(id,v.reason());}
 @DeleteMapping("/invoices/{id}") void delete(@PathVariable String id,@RequestParam long version){s.deleteDraft(id,version);}
 @GetMapping("/expenses") Object expenses(@RequestParam LocalDate from,@RequestParam LocalDate to){return s.expenses(from,to);}
 @PostMapping("/expenses") void addExpense(@Valid @RequestBody ExpenseInput e){s.saveExpense(null,e);}
 @PutMapping("/expenses/{id}") void editExpense(@PathVariable long id,@Valid @RequestBody ExpenseInput e){s.saveExpense(id,e);}
 @DeleteMapping("/expenses/{id}") void deleteExpense(@PathVariable long id,@RequestParam long version){s.deleteExpense(id,version);}
 @GetMapping("/reports") Object report(@RequestParam LocalDate from,@RequestParam LocalDate to,@RequestParam(defaultValue="DAY") String group){return s.report(from,to,group);}
}
@RestControllerAdvice
class Errors {
 @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> business(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",Objects.requireNonNullElse(e.getReason(),"Request failed")));}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation(MethodArgumentNotValidException e){return ResponseEntity.badRequest().body(Map.of("message",e.getBindingResult().getFieldErrors().stream().map(f->f.getField()+": "+f.getDefaultMessage()).findFirst().orElse("Invalid input")));}
 @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<?> invalid(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("message",Objects.requireNonNullElse(e.getMessage(),"Invalid input")));}
 @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<?> conflict(){return ResponseEntity.status(409).body(Map.of("message","Duplicate SKU, conflicting update, or referenced record. Refresh and try again."));}
}
