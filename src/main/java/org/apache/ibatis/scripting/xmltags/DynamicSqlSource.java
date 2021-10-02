/**
 * Copyright 2009-2019 the original author or authors.
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
package org.apache.ibatis.scripting.xmltags;

import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;

/**
 * 当 SQL 语句中包含动态 SQL 的时候，会使用 DynamicSqlSource 对象。
 *
 * @author Clinton Begin
 */
public class DynamicSqlSource implements SqlSource {
  /*
    DynamicSqlSource 中维护了一个 SqlNode 类型的字段（rootSqlNode 字段），用于记录整个 SqlNode 树形结构的根节点。
    在 DynamicSqlSource 的 getBoundSql() 方法实现中，会使用前面介绍的 SqlNode、SqlSourceBuilder 等组件，
    完成动态 SQL 语句以及“#{}”占位符的解析，具体的实现如下：
   */
  private final Configuration configuration;
  private final SqlNode rootSqlNode;

  public DynamicSqlSource(Configuration configuration, SqlNode rootSqlNode) {
    //
    this.configuration = configuration;
    //
    this.rootSqlNode = rootSqlNode;
  }

  @Override
  public BoundSql getBoundSql(Object parameterObject) {
    //创建DynamicContext对象，parameterObject是用户传入的实参
    DynamicContext context = new DynamicContext(configuration, parameterObject);
    //调用rootSqlNode.apply()方法，完成整个树形结构中全部SqlNode对象对SQL片段的解析
    //这里无须关心rootSqlNode这棵树中到底有多少SqlNode对象，每个SqlNode对象的行为都是一致的，
    //都会将解析之后的SQL语句片段追加到DynamicContext中，形成最终的、完整的SQL语句
    //这是使用组合设计模式的好处
    rootSqlNode.apply(context);
    //通过SqlSourceBuilder解析"#{}"占位符中的属性，并将SQL语句中的"#{}"占位符替换成"?"占位符
    SqlSourceBuilder sqlSourceParser = new SqlSourceBuilder(configuration);
    //
    Class<?> parameterType = parameterObject == null ? Object.class : parameterObject.getClass();
    //
    SqlSource sqlSource = sqlSourceParser.parse(context.getSql(), parameterType, context.getBindings());
    //创建BoundSql对象
    BoundSql boundSql = sqlSource.getBoundSql(parameterObject);
    context.getBindings().forEach(boundSql::setAdditionalParameter);
    //这里最终返回的 BoundSql 对象，包含了解析之后的 SQL 语句（sql 字段）、每个“#{}”占位符的属性信息（parameterMappings 字段 ，
    //List<ParameterMapping> 类型）、实参信息（parameterObject 字段）以及 DynamicContext
    //中记录的 KV 信息（additionalParameters 集合，Map<String, Object> 类型）。
    return boundSql;
  }

}
