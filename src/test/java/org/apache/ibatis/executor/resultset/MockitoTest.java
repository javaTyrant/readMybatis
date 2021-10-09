package org.apache.ibatis.executor.resultset;

import org.apache.ibatis.mytest.Person;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

/**
 * @author lufengxiang
 * @since 2021/10/8
 **/
@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class MockitoTest {
  @Mock
  private Person person;

  @Test
  public void testPerson() {
    when(person.getName()).thenReturn("jack");
    Assert.assertEquals("jack", person.getName());
  }

  class Animal {
    private Animal parent;
    private String name;

    public Animal getParent() {
      return parent;
    }

    public void setParent(Animal parent) {
      this.parent = parent;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  @Test
  public void testMetaObject() {
    Animal animal = new Animal();
    MetaObject metaObject = MetaObject.forObject(animal,
      new DefaultObjectFactory(),
      new DefaultObjectWrapperFactory(),
      new DefaultReflectorFactory());
    metaObject.setValue("name", "bean");
    System.out.println(animal.getName());
    Assert.assertEquals("bean", animal.getName());
  }

  @Test
  public void testAssign() {
    //确定此 Class 对象表示的类或接口是否与指定的 Class 参数表示的类或接口相同，或者是其超类或超接口。
    //Determines if the class or interface represented by this Class object is either the same as,
    //or is a superclass or superinterface of, the class or interface represented by the specified Class parameter.
    System.out.println(String.class.isAssignableFrom(Object.class));
    System.out.println(Object.class.isAssignableFrom(String.class));
  }
}
