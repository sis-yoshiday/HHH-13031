package org.hibernate.bugs;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class Department {

  @Id
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY)
  private Company company;

  Department() {
  }

  public Department(Integer id, Company company) {
    this.id = id;
    this.company = company;
  }
}
