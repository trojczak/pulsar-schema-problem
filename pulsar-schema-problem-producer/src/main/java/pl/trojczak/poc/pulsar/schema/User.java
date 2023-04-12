package pl.trojczak.poc.pulsar.schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

  private String name;
  private Integer age;
//  private Boolean active;
}
