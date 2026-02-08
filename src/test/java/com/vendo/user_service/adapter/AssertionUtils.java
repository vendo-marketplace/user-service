package com.vendo.user_service.adapter;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

// TODO move to test-common
public class AssertionUtils {

    private static final Logger LOGGER = Logger.getLogger(AssertionUtils.class.getName() );

    public static void assertFromDto(Object entity, Object dto) {
        Map<String, Object> entityData = mapObject(entity);
        Map<String, Object> dtoData = mapObject(dto);

        for (Map.Entry<String, Object> entry : dtoData.entrySet()) {
            Object object = entityData.get(entry.getKey());
            if (object == null) {
                LOGGER.warning("No value present in target entity: " + entry.getKey());
                continue;
            }

            assertThat(entry.getValue()).isEqualTo(object);
        }
    }

    private static Map<String, Object> mapObject(Object o) {
        Map<String, Object> data = new HashMap<>();

        Class<?> dtoClass = o.getClass();
        Field[] fields = dtoClass.getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);

            try {
                data.put(field.getName(), field.get(o));
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Unmapped target value: " + field.getName());
            }
        }

        return data;
    }
}
