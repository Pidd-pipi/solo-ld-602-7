package com.generated.rescueStock.controllers;

import com.generated.rescueStock.routes.SupplyItemRoutes;
import com.generated.rescueStock.routes.WarehouseRoutes;
import com.generated.rescueStock.services.CatalogService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 仓库与物资档案只读接口。 */
@RestController
public class CatalogController {
  private final CatalogService catalog;

  public CatalogController(CatalogService catalog) { this.catalog = catalog; }

  @GetMapping(WarehouseRoutes.BASE)
  public List<Map<String, Object>> warehouses() {
    return com.generated.rescueStock.constructors.WarehouseViewFactory.list(catalog.warehouses());
  }

  @GetMapping(SupplyItemRoutes.BASE)
  public List<Map<String, Object>> supplyItems() {
    return com.generated.rescueStock.constructors.SupplyItemViewFactory.list(catalog.supplyItems());
  }
}
