package com.generated.rescueStock;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.generated.rescueStock.repositories")
public class RescueStockApplication {
  public static void main(String[] args) {
    SpringApplication.run(RescueStockApplication.class, args);
  }
}
