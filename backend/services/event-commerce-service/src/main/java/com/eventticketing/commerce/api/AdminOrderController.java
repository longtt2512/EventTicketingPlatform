package com.eventticketing.commerce.api;

import com.eventticketing.commerce.application.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/admin/orders") @PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {
    private final JdbcTemplate db;private final OrderRepository orders;
    public AdminOrderController(JdbcTemplate db,OrderRepository orders){this.db=db;this.orders=orders;}
    @GetMapping public Map<String,Object> list(@RequestParam(required=false)String status,@RequestParam(required=false)UUID cursor,@RequestParam(defaultValue="20")int limit){
        if(limit<1||limit>100||status!=null&&!Set.of("PENDING_PAYMENT","CONFIRMED","EXPIRED","CANCELLED").contains(status))throw EventRepository.invalid("Bộ lọc đơn hàng không hợp lệ.");
        var ids=db.queryForList("select id from event_commerce.orders where (?::text is null or status=?) and (?::uuid is null or id>?) order by id limit ?",UUID.class,status,status,cursor,cursor,limit+1);
        boolean more=ids.size()>limit;var result=new LinkedHashMap<String,Object>();result.put("data",ids.stream().limit(limit).map(orders::get).toList());result.put("hasMore",more);result.put("nextCursor",more?ids.get(limit-1):null);return result;
    }
}
