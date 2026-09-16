package com.generated.rescueStock.services;

import com.generated.rescueStock.repositories.SupplyItemRepository;
import com.generated.rescueStock.repositories.WarehouseRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** 仓库与物资档案只读服务（写维护在演示范围内省略，数据由种子提供）。 */
@Service
public class CatalogService {
  private final WarehouseRepository warehouseRepo;
  private final SupplyItemRepository supplyItemRepo;

  public CatalogService(WarehouseRepository warehouseRepo, SupplyItemRepository supplyItemRepo) {
    this.warehouseRepo = warehouseRepo;
    this.supplyItemRepo = supplyItemRepo;
  }

  public List<Map<String, Object>> warehouses() { return warehouseRepo.findAll(); }
  public List<Map<String, Object>> supplyItems() { return supplyItemRepo.findAll(); }
}
