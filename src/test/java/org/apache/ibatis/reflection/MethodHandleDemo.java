package org.apache.ibatis.reflection;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * @author lufengxiang
 * @since 2021/10/11
 **/
public class MethodHandleDemo {
  // 定义一个sayHello()方法

  public String sayHello(String s) {
    return "Hello, " + s;
  }

  public static void main(String[] args) throws Throwable {
    //1.初始化MethodHandleDemo实例
    MethodHandleDemo subMethodHandleDemo = new SubMethodHandleDemo();
    //2.定义sayHello()方法的签名，第一个参数是方法的返回值类型，第二个参数是方法的参数列表
    MethodType methodType = MethodType.methodType(String.class, String.class);
    //3.根据方法名和MethodType在MethodHandleDemo中查找对应的MethodHandle
    MethodHandle methodHandle = MethodHandles.lookup()
      .findVirtual(MethodHandleDemo.class, "sayHello", methodType);
    //4.将MethodHandle绑定到一个对象上，然后通过invokeWithArguments()方法传入实参并执行
    System.out.println(methodHandle.bindTo(subMethodHandleDemo)
      .invokeWithArguments("MethodHandleDemo"));
    // 下面是调用MethodHandleDemo对象(即父类)的方法
    MethodHandleDemo methodHandleDemo = new MethodHandleDemo();
    System.out.println(methodHandle.bindTo(methodHandleDemo)
      .invokeWithArguments("MethodHandleDemo"));
  }

  public static class SubMethodHandleDemo extends MethodHandleDemo {
    // 定义一个sayHello()方法
    public String sayHello(String s) {
      return "Sub Hello, " + s;
    }
  }
}
