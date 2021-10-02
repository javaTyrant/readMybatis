package org.apache.ibatis.plugin.demo;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Statement;
import java.util.Properties;

/**
 * @author lumac
 * @since 2021/2/27
 */
//Intercepts注解在哪里扫描的:Plugin:getSignatureMap
@Intercepts({@Signature(type = StatementHandler.class, method = "query", args = {Statement.class, ResultHandler.class}),
  @Signature(type = StatementHandler.class, method = "update", args = {Statement.class}),
  @Signature(type = StatementHandler.class, method = "batch", args = {Statement.class})})
public class SqlCostTimeInterceptor implements Interceptor {
  public static final Logger logger = LoggerFactory.getLogger(SqlCostTimeInterceptor.class);

  public Object intercept(Invocation invocation) throws Throwable {
    StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
    long start = System.currentTimeMillis();
    try {
      //执行拦截的方法:执行sql的方法
      return invocation.proceed();
    } finally {
      //获取boundsql
      BoundSql boundSql = statementHandler.getBoundSql();
      String sql = boundSql.getSql();
      long end = System.currentTimeMillis();
      long cost = end - start;
      logger.info("{}, cost is {}", sql, cost);
    }
  }

  public Object plugin(Object target) {
    return Plugin.wrap(target, this);
  }

  public void setProperties(Properties properties) {

  }
}
