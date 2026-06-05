package com.importorder.fakes;

import com.importorder.model.WarehouseDiscrepancy;
import com.importorder.repository.DiscrepancyRepository;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class FakeDiscrepancyRepository extends DiscrepancyRepository {

    private List<WarehouseDiscrepancy> database = new ArrayList<>();

    @Override
    public void save(WarehouseDiscrepancy record) {
        if (record.getId() == null) {
            record.setId(new ObjectId());
        }
        database.add(record);
    }

    @Override
    public void saveAll(List<WarehouseDiscrepancy> discrepancies) {
        for (WarehouseDiscrepancy d : discrepancies) {
            save(d);
        }
    }
    
    @Override
    public List<WarehouseDiscrepancy> findBySiteOrder(String siteOrderId) {
        List<WarehouseDiscrepancy> list = new ArrayList<>();
        for (WarehouseDiscrepancy d : database) {
            if (siteOrderId.equals(d.getSiteOrderId())) {
                list.add(d);
            }
        }
        return list;
    }

    @Override
    public void markSynced(ObjectId id) {
        for (WarehouseDiscrepancy d : database) {
            if (id.equals(d.getId())) {
                d.setSyncedToWMS(true);
            }
        }
    }
}
