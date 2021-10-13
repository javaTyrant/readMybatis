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
package org.apache.ibatis.executor;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.cache.TransactionalCacheManager;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;

import java.sql.SQLException;
import java.util.List;

/**
 * CachingExecutor 是我们最后一个要介绍的 Executor 接口实现类，
 * 它是一个 Executor 装饰器实现，会在其他 Executor 的基础之上添加二级缓存的相关功能。
 *在上文中提到的一级缓存中，其最大的共享范围就是一个SqlSession内部，如果多个SqlSession之间需要共享缓存，则需要使用到二级缓存。
 *开启二级缓存后，会使用CachingExecutor装饰Executor，进入一级缓存的查询流程前，先在CachingExecutor进行二级缓存的查询，具体的工作流程如下所示。
 * @author Clinton Begin
 * @author Eduardo Macarron
 * 结论:1.当sqlsession没有调用commit()方法时，二级缓存并没有起到作用。
 * 2.当提交事务时，sqlSession1查询完数据后，sqlSession2相同的查询是否会从缓存中获取数据。
 * sqlsession2的查询，使用了缓存，缓存的命中率是0.5。
 * 3.测试update操作是否会刷新该namespace下的二级缓存。
 * 在sqlSession3更新数据库，并提交事务后，sqlsession2的StudentMapper namespace下的查询走了数据库，没有走Cache。
 * 4.验证MyBatis的二级缓存不适应用于映射文件中存在多表查询的情况。
 * 在这个实验中，我们引入了两张新的表，一张class，一张classroom。class中保存了班级的id和班级名，classroom中保存了班级id和学生id。
 * 我们在StudentMapper中增加了一个查询方法getStudentByIdWithClassInfo，用于查询学生所在的班级，涉及到多表查询。
 * 在ClassMapper中添加了updateClassName，根据班级id更新班级名的操作。
 * 当sqlsession1的studentmapper查询数据后，二级缓存生效。保存在StudentMapper的namespace下的cache中。
 * 当sqlSession3的classMapper的updateClassName方法对class表进行更新时，updateClassName不属于StudentMapper的namespace，
 * 所以StudentMapper下的cache没有感应到变化，没有刷新缓存。当StudentMapper中同样的查询再次发起时，从缓存中读取了脏数据。
 * 5.为了解决实验4的问题呢，可以使用Cache ref，让ClassMapper引用StudenMapper命名空间，这样两个映射文件对应的SQL操作都使用的是同一块缓存了。
 * 缓存的粒度变粗了，多个Mapper namespace下的所有操作都会对缓存使用造成影响。
 */
public class CachingExecutor implements Executor {
  //
  private final Executor delegate;
  //
  private final TransactionalCacheManager tcm = new TransactionalCacheManager();

  public CachingExecutor(Executor delegate) {
    this.delegate = delegate;
    delegate.setExecutorWrapper(this);
  }

  @Override
  public Transaction getTransaction() {
    return delegate.getTransaction();
  }

  @Override
  public void close(boolean forceRollback) {
    try {
      //issues #499, #524 and #573
      if (forceRollback) {
        tcm.rollback();
      } else {
        tcm.commit();
      }
    } finally {
      delegate.close(forceRollback);
    }
  }

  @Override
  public boolean isClosed() {
    return delegate.isClosed();
  }

  @Override
  public int update(MappedStatement ms, Object parameterObject) throws SQLException {
    flushCacheIfRequired(ms);
    return delegate.update(ms, parameterObject);
  }

  //cacheEnabled 被设置为 true 时，才会开启二级缓存功能
  //SynchronizedCache -> LoggingCache -> SerializedCache -> LruCache -> PerpetualCache。
  //SynchronizedCache：同步Cache，实现比较简单，直接使用synchronized修饰方法。
  //LoggingCache：日志功能，装饰类，用于记录缓存的命中率，如果开启了DEBUG模式，则会输出命中率日志。
  //SerializedCache：序列化功能，将值序列化后存到缓存中。该功能用于缓存返回一份实例的Copy，用于保存线程安全。
  //LruCache：采用了Lru算法的Cache实现，移除最近最少使用的Key/Value。
  //PerpetualCache： 作为为最基础的缓存类，底层实现比较简单，直接使用了HashMap。
  @Override
  public <E> List<E> query(MappedStatement ms, Object parameterObject,
                           RowBounds rowBounds, ResultHandler resultHandler) throws SQLException {
    //获取BoundSql对象
    BoundSql boundSql = ms.getBoundSql(parameterObject);
    //创建相应的CacheKey
    CacheKey key = createCacheKey(ms, parameterObject, rowBounds, boundSql);
    //调用下面的query()方法重载
    return query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
  }

  @Override
  public <E> Cursor<E> queryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds) throws SQLException {
    flushCacheIfRequired(ms);
    return delegate.queryCursor(ms, parameter, rowBounds);
  }

  @Override
  public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds,
                           ResultHandler resultHandler, CacheKey key, BoundSql boundSql)
    throws SQLException {
    //// 获取该命名空间使用的二级缓存
    Cache cache = ms.getCache();
    if (cache != null) {
      // 根据<select>标签配置决定是否需要清空二级缓存.
      flushCacheIfRequired(ms);
      if (ms.isUseCache() && resultHandler == null) {
        //是否包含输出参数
        ensureNoOutParams(ms, boundSql);
        //查询二级缓存
        @SuppressWarnings("unchecked")
        List<E> list = (List<E>) tcm.getObject(cache, key);
        if (list == null) {
          //二级缓存未命中，通过被装饰的Executor对象查询结果对象
          list = delegate.query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
          //将查询结果放入TransactionalCache.entriesToAddOnCommit集合中暂存
          tcm.putObject(cache, key, list); // issue #578 and #116
        }
        return list;
      }
    }
    // 如果未开启二级缓存，直接通过被装饰的Executor对象查询结果对象
    return delegate.query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
  }

  @Override
  public List<BatchResult> flushStatements() throws SQLException {
    return delegate.flushStatements();
  }

  @Override
  public void commit(boolean required) throws SQLException {
    delegate.commit(required);
    tcm.commit();
  }

  @Override
  public void rollback(boolean required) throws SQLException {
    try {
      delegate.rollback(required);
    } finally {
      if (required) {
        tcm.rollback();
      }
    }
  }

  private void ensureNoOutParams(MappedStatement ms, BoundSql boundSql) {
    if (ms.getStatementType() == StatementType.CALLABLE) {
      for (ParameterMapping parameterMapping : boundSql.getParameterMappings()) {
        if (parameterMapping.getMode() != ParameterMode.IN) {
          throw new ExecutorException("Caching stored procedures with OUT params is not supported.  Please configure useCache=false in " + ms.getId() + " statement.");
        }
      }
    }
  }

  @Override
  public CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql) {
    return delegate.createCacheKey(ms, parameterObject, rowBounds, boundSql);
  }

  @Override
  public boolean isCached(MappedStatement ms, CacheKey key) {
    return delegate.isCached(ms, key);
  }

  @Override
  public void deferLoad(MappedStatement ms, MetaObject resultObject, String property, CacheKey key, Class<?> targetType) {
    delegate.deferLoad(ms, resultObject, property, key, targetType);
  }

  @Override
  public void clearLocalCache() {
    delegate.clearLocalCache();
  }

  private void flushCacheIfRequired(MappedStatement ms) {
    Cache cache = ms.getCache();
    if (cache != null && ms.isFlushCacheRequired()) {
      tcm.clear(cache);
    }
  }

  @Override
  public void setExecutorWrapper(Executor executor) {
    throw new UnsupportedOperationException("This method should not be called");
  }

}
