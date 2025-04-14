package com.melancholia.worker;

import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.nio.file.Path;

@Service
public class Solver {

    private Method solveMethod = null;
    private Path zipPath = null;

    public void setSolveMethod(Method solveMethod) {
        this.solveMethod = solveMethod;
    }

    public void setZipPath(Path zipPath) {
        this.zipPath = zipPath;
    }

    public Object solve(Task task) {
        Object[] solveArgs = {zipPath, Integer.valueOf(task.getStart()), Integer.valueOf(task.getEnd())};
        return ReflectionUtils.executeMethod(solveMethod, solveArgs);
    }

}
