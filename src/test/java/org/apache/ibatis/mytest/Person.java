package org.apache.ibatis.mytest;

/**
 * @author lufengxiang
 * @since 2021/10/8
 **/
public class Person {
  private final int age;
  private final int name;

  public Person(int age, int name) {
    this.age = age;
    this.name = name;
  }

  public String getName() {
    return "name";
  }

}
