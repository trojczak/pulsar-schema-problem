package pl.trojczak.poc.pulsar.schema;

import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;

public class InputFunctionProducer {

    private static final String PULSAR_SERVICE_URL = "pulsar://localhost:6650";
    private static final String INPUT_TOPIC = "persistent://rtk/example/input";

    public static void main(String[] args) throws PulsarClientException {
        PulsarClient pulsarClient = PulsarClient.builder().serviceUrl(PULSAR_SERVICE_URL).build();
        try (Producer<User> userProducer = pulsarClient.newProducer(Schema.AVRO(User.class)).topic(INPUT_TOPIC).create()) {
            userProducer.send(new User("Bob", 55));
//        userProducer.send(new User("Bob", 55, true));
        }
        pulsarClient.close();
    }
}
