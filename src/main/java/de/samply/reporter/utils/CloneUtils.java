package de.samply.reporter.utils;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class CloneUtils {

    private static ObjectMapper objectMapper = JsonMapper.builder().configureForJackson2().build();

    public static <T extends Object> T clone(T object) throws CloneUtilsException {
        try {
            return cloneWithoutExceptionHandling(object);
        } catch (JacksonException e) {
            throw new CloneUtilsException(e);
        }

    }

    private static <T extends Object> T cloneWithoutExceptionHandling(T object) throws JacksonException {
        String serializedObject = objectMapper.writeValueAsString(object);
        return (T) objectMapper.readValue(serializedObject, object.getClass());
    }

}
