package ir.models;

import ir.enumDefine.BoolOptionSymbol;
import ir.enumDefine.FieldType;

/**
 *此类用来表示一行高级搜索框的值，包括关键词、域值、布尔运算符
 * @author 杨涛
 *
 */
public class BoolExpression {

	public String symbol;
	public String keyWords;
	public String field;
	
	public BoolExpression(String keyWords,String field,String symbol) {
		this.keyWords=keyWords;
		this.field=field;
		this.symbol=symbol;
	}
	
}