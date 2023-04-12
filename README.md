# Problem Description

(Using Pulsar 2.9.3 on local Docker container)

I have a simple Pulsar function that looks like this:

```
public class InputFunction implements Function<User, User> {

  @Override
  public User process(User input, Context context) throws Exception {
    System.out.println(input);
    return input;
  }
}
```

and the following local runner:

```
public class InputFunctionRunner {

  public static void main(String[] args) throws Exception {
    System.out.println(Schema.AVRO(User.class).getSchemaInfo().getSchemaDefinition());

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
```

The User class is as follows:

```
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

  private String name;
  private Integer age;
}
```

And before running the above function, I run the Bash scripts that sets up topics and schemas (see `docs/schema_evolution.sh`):

```
# Tenant and namespace
./pulsar-admin tenants create rtk
./pulsar-admin namespaces create rtk/example
./pulsar-admin namespaces set-is-allow-auto-update-schema --disable rtk/example
./pulsar-admin namespaces set-schema-validation-enforce --enable rtk/example
./pulsar-admin namespaces set-schema-compatibility-strategy --compatibility FULL_TRANSITIVE rtk/example

# Topics
./pulsar-admin topics delete-partitioned-topic persistent://rtk/example/input
./pulsar-admin topics create-partitioned-topic --partitions 1 persistent://rtk/example/input
# infinite retention - keep forever even if acknowledged
./pulsar-admin topics set-retention -s -1 -t -1 persistent://rtk/example/input
./pulsar-admin topics create-subscription persistent://rtk/example/input -s input-function

# Topics
./pulsar-admin topics delete-partitioned-topic persistent://rtk/example/output
./pulsar-admin topics create-partitioned-topic --partitions 1 persistent://rtk/example/output
# infinite retention - keep forever even if acknowledged
./pulsar-admin topics set-retention -s -1 -t -1 persistent://rtk/example/output

# Schemas
./pulsar-admin schemas delete persistent://rtk/example/input
./pulsar-admin schemas delete persistent://rtk/example/output
./pulsar-admin schemas upload \
    --filename /pulsar/schemas/internal/User1.json \
    persistent://rtk/example/input
./pulsar-admin schemas upload \
    --filename /pulsar/schemas/internal/User1.json \
    persistent://rtk/example/output
```

For `pulsar-admin` to be able to find schema files (located in the `schemas/` folder in this project), we need to put them within the Docker container in the given path: `/pulsar/schemas/internal` .  The `/pulsar/schemas/internal/User1.json` looks like this:

```
{
  "type": "AVRO",
  "properties": {
    "__alwaysAllowNull": "true",
    "__jsr310ConversionEnabled": "false"
  },
  "schema": "{\n  \"type\": \"record\",\n  \"name\": \"User\",\n  \"namespace\": \"pl.trojczak.playground.pulsar.schema\",\n  \"fields\": [\n    {\n      \"name\": \"age\",\n      \"type\": [\"null\", \"int\"],\n      \"default\": null\n    },\n    {\n      \"name\": \"name\",\n      \"type\": [\"null\", \"string\"],\n      \"default\": null\n    }\n  ]\n}"
}
```

With this setup, I can run `InputFunction` and produce an event to `persistent://rtk/example/input` without any problems.

Then, I want to change the schemas of `persistent://rtk/example/input` and `persistent://rtk/example/output` to a new schema that adds a new field (`active: Boolean`):

```
{
  "type": "AVRO",
  "properties": {
    "__alwaysAllowNull": "true",
    "__jsr310ConversionEnabled": "false"
  },
  "schema": "{\n  \"type\": \"record\",\n  \"name\": \"User\",\n  \"namespace\": \"pl.trojczak.playground.pulsar.schema\",\n  \"fields\": [\n    {\n      \"name\": \"age\",\n      \"type\": [\"null\", \"int\"],\n      \"default\": null\n    },\n    {\n      \"name\": \"name\",\n      \"type\": [\"null\", \"string\"],\n      \"default\": null\n    },\n    {\n      \"name\": \"active\",\n      \"type\": [\"null\", \"boolean\"],\n      \"default\": null\n    }\n  ]\n}"
}
```

with the `schemas upload` commands:

```
./pulsar-admin schemas upload \
    --filename /pulsar/schemas/internal/User2.json \
    persistent://rtk/example/input
./pulsar-admin schemas upload \
    --filename /pulsar/schemas/internal/User2.json \
    persistent://rtk/example/output
```

Before uploading these schemas I checked with the schemas compatibility endpoint (`{{SERVICE_URL}}/admin/v2/schemas/rtk/example/output/compatibility`) that this new schema is in fact compatible with the current schema and I got the response:

```
{
    "schemaCompatibilityStrategy": "FULL_TRANSITIVE",
    "compatibility": true
}
```

So, it seems fine. And the `InputFunction` function starts without any problems.

But when starting a producer (`pulsar-schema-problem-producer/src/main/java/pl/trojczak/poc/pulsar/schema/InputFunctionProducer.java`), I get the following problem:

```
exception in thread "main" org.apache.pulsar.client.api.PulsarClientException$IncompatibleSchemaException: {"errorMsg":"org.apache.pulsar.broker.service.schema.exceptions.IncompatibleSchemaException: Schema not found and schema auto updating is disabled. caused by org.apache.pulsar.broker.service.schema.exceptions.IncompatibleSchemaException: Schema not found and schema auto updating is disabled.","reqId":1099532710224954493, "remote":"localhost/127.0.0.1:6650", "local":"/127.0.0.1:53766"}
	at org.apache.pulsar.client.api.PulsarClientException.unwrap(PulsarClientException.java:1014)
	at org.apache.pulsar.client.impl.ProducerBuilderImpl.create(ProducerBuilderImpl.java:91)
	at pl.trojczak.poc.pulsar.schema.InputFunctionProducer.main(InputFunctionProducer.java:15)
```

The producer is quite simple:

```
public class InputFunctionProducer {

    private static final String PULSAR_SERVICE_URL = "pulsar://localhost:6650";
    private static final String INPUT_TOPIC = "persistent://rtk/example/input";

    public static void main(String[] args) throws PulsarClientException {
        PulsarClient pulsarClient = PulsarClient.builder().serviceUrl(PULSAR_SERVICE_URL).build();
        try (Producer<User> userProducer = pulsarClient.newProducer(Schema.AVRO(User.class)).topic(INPUT_TOPIC).create()) {
//          userProducer.send(new User("Bob", 55));
            userProducer.send(new User("Bob", 55, true));
        }
        pulsarClient.close();
    }
}
```

When I debug the starting process of this producer, the schema that is generated by it looks just fine:

```
{"schema":"{\"type\":\"record\",\"name\":\"User\",\"namespace\":\"pl.trojczak.playground.pulsar.schema\",\"fields\":[{\"name\":\"active\",\"type\":[\"null\",\"boolean\"],\"default\":null},{\"name\":\"age\",\"type\":[\"null\",\"int\"],\"default\":null},{\"name\":\"name\",\"type\":[\"null\",\"string\"],\"default\":null}]}","name":"","type":"AVRO","properties":{"__jsr310ConversionEnabled":"false","__alwaysAllowNull":"true"}}
```

And even when using the compatibility-checker endpoint with this schema, it also doesn't show any problems:

```
{
    "schemaCompatibilityStrategy": "FULL_TRANSITIVE",
    "compatibility": true
}
```