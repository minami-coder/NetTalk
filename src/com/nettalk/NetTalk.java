/**
 * @author minami
 * @time 	2020-5-9
 */

package com.nettalk;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

// ���ڿͻ��˵�������
class MyClient {
	// Ŀ��socket
	Socket socket;
	// ��������
	DataOutputStream out;
	// ��������
	DataInputStream in;
	// �̣߳����ڻ����Ϣ
	Thread msgThread;
	// �߳�flag
	boolean flag_msg;
	// ����IP
	String objip;
	// �ÿͻ�������
	String name;

	public MyClient() {
		// ��ʼ���̱߳�־
		flag_msg = true;
	}

	void getMsgFromServer(JTextPane msgArea) {
		msgThread = new Thread(new Runnable() {
			@Override
			public void run() {
				// �̣߳���ȡ���Է���������Ϣ
				while (flag_msg) {
					try {
						// readline������
						String str = null;
						while ((str = in.readUTF()).length() > 0) {
							SwingTools.appendSysInfo(msgArea, str + "\n", SwingTools.MSG_INFO.NORMAL);
						}
						Thread.sleep(200);
					} catch (SocketException e) {
						// socket�ѹر�
						System.out.println("����:socket���ر�");
						return;
					} catch (IOException e) {
						// IO����
						e.printStackTrace();
						System.out.println("����:�ӷ�������ȡ��Ϣʧ��,���ܷ�����������");
						return;
					} catch (NullPointerException e) {
						// socketΪ�մ���
						System.out.println("����:ʧȥ����������");
						return;
					} catch (InterruptedException e) {
						// ��ʱ����
						e.printStackTrace();
					}
				}
			}
		});
		msgThread.start();
	}

	void close() {
		try {
			flag_msg = false;
			if (socket != null)
				socket.close();
			if (in != null)
				in.close();
			if (out != null)
				out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

// ���ڷ������˵�������
class MyServer {
	// ����������socket
	ServerSocket serverSocket;
	// ���ڼ�¼��������������ӵ�socket
	ArrayList<Socket> clientSocket = new ArrayList<>();
	// ��������
	DataOutputStream out;
	// ��������
	DataInputStream in;
	// �̣߳����ڼ����ͻ�������
	Thread sockThread;
	// �̣߳����ڼ��������ӵĿͻ�����Ϣ
	Thread msgThread;
	// �߳�flag
	boolean flag_sock;
	// �߳�flag2
	boolean flag_msg;
	// �÷���������
	String name;

	public MyServer() {
		// ��ʼ���̱߳�־
		flag_sock = true;
		flag_msg = true;
	}

	void close() {
		flag_msg = false;
		flag_sock = false;
		sockThread.interrupt();
		// �ȴ��߳̽���
		while (true) {
			if (!sockThread.isAlive()) {
				try {
					if (in != null)
						in.close();
					if (out != null)
						out.close();
					for (Socket i : clientSocket) {
						if (i != null)
							i.close();
					}
					if (serverSocket != null)
						serverSocket.close();
				} catch (IOException e) {
					// �ر�ʱ��IO������
					e.printStackTrace();
				}
				break;
			}
		}
	}

	void getMsgFromClient(JTextPane msgArea, JPanel jPanel) {
		msgThread = new Thread(new Runnable() {
			// ���߳����ڻ�ȡ�Ƿ����û�������Ϣ
			@Override
			public void run() {
				// ����
				while (flag_msg) {
					for (int i = 0; i < clientSocket.size(); i++) {
						try {
							in = new DataInputStream(clientSocket.get(i).getInputStream());
							String str;
							while ((str = in.readUTF()).length() > 0) {
								SwingTools.appendSysInfo(msgArea, str + "\n", SwingTools.MSG_INFO.NORMAL);
							}
						} catch (IOException e) {
							System.out.println("����:��ȡ�ͻ�����Ϣʧ��");
							SwingTools.appendSysInfo(msgArea, "<һ���ͻ����ѶϿ�����>\n", SwingTools.MSG_INFO.WARNING);
							clientSocket.remove(i);
							jPanel.setBorder(
									BorderFactory.createTitledBorder("��ǰ�û�(" + (clientSocket.size() + 1) + ")"));
							continue;
						} catch (Exception e) {
							e.printStackTrace();
							System.out.println("����:ȫ�ֻ�ȡ�ͻ�����Ϣʧ��");
						}
					}
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// ��ʱʧ��
						e.printStackTrace();
					}
				}
			}
		});
		msgThread.start();
	}

	void startPortListener(JPanel jpanel) {
		sockThread = new Thread(new Runnable() {
			// ���߳����ڼ����Ƿ����û�����
			@Override
			public void run() {
				while (flag_sock) {
					try {
						// �˴����жԱȣ�����������ͬ��ַ������
						{
							boolean isSame = false;
							// �˴���������
							Socket socket = serverSocket.accept();
							for (int index = 0; index < clientSocket.size(); index++) {
								System.out.println("��:" + socket.getInetAddress());
								System.out.println("��:" + clientSocket.get(index).getInetAddress());
								if (socket.getInetAddress().toString()
										.equals(clientSocket.get(index).getInetAddress().toString())) {
									clientSocket.set(index, socket);
									isSame = true;
									System.out.println("����ͬһ��ַ���ٴ����ӣ�Ŀǰ����������" + clientSocket.size());
								}
							}
							if (!isSame) {
								clientSocket.add(socket);
								jpanel.setBorder(BorderFactory.createTitledBorder("��ǰ�û�(" + clientSocket.size() + ")"));
							}
						}
						if (sockThread.isInterrupted()) {
							clientSocket.clear();
							return;
						}
					} catch (SocketTimeoutException e) {
						// �������ȴ����ӳ�ʱ
						// e.printStackTrace();
						// System.out.println("����:3�����޿ͻ�������");
					} catch (IOException e) {
						// �����������׽���
						// e.printStackTrace();
						// System.out.println("����:���Ŀ��ʧ��/IO����");
					}
				}
			}
		});
		sockThread.start();
	}
}

// �����ڽ���
class MainWindow {
	MyServer myServer;
	MyClient myClient;
	Font font;
	boolean isHost;

	public MainWindow(Boolean isHost, Font font, Object obj) {
		this.font = font;
		this.isHost = isHost;
		if (this.isHost)
			myServer = (MyServer) obj;
		else {
			myClient = (MyClient) obj;
		}
	}

	void show() throws UnknownHostException {
		// ���촰��������
		UIManager.put("TitledBorder.font", font);
		JFrame jframe = new JFrame();
		String str = isHost ? "����ͨѶ - ��������" : "����ͨѶ - �ͻ���";
		jframe.setTitle(str);
		jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jframe.setSize(400, 400);
		jframe.setResizable(false);
		// Note ���������ô�С�ٽ��о���
		jframe.setLocationRelativeTo(null);

		JPanel jPanel_info = new JPanel();
		jPanel_info.setBounds(260, 10, 120, 80);
		jPanel_info.setBorder(BorderFactory.createTitledBorder("������Ϣ"));
		jPanel_info.setFont(font);

		JLabel jLabel_name = new JLabel();
		jLabel_name.setBounds(0, 0, 100, 15);
		jLabel_name.setText(InetAddress.getLocalHost().getHostName());
		jLabel_name.setFont(font);

		JLabel jLabel_ip = new JLabel();
		jLabel_ip.setBounds(0, 20, 100, 15);
		jLabel_ip.setText(InetAddress.getLocalHost().getHostAddress());
		jLabel_ip.setFont(font);

		jPanel_info.add(jLabel_name);
		jPanel_info.add(jLabel_ip);

		DefaultListModel<String> jListModel = new DefaultListModel<>();
		if (isHost)
			jListModel.addElement(myServer.name);
		else {
			jListModel.addElement(myClient.name);
		}
		// TODO �ͻ��˵ļ�����ɾ��
		JList<String> jList = new JList<>(jListModel);
		jList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jList.setBounds(10, 20, 100, 160);
		jList.setBackground(new Color(240, 240, 240));
		jList.setFont(font);
		DefaultListCellRenderer defaultListCellRenderer = new DefaultListCellRenderer();
		defaultListCellRenderer.setHorizontalAlignment(SwingConstants.CENTER);
		jList.setCellRenderer(defaultListCellRenderer);

		JPanel jPanel_host = new JPanel();
		jPanel_host.setBounds(260, 100, 120, 190);
		jPanel_host.setBorder(BorderFactory.createTitledBorder("��ǰ�û�(1)"));
		jPanel_host.setFont(font);
		jPanel_host.setLayout(null);
		jPanel_host.add(jList);

		str = isHost ? "�˳�/�ط�" : "�Ͽ����˳�";
		JButton j_exit = new JButton(str);
		j_exit.setBounds(270, 300, 100, 50);
		j_exit.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// ��Ӧ�˳������¼�
				if (isHost) {
					myServer.close();
				} else {
					myClient.close();
				}
				System.exit(0);
			}
		});

		// ��Ϣ��
		JTextPane jTextPane = new JTextPane();
		jTextPane.setBounds(10, 10, 240, 280);
		jTextPane.setFont(font);
		jTextPane.setEditable(false);

		JScrollPane jScrollPane_info = new JScrollPane(jTextPane);
		jScrollPane_info.setBounds(10, 10, 240, 280);
		jScrollPane_info.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		jScrollPane_info.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

		// �ı������
		JTextArea jInput = new JTextArea();
		jInput.setRows(2);
		jInput.setBounds(10, 300, 165, 50);
		jInput.setLineWrap(true);
		jInput.setWrapStyleWord(true);
		jInput.setFont(font);

		JScrollPane jScrollPane_input = new JScrollPane(jInput);
		jScrollPane_input.setBounds(10, 300, 165, 50);
		jScrollPane_input.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		jScrollPane_input.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

		JButton j_clear = new JButton("���");
		j_clear.setBounds(180, 325, 70, 25);
		j_clear.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// �����Ϣ�༭�е�����
				jInput.setText("");
			}
		});

		JButton j_send = new JButton("����");
		j_send.setBounds(180, 300, 70, 25);
		j_send.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// ����һ����Ϣ
				if (!isHost) {
					// ���´���socket
					try {
						// if (myClient.socket == null) { myClient.socket = new
						// Socket(myClient.socket.getInetAddress(), 44444);System.out.println("rtgt" );}
						if (myClient.out == null) {
							myClient.out = new DataOutputStream(myClient.socket.getOutputStream());
						}
						myClient.out.writeUTF(myClient.name + ":" + jInput.getText());
						System.out.println("�ͻ��˷�����Ϣ��" + jInput.getText());
						SwingTools.appendSysInfo(jTextPane, myClient.name + ":" + jInput.getText() + "\n",
								SwingTools.MSG_INFO.NORMAL);
						jInput.setText("");
						myClient.out.flush();
						// myClient.socket = null;
					} catch (Exception e1) {
						// �ͻ��˷�����Ϣ����:δ֪����
						e1.getStackTrace();
						jInput.setText("����:��Ϣ����ʧ��");
					}
				} else {
					try {
						if (myServer.clientSocket.size() == 0) {
							String str_info = "<���κοͻ�������>\n";
							SwingTools.appendSysInfo(jTextPane, str_info, SwingTools.MSG_INFO.WARNING);
							return;
						}
						for (int i = 0; i < myServer.clientSocket.size(); i++) {
							myServer.out = new DataOutputStream(myServer.clientSocket.get(i).getOutputStream());
							if (myServer.out != null) {
								myServer.out.writeUTF(myServer.name + ":" + jInput.getText());
								System.out.println("������������Ϣ��" + jInput.getText());
								myServer.out.flush();
							} else {
								System.out.println("�޷�������Ϣ��Ŀ��ͻ��˿���������");
							}
							SwingTools.appendSysInfo(jTextPane, myServer.name + ":" + jInput.getText() + "\n",
									SwingTools.MSG_INFO.NORMAL);
							jInput.setText("");
						}
					} catch (IOException e1) {
						// ������������Ϣ����IO��
						e1.printStackTrace();
					}
				}
			}
		});

		if (isHost) {
			// ���������˿��߳�
			myServer.startPortListener(jPanel_host);
			// ������ȡ��Ϣ�߳�
			myServer.getMsgFromClient(jTextPane, jPanel_host);
		} else {
			// ����������Ϣ�߳�
			myClient.getMsgFromServer(jTextPane);
		}

		Container container = jframe.getContentPane();
		jframe.getRootPane().setDefaultButton(j_send);
		container.setLayout(null);
		container.add(jScrollPane_info);
		container.add(jScrollPane_input);
		container.add(j_send);
		container.add(j_clear);
		container.add(jPanel_info);
		container.add(jPanel_host);
		container.add(j_exit);

		// ��һ����ʾ����
		jframe.setVisible(true);
	}
}

public class NetTalk {

	static JButton j_link;
	MyClient myClient = new MyClient();
	MyServer myServer = new MyServer();
	// JFrame���
	JFrame jf;
	// �û���
	JTextField jt_user;
	// IP��ַ
	JTextField jt_address;
	// �˿�
	JTextField jt_port;
	// ȫ������
	Font font;

	public static void main(String args[]) {
		NetTalk netTalk = new NetTalk();
		try {
			netTalk.login();
		} catch (Exception e) {
			// ��¼��
		}
	}

	void login() throws UnknownHostException {
		// ���ô��ں�������
		setWindowStyle();
		font = new Font("΢���ź�", Font.PLAIN, 14);
		UIManager.put("TitledBorder.font", font);

		jf = new JFrame("��¼");
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jf.setSize(300, 200);
		jf.setResizable(false);
		// Note ���������ô�С�ٽ��о���
		jf.setLocationRelativeTo(null);
		Container c = jf.getContentPane();
		c.setLayout(null);

		JLabel j_ip = new JLabel(InetAddress.getLocalHost().getHostAddress());
		j_ip.setBounds(0, 0, 130, 55);
		j_ip.setFont(font);

		JPanel jPanel_ip = new JPanel();
		jPanel_ip.setBounds(135, 65, 140, 55);
		jPanel_ip.setBorder(BorderFactory.createTitledBorder("����IP"));
		jPanel_ip.setFont(font);
		jPanel_ip.add(j_ip);

		JLabel j_user = new JLabel("�û�����");
		j_user.setToolTipText("Ĭ����ʾ�������������Ը��Ի�����");
		j_user.setBounds(10, 10, 100, 25);
		j_user.setFont(font);

		jt_user = new JTextField();
		jt_user.setText(InetAddress.getLocalHost().getHostName());
		jt_user.getCaret().setDot(jt_user.getText().length());
		jt_user.setBounds(80, 10, 190, 22);
		jt_user.setFont(font);

		JLabel j_objhost = new JLabel("Ŀ��������");
		j_objhost.setBounds(10, 35, 100, 25);
		j_objhost.setFont(font);

		// ǰ������
		JButton j_server = new JButton("����������");
		jt_address = new JTextField();
		jt_port = new JTextField();
		// �˿ں�
		jt_port.setBounds(30, 22, 60, 22);
		jt_port.setText("7777");
		jt_port.setTransferHandler(null);
		jt_port.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
				// ��������
				if (e.getKeyChar() < KeyEvent.VK_0 || e.getKeyChar() > KeyEvent.VK_9)
					e.consume();
				if (jt_port.getText().length() > 4)
					e.consume();
			}

			@Override
			public void keyReleased(KeyEvent e) {
				// �����ͷ�

			}

			@Override
			public void keyPressed(KeyEvent e) {
				// ��������

			}
		});
		jt_port.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {
				// �ı��Ƴ��¼�
				if (jt_port.getText().isEmpty()) {
					j_link.setEnabled(false);
					j_server.setEnabled(false);
				} else {
					j_server.setEnabled(true);
				}
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				// �ı������¼�
				if (jt_address.getText().isBlank() || jt_port.getText().isBlank())
					j_link.setEnabled(false);
				else {
					j_link.setEnabled(true);
				}
				if (!jt_port.getText().isEmpty()) {
					j_server.setEnabled(true);
				}
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				// �ı��ı��¼�
			}
		});
		jt_port.setFont(font);

		JPanel jPanel_port = new JPanel();
		jPanel_port.setBounds(10, 65, 120, 55);
		jPanel_port.setBorder(BorderFactory.createTitledBorder("�˿ں�"));
		jPanel_port.setFont(font);
		jPanel_port.setLayout(null);
		jPanel_port.add(jt_port);

		jt_address.setBounds(80, 35, 190, 22);
		jt_address.setFont(font);
		jt_address.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {
				// �ı��Ƴ��¼�
				if (jt_address.getText().isEmpty())
					j_link.setEnabled(false);
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				// �ı������¼�
				if (jt_address.getText().isBlank() || jt_port.getText().isBlank())
					j_link.setEnabled(false);
				else {
					j_link.setEnabled(true);
				}
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				// �ı��ı��¼�
			}
		});

		j_link = new JButton("����");
		j_link.setBounds(130, 130, 70, 25);
		j_link.setFont(font);
		j_link.setEnabled(false);
		j_link.addActionListener(new connectTo());

		JButton j_cancel = new JButton("�˳�");
		j_cancel.setBounds(210, 130, 70, 25);
		j_cancel.setFont(font);
		j_cancel.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// ��ť���˳�����
				myClient.close();
				System.exit(0);
			}
		});

		j_server.setBounds(10, 130, 110, 25);
		j_server.setFont(font);
		j_server.addActionListener(new createServer());

		c.add(jPanel_ip);
		c.add(jPanel_port);
		c.add(j_user);
		c.add(jt_user);
		c.add(jt_address);
		c.add(j_objhost);
		c.add(j_link);
		c.add(j_cancel);
		c.add(j_server);
		// ��һ����ʾ����
		jf.setVisible(true);
	}

	static void setWindowStyle() {
		// �л����ڷ��
		String look = UIManager.getSystemLookAndFeelClassName();
		try {
			UIManager.setLookAndFeel(look);
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		} catch (InstantiationException e1) {
			e1.printStackTrace();
		} catch (IllegalAccessException e1) {
			e1.printStackTrace();
		} catch (UnsupportedLookAndFeelException e1) {
			e1.printStackTrace();
		}
	}

	private class connectTo implements ActionListener {

		// ����ָ��������
		@Override
		public void actionPerformed(ActionEvent e) {
			j_link.setEnabled(false);
			// ����ͨ����ж�ip��ַ��ʽ
			String regex = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";
			if (!jt_address.getText().matches(regex)) {
				Toolkit.getDefaultToolkit().beep();
				JOptionPane.showMessageDialog(null, "��Ч��Ŀ���������ַ", "��ʾ", JOptionPane.INFORMATION_MESSAGE);
				j_link.setEnabled(true);
				return;
			}
			// ����Ŀ���ַ
			try {
				myClient.objip = jt_address.getText();
				myClient.socket = new Socket(myClient.objip, Integer.valueOf(jt_port.getText()));
				myClient.in = new DataInputStream(myClient.socket.getInputStream());
				myClient.name = jt_user.getText();
			} catch (ConnectException e3) {
				Toolkit.getDefaultToolkit().beep();
				JOptionPane.showMessageDialog(null, "���ӳ�ʱ���޷����ӷ�����", "��ʾ", JOptionPane.INFORMATION_MESSAGE);
				j_link.setEnabled(true);
				return;
			} catch (IOException e3) {
				// e2.printStackTrace();
				Toolkit.getDefaultToolkit().beep();
				JOptionPane.showMessageDialog(null, "IO������", "��ʾ", JOptionPane.INFORMATION_MESSAGE);
				j_link.setEnabled(true);
				return;
			}
			jf.setVisible(false);
			MainWindow mainWindow = new MainWindow(false, font, myClient);
			try {
				mainWindow.show();
			} catch (UnknownHostException e1) {
				// δ֪����
				e1.printStackTrace();
			}
		}
	}

	private class createServer implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			// �����������׽���
			try {
				myServer.serverSocket = new ServerSocket(Integer.valueOf(jt_port.getText()));
				myServer.serverSocket.setSoTimeout(1000);
				myServer.name = jt_user.getText();
			} catch (BindException e2) {
				// �������ظ�����������
				Toolkit.getDefaultToolkit().beep();
				JOptionPane.showMessageDialog(null, "�޷����������������ܸõ�ַ�ѱ�ռ�á�", "��ʾ", JOptionPane.ERROR_MESSAGE);
				return;
			} catch (IOException e2) {
				// �����������׽���
				e2.printStackTrace();
			}
			jf.setVisible(false);
			MainWindow mainWindow = new MainWindow(true, font, myServer);
			try {
				mainWindow.show();
			} catch (UnknownHostException e1) {
				// �������׳���δ֪����
				e1.printStackTrace();
			}
		}
	}
}