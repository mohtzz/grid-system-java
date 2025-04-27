package com.melancholia.worker;

public class Manifest {

    private String className;
    private String annotationName;

    public Manifest() {}

    public Manifest(String className, String annotationName) {
        this.className = className;
        this.annotationName = annotationName;
    }

    public String getClassName() {
        return className;
    }

    public String getAnnotationName() {
        return annotationName;
    }

}
