package ca.bc.gov.educ.api.batchgraduation.util;

import java.lang.reflect.Field;

/**
 * The type Transform util.
 */
public class TransformUtil {

    private TransformUtil() {
    }

    /**
     * Is uppercase field boolean.
     *
     * @param clazz     the clazz
     * @param fieldName the field name
     * @return the boolean
     */
    public static boolean isUppercaseField(Class<?> clazz, String fieldName) {
        var superClazz = clazz;
        while (!superClazz.equals(Object.class)) {
            try {
                Field field = superClazz.getDeclaredField(fieldName);
                return field.getAnnotation(UpperCase.class) != null;
            } catch (NoSuchFieldException e) {
                superClazz = superClazz.getSuperclass();
            }
        }
        return false;
    }
}
