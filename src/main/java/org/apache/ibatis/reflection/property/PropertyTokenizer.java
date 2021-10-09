/**
 * Copyright 2009-2017 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ibatis.reflection.property;

import java.util.Iterator;

/**
 * 标准的属性名称解析器
 * @author Clinton Begin
 */
public class PropertyTokenizer implements Iterator<PropertyTokenizer> {
  private String name;
  private final String indexedName;
  private String index;
  private final String children;

  //把入参解析成name xxx
  //支持两种语法
  //一种是通过.分隔符表示多层属性的引用:name.first,
  //另一种是通过[key]来表示集合中角标或者key值为key的元素:names[0].first。
  public PropertyTokenizer(String fullname) {
    int delim = fullname.indexOf('.');
    if (delim > -1) {
      //有属性分隔符，表示是一个嵌套的多层属性
      //获取属性分隔符前面的内容作为属性，此处是names[0]
      name = fullname.substring(0, delim);
      //属性分隔符后面的内容，则作为子属性保存起来，此处是first
      children = fullname.substring(delim + 1);
    } else {
      // 当前不存属性分隔符，直接赋值即可
      name = fullname;
      children = null;
    }
    // 处理names[0].first这种包含了索引的场景
    // 包含索引内容的属性名称此处是names[0]
    indexedName = name;
    delim = name.indexOf('[');
    if (delim > -1) {
      // 包含索引引用
      // 获取索引的值，表示从names[1]中获取1。
      index = name.substring(delim + 1, name.length() - 1);
      // 重新赋值，将names[0]转为names
      name = name.substring(0, delim);
    }
  }

  public String getName() {
    return name;
  }

  public String getIndex() {
    return index;
  }

  public String getIndexedName() {
    return indexedName;
  }

  public String getChildren() {
    return children;
  }

  @Override
  public boolean hasNext() {
    return children != null;
  }

  @Override
  public PropertyTokenizer next() {
    return new PropertyTokenizer(children);
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("Remove is not supported, as it has no meaning in the context of properties.");
  }
}
