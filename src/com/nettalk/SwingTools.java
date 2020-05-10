/*
 * �����ƣ���ѵ�ʹ��
 */

package com.nettalk;

import java.awt.Color;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * ������JTextPane�������ı��������ƻ���ԭ�еĸ�ʽ
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
	 * ��ʼ��ָ����Ԫ����������
	 * 
	 * @return ���趨��Ԫ����������
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
	 * ������JTextPane׷���ַ���
	 * 
	 * @param Ŀ���ı����
	 * @param �ַ���
	 * @param ��Ϣ����
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
			// �������ı�����ʧ��
			e1.printStackTrace();
		}
	}
}
