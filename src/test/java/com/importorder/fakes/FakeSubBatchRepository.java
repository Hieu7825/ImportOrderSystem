package com.importorder.fakes;

import com.importorder.model.SubBatch;
import com.importorder.repository.SubBatchRepository;

import java.util.ArrayList;
import java.util.List;

public class FakeSubBatchRepository extends SubBatchRepository {

    private List<SubBatch> database = new ArrayList<>();

    @Override
    public void save(SubBatch sb) {
        database.add(sb);
    }

    @Override
    public List<SubBatch> findByParentBatch(String batchId) {
        List<SubBatch> list = new ArrayList<>();
        for (SubBatch sb : database) {
            if (batchId.equals(sb.getParentBatchId())) {
                list.add(sb);
            }
        }
        return list;
    }

    @Override
    public void updateStatus(String subBatchId, String status) {
        for (SubBatch sb : database) {
            if (subBatchId.equals(sb.getSubBatchId())) {
                sb.setStatus(status);
            }
        }
    }
}
