package org.apache.ibatis.executor.resultset;

import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.mytest.Person;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Person {
  int get() {
    return 1;
  }
}

@ExtendWith(MockitoExtension.class)
class DefaultResultSetHandlerTest {

  @Mock
  private Statement stmt;
  @Mock
  private ResultSet rs;
  @Mock
  private ResultSetMetaData rsmd;
  @Mock
  private Connection conn;
  @Mock
  private DatabaseMetaData dbmd;
  @Mock
  private Person person;

  @Test
  public void testPerson() {
    when(person.getName()).thenReturn("jack");
    Assert.assertEquals("jack", person.getName());
  }

  @Mock
  private Person person;

  @Test
  void testPerson() {
    when(person.get()).thenReturn(4);
    assertEquals(4, person.get());
  }

  /**
   * Contrary to the spec, some drivers require case-sensitive column names when getting result.
   *
   * @see <a href="http://code.google.com/p/mybatis/issues/detail?id=557">Issue 557</a>
   */
  @Test
  void shouldRetainColumnNameCase() throws Exception {
    //如何mock的?
    final MappedStatement ms = getMappedStatement();

    final Executor executor = null;
    final ParameterHandler parameterHandler = null;
    final ResultHandler resultHandler = null;
    final BoundSql boundSql = null;
    final RowBounds rowBounds = new RowBounds(0, 100);
    //构造器传入
    final DefaultResultSetHandler fastResultSetHandler = new DefaultResultSetHandler(executor, ms, parameterHandler, resultHandler, boundSql, rowBounds);
    //stmt mock对象
    when(stmt.getResultSet()).thenReturn(rs);
    when(rs.getMetaData()).thenReturn(rsmd);
    when(rs.getType()).thenReturn(ResultSet.TYPE_FORWARD_ONLY);
    when(rs.next()).thenReturn(true).thenReturn(false);
    when(rs.getInt("CoLuMn1")).thenReturn(100);
    when(rsmd.getColumnCount()).thenReturn(1);
    when(rsmd.getColumnLabel(1)).thenReturn("CoLuMn1");
    when(rsmd.getColumnType(1)).thenReturn(Types.INTEGER);
    when(rsmd.getColumnClassName(1)).thenReturn(Integer.class.getCanonicalName());
    when(stmt.getConnection()).thenReturn(conn);
    when(conn.getMetaData()).thenReturn(dbmd);
    when(dbmd.supportsMultipleResultSets()).thenReturn(false); // for simplicity.
    //
    final List<Object> results = fastResultSetHandler.handleResultSets(stmt);
    assertEquals(1, results.size());
    assertEquals(100, ((HashMap) results.get(0)).get("cOlUmN1"));
  }

  @Test
  void shouldThrowExceptionWithColumnName() throws Exception {
    final MappedStatement ms = getMappedStatement();
    final RowBounds rowBounds = new RowBounds(0, 100);

    final DefaultResultSetHandler defaultResultSetHandler = new DefaultResultSetHandler(null/*executor*/, ms,
      null/*parameterHandler*/, null/*resultHandler*/, null/*boundSql*/, rowBounds);

    final ResultSetWrapper rsw = mock(ResultSetWrapper.class);
    when(rsw.getResultSet()).thenReturn(mock(ResultSet.class));

    final ResultMapping resultMapping = mock(ResultMapping.class);
    final TypeHandler typeHandler = mock(TypeHandler.class);
    when(resultMapping.getColumn()).thenReturn("column");
    when(resultMapping.getTypeHandler()).thenReturn(typeHandler);
    //any的用法.
    when(typeHandler.getResult(any(ResultSet.class), any(String.class))).thenThrow(new SQLException("exception"));
    List<ResultMapping> constructorMappings = Collections.singletonList(resultMapping);

    try {
      defaultResultSetHandler.createParameterizedResultObject(rsw, null/*resultType*/, constructorMappings,
        null/*constructorArgTypes*/, null/*constructorArgs*/, null/*columnPrefix*/);
      Assertions.fail("Should have thrown ExecutorException");
    } catch (Exception e) {
      Assertions.assertTrue(e instanceof ExecutorException, "Expected ExecutorException");
      Assertions.assertTrue(e.getMessage().contains("mapping: " + resultMapping.toString()));
    }
  }

  //mappedStatement是如何构造的.
  MappedStatement getMappedStatement() {
    final Configuration config = new Configuration();
    final TypeHandlerRegistry registry = config.getTypeHandlerRegistry();
    //
    ArrayList<ResultMap> list = new ArrayList<ResultMap>() {
      {
        add(new ResultMap
          .Builder(config, "testMap", HashMap.class, new ArrayList<ResultMapping>() {
          {
            add(new ResultMapping.Builder(config, "cOlUmN1", "CoLuMn1", registry.getTypeHandler(Integer.class)).build());
          }
        })
          .build());
      }
    };
    return new MappedStatement
      .Builder(config, "testSelect", new StaticSqlSource(config, "some select statement"), SqlCommandType.SELECT)
      .resultMaps(list)
      .build();
  }

  @Test
  public void testArrayList() {
    ArrayList<Integer> list = new ArrayList<Integer>() {
      {
        add(1);
        add(2);
      }
    };
    Assert.assertEquals(2, list.size());
    Map<Integer, Integer> map = new HashMap<Integer, Integer>() {
      {
        put(1, 1);
        put(2, 2);
      }
    };
    Assertions.assertEquals(2, map.size());
  }
}
