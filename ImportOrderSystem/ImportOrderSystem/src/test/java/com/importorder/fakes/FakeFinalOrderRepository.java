package com.importorder.fakes;

import com.importorder.model.FinalOrder;
import com.importorder.repository.FinalOrderRepository;

import java.util.ArrayList;
import java.util.List;

public class FakeFinalOrderRepository extends FinalOrderRepository {

    private List<FinalOrder> database = new ArrayList<>();

    @Override
    public void save(FinalOrder fo) {
        database.add(fo);
    }

    @Override
    public void saveAll(List<FinalOrder> orders) {
        database.addAll(orders);
    }

    @Override
    public List<FinalOrder> findBySiteOrder(String siteOrderId) {
        List<FinalOrder> list = new ArrayList<>();
        for (FinalOrder fo : database) {
            if (siteOrderId.equals(fo.getSiteOrderId())) {
                list.add(fo);
            }
        }
        return list;
    }

    @Override
    public void updateStatus(String siteOrderId, String itemCode, String status) {
        for (FinalOrder fo : database) {
            if (siteOrderId.equals(fo.getSiteOrderId()) && itemCode.equals(fo.getItemCode())) {
                fo.setStatus(status);
            }
        }
    }

    @Override
    public void cancelBySiteOrder(String siteOrderId) {
        for (FinalOrder fo : database) {
            if (siteOrderId.equals(fo.getSiteOrderId())) {
                fo.setStatus("CANCELLED");
            }
        }
    }
}
