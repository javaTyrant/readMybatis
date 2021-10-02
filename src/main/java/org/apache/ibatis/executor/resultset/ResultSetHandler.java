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
package org.apache.ibatis.executor.resultset;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.ibatis.cursor.Cursor;
//你如果有 JDBC 编程经验的话，应该知道在数据库中执行一条 Select 语句通常只能拿到一个 ResultSet
//但这只是我们最常用的一种查询数据库的方式，其实数据库还支持同时返回多个 ResultSet 的场景，
//例如在存储过程中执行多条 Select 语句。MyBatis 作为一个通用的持久化框架
//不仅要支持常用的基础功能，还要对其他使用场景进行全面的支持。
/**
 * @author Clinton Begin
 */
public interface ResultSetHandler {

  //将ResultSet映射成Java对象
  <E> List<E> handleResultSets(Statement stmt) throws SQLException;

  //将ResultSet映射成游标对象
  <E> Cursor<E> handleCursorResultSets(Statement stmt) throws SQLException;

  //处理存储过程的输出参数
  void handleOutputParameters(CallableStatement cs) throws SQLException;

}
