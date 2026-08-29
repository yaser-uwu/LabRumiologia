package com.uteq.software.labrumiologia.data;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.uteq.software.labrumiologia.model.EquipmentInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EquipmentRepository {
    private final Map<String, EquipmentInfo> byId = new HashMap<>();

    public EquipmentRepository(Context context) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open("equipment_info.json")))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            Type type = new TypeToken<List<EquipmentInfo>>(){}.getType();
            List<EquipmentInfo> list = new Gson().fromJson(sb.toString(), type);
            if (list != null) {
                for (EquipmentInfo info : list) {
                    byId.put(info.id, info);
                }
            }
        } catch (Exception ignored) {
        }
    }

    public EquipmentInfo get(String classId) {
        return byId.get(classId);
    }

    public Map<String, EquipmentInfo> all() {
        return Collections.unmodifiableMap(byId);
    }
}
