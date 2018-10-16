package org.hibernate.bugs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceUnitUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class HHH13031 {

  private EntityManagerFactory entityManagerFactory;

  @Before
  public void init() {
    entityManagerFactory = Persistence.createEntityManagerFactory("templatePU");
  }

  @After
  public void destroy() {
    entityManagerFactory.close();
  }

  private void prepareData() {

    EntityManager entityManager = entityManagerFactory.createEntityManager();
    entityManager.getTransaction().begin();

    Company company1 = new Company(1);
    entityManager.persist(company1);

    Department department = new Department(11, company1);
    entityManager.persist(department);

    Company company2 = new Company(2);
    entityManager.persist(company2);

    Department relatedDepartment1 = new Department(21, company2);
    entityManager.persist(relatedDepartment1);

    Department relatedDepartment2 = new Department(22, company2);
    entityManager.persist(relatedDepartment2);

    Set<Department> relatedDepartments = new HashSet<>();
    relatedDepartments.add(relatedDepartment1);
    relatedDepartments.add(relatedDepartment2);

    User user = new User(1, department, relatedDepartments);

    entityManager.persist(user);

    entityManager.getTransaction().commit();

    entityManager.close();
  }

  private <T> T doInEntityManager(Function<EntityManager, T> f) {

    EntityManager entityManager = null;

    try {
      entityManager = entityManagerFactory.createEntityManager();

      return f.apply(entityManager);
    } finally {
      if (entityManager != null) {
        entityManager.close();
      }
    }
  }

  private static User findByIdWithGraph(EntityManager entityManager, Integer id,
      EntityGraph<User> entityGraph) {

    Map<String, Object> hints = new HashMap<>();
    hints.put("javax.persistence.fetchgraph", entityGraph);

    return entityManager.find(User.class, id, hints);
  }

  // ManyToOne and ManyToMany
  @Test
  public void test_fetch_department_company_and_relatedDepartments_company() {

    prepareData();

    User user = doInEntityManager(em -> {

      EntityGraph<User> entityGraph = em.createEntityGraph(User.class);
      entityGraph
          .addSubgraph("department", Department.class)
          .addAttributeNodes("company");

      entityGraph
          .addSubgraph("relatedDepartments", Department.class)
          .addAttributeNodes("company");

      return findByIdWithGraph(em, 1, entityGraph);
    });

    // see generated sql,
    // and confirm that join statement from relatedDepartments to company does not generated.

    /*
     * select
     *   user0_.id as id1_2_0_,
     *   user0_.department_id as departme2_2_0_,
     *   department1_.id as id1_1_1_,
     *   department1_.company_id as company_2_1_1_,
     *   company2_.id as id1_0_2_,
     *   relateddep3_.User_id as User_id1_3_3_,
     *   department4_.id as relatedD2_3_3_,
     *   department4_.id as id1_1_4_,
     *   department4_.company_id as company_2_1_4_
     * from
     *   User user0_
     * left outer join
     *   Department department1_ on user0_.department_id=department1_.id
     * left outer join
     *   Company company2_ on department1_.company_id=company2_.id
     * left outer join
     *   User_Department relateddep3_ on user0_.id=relateddep3_.User_id
     * left outer join
     *   Department department4_ on relateddep3_.relatedDepartments_id=department4_.id
     * where
     *   user0_.id=?
     */

    PersistenceUnitUtil unitUtil = entityManagerFactory.getPersistenceUnitUtil();

    assertTrue(unitUtil.isLoaded(user, "department"));
    assertTrue(unitUtil.isLoaded(user.getDepartment(), "company"));

    assertTrue(unitUtil.isLoaded(user, "relatedDepartments"));
    user.getRelatedDepartments()
        .forEach(department -> assertTrue(unitUtil.isLoaded(department, "company")));
  }

  // only ManyToMany
  @Test
  public void test_fetch_relatedDepartments_company() {

    prepareData();

    User user = doInEntityManager(em -> {

      EntityGraph<User> entityGraph = em.createEntityGraph(User.class);

      entityGraph
          .addSubgraph("relatedDepartments", Department.class)
          .addAttributeNodes("company");

      return findByIdWithGraph(em, 1, entityGraph);
    });

    /*
     * select
     *   user0_.id as id1_2_0_,
     *   user0_.department_id as departme2_2_0_,
     *   relateddep1_.User_id as User_id1_3_1_,
     *   department2_.id as relatedD2_3_1_,
     *   department2_.id as id1_1_2_,
     *   department2_.company_id as company_2_1_2_,
     *   company3_.id as id1_0_3_
     * from
     *   User user0_
     * left outer join
     *   User_Department relateddep1_ on user0_.id=relateddep1_.User_id
     * left outer join
     *   Department department2_ on relateddep1_.relatedDepartments_id=department2_.id
     * left outer join
     *   Company company3_ on department2_.company_id=company3_.id
     * where
     *     user0_.id=?
     */

    PersistenceUnitUtil unitUtil = entityManagerFactory.getPersistenceUnitUtil();

    assertFalse(unitUtil.isLoaded(user, "department"));

    assertTrue(unitUtil.isLoaded(user, "relatedDepartments"));
    user.getRelatedDepartments()
        .forEach(department -> assertTrue(unitUtil.isLoaded(department, "company")));
  }

  // only ManyToOne
  @Test
  public void test_fetch_department_company() {

    prepareData();

    User user = doInEntityManager(em -> {

      EntityGraph<User> entityGraph = em.createEntityGraph(User.class);

      entityGraph
          .addSubgraph("department")
          .addAttributeNodes("company");

      return findByIdWithGraph(em, 1, entityGraph);
    });

    /*
     * select
     *   user0_.id as id1_2_0_,
     *   user0_.department_id as departme2_2_0_,
     *   department1_.id as id1_1_1_,
     *   department1_.company_id as company_2_1_1_,
     *   company2_.id as id1_0_2_
     * from
     *   User user0_
     * left outer join
     *   Department department1_ on user0_.department_id=department1_.id
     * left outer join
     *   Company company2_ on department1_.company_id=company2_.id
     * where
     *   user0_.id=?
     */

    PersistenceUnitUtil unitUtil = entityManagerFactory.getPersistenceUnitUtil();

    assertTrue(unitUtil.isLoaded(user, "department"));
    assertTrue(unitUtil.isLoaded(user.getDepartment(), "company"));

    assertFalse(unitUtil.isLoaded(user, "relatedDepartments"));
  }

  @Test
  public void test_join_fetch() {

    prepareData();

    User user = doInEntityManager(em -> em.createQuery(
        "select distinct u from User u " +
            "join fetch u.department d " +
            "join fetch d.company " +
            "join fetch u.relatedDepartments rd " +
            "join fetch rd.company " +
            "where u.id = 1",
        User.class).getSingleResult());

    PersistenceUnitUtil unitUtil = entityManagerFactory.getPersistenceUnitUtil();

    assertTrue(unitUtil.isLoaded(user, "department"));
    assertTrue(unitUtil.isLoaded(user.getDepartment(), "company"));

    assertTrue(unitUtil.isLoaded(user, "relatedDepartments"));
    user.getRelatedDepartments()
        .forEach(department -> assertTrue(unitUtil.isLoaded(department, "company")));
  }
}
