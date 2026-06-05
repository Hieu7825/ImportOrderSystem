package com.importorder.fakes;

import com.importorder.model.Merchandise;
import com.importorder.repository.MerchandiseRepository;

import java.util.HashMap;
import java.util.Map;

public class FakeMerchandiseRepository extends MerchandiseRepository {

    private Map<String, Merchandise> database = new HashMap<>();

    @Override
    public Merchandise findByCode(String code) {
        return database.get(code);
    }
    
    public void save(Merchandise m) {
        database.put(m.getItemCode(), m);
    }
}
