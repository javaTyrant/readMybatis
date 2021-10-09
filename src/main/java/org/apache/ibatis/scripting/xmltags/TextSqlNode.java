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

import java.util.regex.Pattern;

import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.parsing.TokenHandler;
import org.apache.ibatis.scripting.ScriptingException;
import org.apache.ibatis.type.SimpleTypeRegistry;

/**
 * @author Clinton Begin
 */
public class TextSqlNode implements SqlNode {
  //
  private final String text;
  //
  private final Pattern injectionFilter;

  public TextSqlNode(String text) {
    this(text, null);
  }

  public TextSqlNode(String text, Pattern injectionFilter) {
    this.text = text;
    this.injectionFilter = injectionFilter;
  }

  //是否是动态sql
  public boolean isDynamic() {
    //
    DynamicCheckerTokenParser checker = new DynamicCheckerTokenParser();
    //创建 ${} 检查
    GenericTokenParser parser = createParser(checker);
    //
    parser.parse(text);
    //
    return checker.isDynamic();
  }

  @Override
  public boolean apply(DynamicContext context) {
    GenericTokenParser parser = createParser(new BindingTokenParser(context, injectionFilter));
    context.appendSql(parser.parse(text));
    return true;
  }

  private GenericTokenParser createParser(TokenHandler handler) {
    //
    return new GenericTokenParser("${", "}", handler);
  }

  //
  private static class BindingTokenParser implements TokenHandler {
    //
    private DynamicContext context;
    //
    private Pattern injectionFilter;

    public BindingTokenParser(DynamicContext context, Pattern injectionFilter) {
      this.context = context;
      this.injectionFilter = injectionFilter;
    }

    //这里使用 GenericTokenParser 识别“${}”占位符，在识别到占位符之后，会通过 BindingTokenParser 将“${}”占位符替换为用户传入的实参。
    //BindingTokenParser 继承了TokenHandler 接口，
    //在其 handleToken() 方法实现中，会根据 DynamicContext.bindings 这个 ContextMap 中的 KV
    //数据替换 SQL 语句中的“${}”占位符，相关的代码片段如下：
    @Override
    public String handleToken(String content) {
      // 获取用户提供的实参数据
      Object parameter = context.getBindings().get("_parameter");
      if (parameter == null) {// 通过value占位符，也可以查找到parameter对象
        context.getBindings().put("value", null);
      } else if (SimpleTypeRegistry.isSimpleType(parameter.getClass())) {
        context.getBindings().put("value", parameter);
      }
      // 通过Ognl解析"${}"占位符中的表达式，解析失败的话会返回空字符串
      Object value = OgnlCache.getValue(content, context.getBindings());
      String srtValue = value == null ? "" : String.valueOf(value); // issue #274 return "" instead of "null"
      checkInjection(srtValue);
      return srtValue;
    }

    private void checkInjection(String value) {
      if (injectionFilter != null && !injectionFilter.matcher(value).matches()) {
        throw new ScriptingException("Invalid input. Please conform to regex" + injectionFilter.pattern());
      }
    }
  }

  //
  private static class DynamicCheckerTokenParser implements TokenHandler {
    //
    private boolean isDynamic;

    public DynamicCheckerTokenParser() {
      // Prevent Synthetic Access
    }

    public boolean isDynamic() {
      return isDynamic;
    }

    @Override
    public String handleToken(String content) {
      //
      this.isDynamic = true;
      //
      return null;
    }
  }

}
