package pl.trojczak.poc.pulsar.schema;

import org.apache.pulsar.functions.api.Context;
import org.apache.pulsar.functions.api.Function;

public class InputFunction implements Function<User, User> {

  @Override
  public User process(User input, Context context) throws Exception {
    System.out.println(input);
    return input;
  }
}
