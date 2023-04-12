package pl.trojczak.poc.pulsar.schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

  private Integer age;
  private String name;
  //  private Boolean active;

  public User(String name, Integer age) {
    this.age = age;
    this.name = name;
  }
}
