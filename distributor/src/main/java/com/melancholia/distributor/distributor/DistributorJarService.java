package com.melancholia.distributor.distributor;

import com.melancholia.distributor.utils.ReflectionUtils;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.nio.file.Path;

@Service
public class DistributorJarService {

    private Method processResult;

    public void setProcessResult(Method method) {
        this.processResult = method;
    }

    public Object executeCalculateEnd(Method method, String dataJson) {
        Object[] calculateArgs = {dataJson};
        return ReflectionUtils.executeMethod(method, calculateArgs);
    }

    public Object executeProcessResult(String jsonResult) {
        Object[] processArgs = {jsonResult};
        return ReflectionUtils.executeMethod(processResult, processArgs);
    }

}
