package testutil;

import java.lang.reflect.Field;

/**
 * Tiêm (inject) đối tượng fake vào field private của Service qua reflection.
 *
 * Các Service tự khởi tạo repository bằng `new` ngay trong lớp và không có
 * constructor/setter để thay thế, nên đây là cách đưa fake vào mà không sửa code gốc.
 */
public final class TestReflection {

    private TestReflection() {
    }

    public static void setField(Object target, String fieldName, Object value) {
        Field field = findField(target.getClass(), fieldName);
        if (field == null) {
            throw new IllegalArgumentException(
                    "Khong tim thay field '" + fieldName + "' trong " + target.getClass().getName());
        }
        field.setAccessible(true);
        try {
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Khong the gan field '" + fieldName + "'", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getField(Object target, String fieldName) {
        Field field = findField(target.getClass(), fieldName);
        if (field == null) {
            throw new IllegalArgumentException(
                    "Khong tim thay field '" + fieldName + "' trong " + target.getClass().getName());
        }
        field.setAccessible(true);
        try {
            return (T) field.get(target);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Khong the doc field '" + fieldName + "'", e);
        }
    }

    private static Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}