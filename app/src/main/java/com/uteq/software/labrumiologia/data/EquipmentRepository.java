package com.uteq.software.labrumiologia.data;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.uteq.software.labrumiologia.model.EquipmentInfo;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EquipmentRepository {
    private final Map<String, EquipmentInfo> byId = new HashMap<>();

    public EquipmentRepository(Context context) {
        try (InputStreamReader reader = new InputStreamReader(context.getAssets().open("equipment_info.json"))) {
            List<EquipmentInfo> list = new Gson().fromJson(reader, new TypeToken<List<EquipmentInfo>>() {}.getType());
            if (list != null) {
                for (EquipmentInfo info : list) byId.put(info.id, info);
            }
        } catch (Exception ignored) {
        }
    }

    public EquipmentInfo get(String classId) {
        return byId.get(classId);
    }
}
