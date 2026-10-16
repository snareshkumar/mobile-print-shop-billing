package com.naresh.shop;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
class MoneyTest {
 @Test void discountBeforeTax(){var m=Money.line(new BigDecimal("100"),new BigDecimal("60"),2,new BigDecimal("20"),new BigDecimal("18"));assertEquals(new BigDecimal("212.40"),m.total());assertEquals(new BigDecimal("120.00"),m.cost());}
 @Test void rejectsExcessDiscount(){assertThrows(IllegalArgumentException.class,()->Money.line(BigDecimal.ONE,BigDecimal.ONE,1,BigDecimal.TEN,BigDecimal.ZERO));}
 @Test void weightedReceipts(){assertEquals(new BigDecimal("70.00"),Money.weightedCost(10,new BigDecimal("60"),10,new BigDecimal("80")));}
}
