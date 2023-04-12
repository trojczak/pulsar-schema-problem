package pl.trojczak.poc.pulsar.schema;

import java.util.Collections;

import org.apache.pulsar.common.functions.FunctionConfig;
import org.apache.pulsar.functions.LocalRunner;

class InputFunctionRunner {

    public static void main(String[] args) throws Exception {
        FunctionConfig functionConfig = new FunctionConfig();
        functionConfig.setName(InputFunction.class.getSimpleName());
        functionConfig.setInputs(Collections.singletonList("persistent://rtk/example/input"));
        functionConfig.setSubName("input-function");
        functionConfig.setOutput("persistent://rtk/example/output");
        functionConfig.setClassName(InputFunction.class.getName());
        functionConfig.setRuntime(FunctionConfig.Runtime.JAVA);

        LocalRunner localRunner = LocalRunner.builder().functionConfig(functionConfig).build();
        localRunner.start(false);
    }
}