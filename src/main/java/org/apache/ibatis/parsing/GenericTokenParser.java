package org.apache.ibatis.parsing;

/**
 * sql解析器,解析字符串里 ${} #{}这种
 *
 * @author Clinton Begin
 */
public class GenericTokenParser {
  //
  private final String openToken;
  //
  private final String closeToken;
  //
  private final TokenHandler handler;

  public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
    this.openToken = openToken;
    this.closeToken = closeToken;
    this.handler = handler;
  }

  //替换#{},${}字符.GenericTokenParserTest看看这个单元测试就知道了.
  public String parse(String text) {
    if (text == null || text.isEmpty()) {
      return "";
    }
    // search open token
    int start = text.indexOf(openToken);
    //open token不存在.
    if (start == -1) {
      return text;
    }
    //toCharArray
    char[] src = text.toCharArray();
    //
    int offset = 0;
    final StringBuilder builder = new StringBuilder();
    StringBuilder expression = null;
    while (start > -1) {
      //转义字符
      //如果有转义字符串.
      if (start > 0 && src[start - 1] == '\\') {
        // this open token is escaped. remove the backslash and continue.
        builder.append(src, offset, start - offset - 1).append(openToken);
        offset = start + openToken.length();
      } else {
        // found open token. let's search close token.
        if (expression == null) {
          expression = new StringBuilder();
        } else {
          expression.setLength(0);
        }
        builder.append(src, offset, start - offset);
        //offset要偏移,以便于找到下一个start
        offset = start + openToken.length();
        int end = text.indexOf(closeToken, offset);
        while (end > -1) {
          if (end > offset && src[end - 1] == '\\') {
            // this close token is escaped. remove the backslash and continue.
            expression.append(src, offset, end - offset - 1).append(closeToken);
            offset = end + closeToken.length();
            end = text.indexOf(closeToken, offset);
          } else {
            expression.append(src, offset, end - offset);
            break;
          }
        }
        if (end == -1) {
          // close token was not found.
          builder.append(src, start, src.length - start);
          offset = src.length;
        } else {
          //由不同的子类来实现token的替换,更加的自由.
          builder.append(handler.handleToken(expression.toString()));
          offset = end + closeToken.length();
        }
      }
      //对api真的熟悉.
      start = text.indexOf(openToken, offset);
      System.out.println("start is :" + start);
    }
    if (offset < src.length) {
      builder.append(src, offset, src.length - offset);
    }
    return builder.toString();
  }
}
