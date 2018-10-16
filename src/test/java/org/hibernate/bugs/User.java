package org.hibernate.bugs;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

@Entity
public class User {

  @Id
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY)
  private Department department;

  @ManyToMany
  private Set<Department> relatedDepartments;

  User() {
  }

  public User(Integer id, Department department, Set<Department> relatedDepartments) {
    this.id = id;
    this.department = department;
    this.relatedDepartments = relatedDepartments;
  }

  public Department getDepartment() {
    return department;
  }

  public Set<Department> getRelatedDepartments() {
    return relatedDepartments;
  }
}
