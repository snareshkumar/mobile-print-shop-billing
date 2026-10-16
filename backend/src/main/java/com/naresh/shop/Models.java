package com.naresh.shop;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public final class Models {
 public record ProductInput(@NotBlank @Size(max=64) String sku,@NotBlank @Size(max=160) String name,@NotBlank @Size(max=80) String category,@Pattern(regexp="PRODUCT|SERVICE") @NotNull String kind,@NotNull @DecimalMin("0") @Digits(integer=10,fraction=2) BigDecimal cost,@NotNull @DecimalMin("0") @Digits(integer=10,fraction=2) BigDecimal price,@NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer=3,fraction=2) BigDecimal taxRate,@Min(0) int reorderLevel,boolean active,@Min(0) long version) {}
 public record StockInput(@Min(-100000) @Max(100000) int delta,@NotBlank @Size(max=255) String reason,@DecimalMin("0") @Digits(integer=10,fraction=2) BigDecimal unitCost) {}
 public record ItemInput(@Min(1) long productId,@Min(1) @Max(100000) int quantity,@NotNull @DecimalMin("0") @Digits(integer=10,fraction=2) BigDecimal discount) {}
 public record BillInput(@NotBlank @Size(max=160) String customer,@NotNull @Size(max=30) String phone,@NotNull @Pattern(regexp="CASH|UPI|CARD") String payment,@NotEmpty @Size(max=100) List<@Valid ItemInput> items,@Min(0) long version) {}
 public record VoidInput(@NotBlank @Size(max=255) String reason) {}
 public record ExpenseInput(@NotNull LocalDate businessDate,@NotBlank @Size(max=80) String category,@NotBlank @Size(max=255) String description,@NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal amount,@Min(0) long version) {}
}
