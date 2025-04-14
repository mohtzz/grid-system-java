package com.melancholia.distributor.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ReflectionUtils {

    private static final Logger log = LoggerFactory.getLogger(ReflectionUtils.class);

    public static List<Method> getAnnotatedMethodsByName(Path jarFilePath, String className, String annotationName) throws Exception {
        List<Method> annotatedMethods = new ArrayList<>();

        File jarFile = jarFilePath.toFile();
        URL jarURL = jarFile.toURI().toURL();
        URLClassLoader classLoader = new URLClassLoader(new URL[]{jarURL});

        Class<?> targetClass = Class.forName(className, true, classLoader);

        Class<? extends Annotation> annotationClass = null;
        try {
            annotationClass = (Class<? extends Annotation>) Class.forName(annotationName, true, classLoader);
        } catch (ClassNotFoundException e) {
            log.error("Annotation class not found: {}", annotationName);
            return annotatedMethods;
        }

        Method[] methods = targetClass.getDeclaredMethods();

        for (Method method : methods) {
            if (method.isAnnotationPresent(annotationClass)) {
                annotatedMethods.add(method);
            }
        }

        return annotatedMethods;
    }

    public static Object executeMethod(Method method, Object[] args) {

        method.setAccessible(true);

        Object instance = null;
        if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
            Class<?> clazz = method.getDeclaringClass();
            try {
                instance = clazz.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                log.error("Error: {}", e.getMessage());
            }
        }

        try {
            return method.invoke(instance, args);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error invoking method {} : {}", method.getName(), e.getMessage());
            return null;
        }
    }

}
