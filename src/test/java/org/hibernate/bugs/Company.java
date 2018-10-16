package org.hibernate.bugs;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Company {

  @Id
  private Integer id;

  Company() {
  }

  public Company(Integer id) {
    this.id = id;
  }
}
