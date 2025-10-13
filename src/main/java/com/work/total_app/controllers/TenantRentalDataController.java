package com.work.total_app.controllers;

import com.work.total_app.constants.AuthenticationConstants;
import com.work.total_app.models.stats.MonthCalculationsDto;
import com.work.total_app.services.TenantRentalDataService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.Month;

@Controller
@RequestMapping("/tenant-rental-data")
@CrossOrigin(origins = {AuthenticationConstants.PROD_WEBSITE_URL, }, originPatterns = {AuthenticationConstants.LOCAL_WEBSITE_PATTERN, AuthenticationConstants.STAGING_WEBSITE_PATTERN})
@Log4j2
public class TenantRentalDataController {

    @Autowired
    private TenantRentalDataService dataService;

    @GetMapping("/month-calculations/{month}")
    public MonthCalculationsDto getMonthCalculations(@PathVariable Month month,
                                                     @RequestParam String tenantId)
    {
        return null;
    }
}
