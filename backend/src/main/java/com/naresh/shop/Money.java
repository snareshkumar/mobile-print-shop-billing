package com.naresh.shop;
import java.math.*;
public final class Money {
 private Money() {}
 public static BigDecimal round(BigDecimal value) { return value.setScale(2,RoundingMode.HALF_UP); }
 public record Line(BigDecimal subtotal,BigDecimal discount,BigDecimal tax,BigDecimal total,BigDecimal cost) {}
 public static Line line(BigDecimal price,BigDecimal unitCost,int quantity,BigDecimal discount,BigDecimal taxRate) {
  if(quantity<1||quantity>100000||price.signum()<0||unitCost.signum()<0||discount.signum()<0||taxRate.signum()<0||taxRate.compareTo(new BigDecimal("100"))>0) throw new IllegalArgumentException("Invalid line values");
  BigDecimal sub=round(price.multiply(BigDecimal.valueOf(quantity)));
  if(discount.compareTo(sub)>0) throw new IllegalArgumentException("Discount exceeds line value");
  BigDecimal net=sub.subtract(discount), tax=round(net.multiply(taxRate).divide(new BigDecimal("100")));
  return new Line(sub,round(discount),tax,round(net.add(tax)),round(unitCost.multiply(BigDecimal.valueOf(quantity))));
 }
 public static BigDecimal weightedCost(int stock,BigDecimal cost,int received,BigDecimal receivedCost) {
  if(received<=0) throw new IllegalArgumentException("Receipt must be positive");
  return cost.multiply(BigDecimal.valueOf(stock)).add(receivedCost.multiply(BigDecimal.valueOf(received))).divide(BigDecimal.valueOf((long)stock+received),2,RoundingMode.HALF_UP);
 }
}
