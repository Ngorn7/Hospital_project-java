package com.setec.hms.config;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.setec.hms.entity.Department;
import java.util.Set;
import org.hibernate.bytecode.internal.bytebuddy.ByteBuddyState;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.proxy.pojo.bytebuddy.ByteBuddyProxyFactory;
import org.hibernate.proxy.pojo.bytebuddy.ByteBuddyProxyHelper;
import org.hibernate.proxy.HibernateProxy;
import org.junit.jupiter.api.Test;

class JacksonConfigTest {

  @Test
  void serializesUninitializedLazyAssociationsAsIdentifiers() throws Exception {
    ProxyFactory factory =
        new ByteBuddyProxyFactory(new ByteBuddyProxyHelper(new ByteBuddyState()));
    factory.postInstantiate(
        Department.class.getName(), Department.class, Set.of(HibernateProxy.class), null, null, null);
    Department department = (Department) factory.getProxy(42L, null);

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JacksonConfig().hibernateProxyModule());

    String json = mapper.writeValueAsString(new DepartmentHolder(department));

    assertEquals("{\"department\":{\"id\":42}}", json);
    assertFalse(json.contains("ByteBuddyInterceptor"), json);
    assertTrue(((HibernateProxy) department).getHibernateLazyInitializer().isUninitialized());
  }

  @Test
  void doesNotSerializePrescriptionItemBackReference() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JacksonConfig().hibernateProxyModule());

    com.setec.hms.entity.PrescriptionItem item = new com.setec.hms.entity.PrescriptionItem();
    item.id = 7L;
    item.dosage = "one tablet";

    String json = mapper.writeValueAsString(item);

    assertFalse(json.contains("\"prescription\""), json);
    assertTrue(json.contains("\"dosage\":\"one tablet\""), json);
  }

  static class DepartmentHolder {
    public Department department;

    DepartmentHolder(Department department) {
      this.department = department;
    }
  }
}
