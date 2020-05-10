/*
 * 无限制，免费的使用
 */

package com.nettalk;

import java.awt.Color;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * 用于向JTextPane组件添加文本，并不破坏其原有的格式
 * 
 * @author wbjtxh
 * @see 	JTextPane
 */

public class SwingTools {
	static SimpleAttributeSet attributeSet[] = getAttributeSets();

	enum MSG_INFO {
		NORMAL,INFO, WARNING
	}
	/**
	 * 初始化指定的元素数组属性
	 * 
	 * @return 所设定的元素属性数组
	 */
	private static SimpleAttributeSet[] getAttributeSets() {
		SimpleAttributeSet a[] = new SimpleAttributeSet[3];
		for (int i = 0; i < 3; i++) {
			a[i] = new SimpleAttributeSet();
			if (i == 0)
				StyleConstants.setForeground(a[i], Color.BLACK);
			if (i == 1)
				StyleConstants.setForeground(a[i], Color.GREEN);
			if (i == 2)
				StyleConstants.setForeground(a[i], Color.RED);
			StyleConstants.setItalic(a[i], i == 0?false:true);
		}
		return a;
	}
	
	/** 
	 * 用于向JTextPane追加字符串
	 * 
	 * @param 目标文本组件
	 * @param 字符串
	 * @param 信息类型
	 */ 
	static void appendSysInfo(JTextPane jTextPane, String str, MSG_INFO msg_type) {
		if (jTextPane == null || str == null)
			return;

		SimpleAttributeSet selector = null;
		switch (msg_type) {
		case NORMAL:
			selector = attributeSet[0];
		default:
			break;
		case INFO:
			selector = attributeSet[1];
			break;
		case WARNING:
			selector = attributeSet[2];
			break;

		}
		StyledDocument document = jTextPane.getStyledDocument();
		try {
			document.insertString(document.getLength(), str, selector);
		} catch (BadLocationException e1) {
			// 出错处理：文本插入失败
			e1.printStackTrace();
		}
	}
}
