package com.devhouse.financial_plan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinancialPlanApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinancialPlanApplication.class, args);
	}

}
