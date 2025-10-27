package com.work.total_app.controllers;

import com.work.total_app.models.stats.MonthCalculationsDto;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.Month;

@Controller
@RequestMapping("/tenant-rental-data")
@Log4j2
public class TenantRentalDataController {

    @GetMapping("/month-calculations/{month}")
    public MonthCalculationsDto getMonthCalculations(@PathVariable Month month,
                                                     @RequestParam String tenantId)
    {
        return null;
    }
}
