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

// 用于客户端的数据类
class MyClient {
	// 目标socket
	Socket socket;
	// 发送数据
	DataOutputStream out;
	// 接受数据
	DataInputStream in;
	// 线程，用于获得消息
	Thread msgThread;
	// 线程flag
	boolean flag_msg;
	// 主机IP
	String objip;
	// 该客户端名称
	String name;

	public MyClient() {
		// 初始化线程标志
		flag_msg = true;
	}

	void getMsgFromServer(JTextPane msgArea) {
		msgThread = new Thread(new Runnable() {
			@Override
			public void run() {
				// 线程：获取来自服务器的消息
				while (flag_msg) {
					try {
						// readline会阻塞
						String str = null;
						while ((str = in.readUTF()).length() > 0) {
							SwingTools.appendSysInfo(msgArea, str + "\n", SwingTools.MSG_INFO.NORMAL);
						}
						Thread.sleep(200);
					} catch (SocketException e) {
						// socket已关闭
						System.out.println("错误:socket被关闭");
						return;
					} catch (IOException e) {
						// IO错误
						e.printStackTrace();
						System.out.println("错误:从服务器获取消息失败,可能服务器已下线");
						return;
					} catch (NullPointerException e) {
						// socket为空错误
						System.out.println("错误:失去服务器连接");
						return;
					} catch (InterruptedException e) {
						// 延时错误
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

// 用于服务器端的数据类
class MyServer {
	// 服务器自身socket
	ServerSocket serverSocket;
	// 用于记录所有与服务器连接的socket
	ArrayList<Socket> clientSocket = new ArrayList<>();
	// 发送数据
	DataOutputStream out;
	// 接受数据
	DataInputStream in;
	// 线程，用于监听客户端连接
	Thread sockThread;
	// 线程，用于监听已连接的客户端消息
	Thread msgThread;
	// 线程flag
	boolean flag_sock;
	// 线程flag2
	boolean flag_msg;
	// 该服务器名称
	String name;

	public MyServer() {
		// 初始化线程标志
		flag_sock = true;
		flag_msg = true;
	}

	void close() {
		flag_msg = false;
		flag_sock = false;
		sockThread.interrupt();
		// 等待线程结束
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
					// 关闭时的IO流错误
					e.printStackTrace();
				}
				break;
			}
		}
	}

	void getMsgFromClient(JTextPane msgArea, JPanel jPanel) {
		msgThread = new Thread(new Runnable() {
			// 该线程用于获取是否有用户发送消息
			@Override
			public void run() {
				// 遍历
				while (flag_msg) {
					for (int i = 0; i < clientSocket.size(); i++) {
						try {
							in = new DataInputStream(clientSocket.get(i).getInputStream());
							String str;
							while ((str = in.readUTF()).length() > 0) {
								SwingTools.appendSysInfo(msgArea, str + "\n", SwingTools.MSG_INFO.NORMAL);
							}
						} catch (IOException e) {
							System.out.println("错误:获取客户端消息失败");
							SwingTools.appendSysInfo(msgArea, "<一个客户端已断开连接>\n", SwingTools.MSG_INFO.WARNING);
							clientSocket.remove(i);
							jPanel.setBorder(
									BorderFactory.createTitledBorder("当前用户(" + (clientSocket.size() + 1) + ")"));
							continue;
						} catch (Exception e) {
							e.printStackTrace();
							System.out.println("错误:全局获取客户端消息失败");
						}
					}
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// 延时失败
						e.printStackTrace();
					}
				}
			}
		});
		msgThread.start();
	}

	void startPortListener(JPanel jpanel) {
		sockThread = new Thread(new Runnable() {
			// 该线程用于监听是否有用户连接
			@Override
			public void run() {
				while (flag_sock) {
					try {
						// 此处进行对比，更新来自相同地址的请求
						{
							boolean isSame = false;
							// 此处进程阻塞
							Socket socket = serverSocket.accept();
							for (int index = 0; index < clientSocket.size(); index++) {
								System.out.println("新:" + socket.getInetAddress());
								System.out.println("旧:" + clientSocket.get(index).getInetAddress());
								if (socket.getInetAddress().toString()
										.equals(clientSocket.get(index).getInetAddress().toString())) {
									clientSocket.set(index, socket);
									isSame = true;
									System.out.println("来自同一地址的再次连接，目前已连接数：" + clientSocket.size());
								}
							}
							if (!isSame) {
								clientSocket.add(socket);
								jpanel.setBorder(BorderFactory.createTitledBorder("当前用户(" + clientSocket.size() + ")"));
							}
						}
						if (sockThread.isInterrupted()) {
							clientSocket.clear();
							return;
						}
					} catch (SocketTimeoutException e) {
						// 出错处理：等待连接超时
						// e.printStackTrace();
						// System.out.println("错误:3秒内无客户端连接");
					} catch (IOException e) {
						// 出错处理：监听套接字
						// e.printStackTrace();
						// System.out.println("错误:添加目标失败/IO错误");
					}
				}
			}
		});
		sockThread.start();
	}
}

// 主窗口界面
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
		// 聊天窗口主界面
		UIManager.put("TitledBorder.font", font);
		JFrame jframe = new JFrame();
		String str = isHost ? "网络通讯 - 服务器端" : "网络通讯 - 客户端";
		jframe.setTitle(str);
		jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jframe.setSize(400, 400);
		jframe.setResizable(false);
		// Note 必须先设置大小再进行居中
		jframe.setLocationRelativeTo(null);

		JPanel jPanel_info = new JPanel();
		jPanel_info.setBounds(260, 10, 120, 80);
		jPanel_info.setBorder(BorderFactory.createTitledBorder("主机信息"));
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
		// TODO 客户端的加入与删除
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
		jPanel_host.setBorder(BorderFactory.createTitledBorder("当前用户(1)"));
		jPanel_host.setFont(font);
		jPanel_host.setLayout(null);
		jPanel_host.add(jList);

		str = isHost ? "退出/关服" : "断开并退出";
		JButton j_exit = new JButton(str);
		j_exit.setBounds(270, 300, 100, 50);
		j_exit.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// 响应退出程序事件
				if (isHost) {
					myServer.close();
				} else {
					myClient.close();
				}
				System.exit(0);
			}
		});

		// 消息框
		JTextPane jTextPane = new JTextPane();
		jTextPane.setBounds(10, 10, 240, 280);
		jTextPane.setFont(font);
		jTextPane.setEditable(false);

		JScrollPane jScrollPane_info = new JScrollPane(jTextPane);
		jScrollPane_info.setBounds(10, 10, 240, 280);
		jScrollPane_info.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		jScrollPane_info.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

		// 文本输入框
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

		JButton j_clear = new JButton("清除");
		j_clear.setBounds(180, 325, 70, 25);
		j_clear.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// 清除消息编辑中的内容
				jInput.setText("");
			}
		});

		JButton j_send = new JButton("发送");
		j_send.setBounds(180, 300, 70, 25);
		j_send.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// 发送一条消息
				if (!isHost) {
					// 重新创建socket
					try {
						// if (myClient.socket == null) { myClient.socket = new
						// Socket(myClient.socket.getInetAddress(), 44444);System.out.println("rtgt" );}
						if (myClient.out == null) {
							myClient.out = new DataOutputStream(myClient.socket.getOutputStream());
						}
						myClient.out.writeUTF(myClient.name + ":" + jInput.getText());
						System.out.println("客户端发送消息：" + jInput.getText());
						SwingTools.appendSysInfo(jTextPane, myClient.name + ":" + jInput.getText() + "\n",
								SwingTools.MSG_INFO.NORMAL);
						jInput.setText("");
						myClient.out.flush();
						// myClient.socket = null;
					} catch (Exception e1) {
						// 客户端发送消息错误:未知错误
						e1.getStackTrace();
						jInput.setText("错误:消息发送失败");
					}
				} else {
					try {
						if (myServer.clientSocket.size() == 0) {
							String str_info = "<无任何客户端连接>\n";
							SwingTools.appendSysInfo(jTextPane, str_info, SwingTools.MSG_INFO.WARNING);
							return;
						}
						for (int i = 0; i < myServer.clientSocket.size(); i++) {
							myServer.out = new DataOutputStream(myServer.clientSocket.get(i).getOutputStream());
							if (myServer.out != null) {
								myServer.out.writeUTF(myServer.name + ":" + jInput.getText());
								System.out.println("服务器发送消息：" + jInput.getText());
								myServer.out.flush();
							} else {
								System.out.println("无法发送消息，目标客户端可能已离线");
							}
							SwingTools.appendSysInfo(jTextPane, myServer.name + ":" + jInput.getText() + "\n",
									SwingTools.MSG_INFO.NORMAL);
							jInput.setText("");
						}
					} catch (IOException e1) {
						// 服务器发送消息错误：IO流
						e1.printStackTrace();
					}
				}
			}
		});

		if (isHost) {
			// 启动监听端口线程
			myServer.startPortListener(jPanel_host);
			// 启动获取消息线程
			myServer.getMsgFromClient(jTextPane, jPanel_host);
		} else {
			// 启动接受消息线程
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

		// 将一切显示出来
		jframe.setVisible(true);
	}
}

public class NetTalk {

	static JButton j_link;
	MyClient myClient = new MyClient();
	MyServer myServer = new MyServer();
	// JFrame框架
	JFrame jf;
	// 用户名
	JTextField jt_user;
	// IP地址
	JTextField jt_address;
	// 端口
	JTextField jt_port;
	// 全局字体
	Font font;

	public static void main(String args[]) {
		NetTalk netTalk = new NetTalk();
		try {
			netTalk.login();
		} catch (Exception e) {
			// 登录框
		}
	}

	void login() throws UnknownHostException {
		// 设置窗口和字体风格
		setWindowStyle();
		font = new Font("微软雅黑", Font.PLAIN, 14);
		UIManager.put("TitledBorder.font", font);

		jf = new JFrame("登录");
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jf.setSize(300, 200);
		jf.setResizable(false);
		// Note 必须先设置大小再进行居中
		jf.setLocationRelativeTo(null);
		Container c = jf.getContentPane();
		c.setLayout(null);

		JLabel j_ip = new JLabel(InetAddress.getLocalHost().getHostAddress());
		j_ip.setBounds(0, 0, 130, 55);
		j_ip.setFont(font);

		JPanel jPanel_ip = new JPanel();
		jPanel_ip.setBounds(135, 65, 140, 55);
		jPanel_ip.setBorder(BorderFactory.createTitledBorder("本机IP"));
		jPanel_ip.setFont(font);
		jPanel_ip.add(j_ip);

		JLabel j_user = new JLabel("用户名：");
		j_user.setToolTipText("默认显示本机名，更改以个性化名称");
		j_user.setBounds(10, 10, 100, 25);
		j_user.setFont(font);

		jt_user = new JTextField();
		jt_user.setText(InetAddress.getLocalHost().getHostName());
		jt_user.getCaret().setDot(jt_user.getText().length());
		jt_user.setBounds(80, 10, 190, 22);
		jt_user.setFont(font);

		JLabel j_objhost = new JLabel("目标主机：");
		j_objhost.setBounds(10, 35, 100, 25);
		j_objhost.setFont(font);

		// 前置声明
		JButton j_server = new JButton("创建服务器");
		jt_address = new JTextField();
		jt_port = new JTextField();
		// 端口号
		jt_port.setBounds(30, 22, 60, 22);
		jt_port.setText("7777");
		jt_port.setTransferHandler(null);
		jt_port.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
				// 键盘输入
				if (e.getKeyChar() < KeyEvent.VK_0 || e.getKeyChar() > KeyEvent.VK_9)
					e.consume();
				if (jt_port.getText().length() > 4)
					e.consume();
			}

			@Override
			public void keyReleased(KeyEvent e) {
				// 按键释放

			}

			@Override
			public void keyPressed(KeyEvent e) {
				// 按键按下

			}
		});
		jt_port.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {
				// 文本移除事件
				if (jt_port.getText().isEmpty()) {
					j_link.setEnabled(false);
					j_server.setEnabled(false);
				} else {
					j_server.setEnabled(true);
				}
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				// 文本插入事件
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
				// 文本改变事件
			}
		});
		jt_port.setFont(font);

		JPanel jPanel_port = new JPanel();
		jPanel_port.setBounds(10, 65, 120, 55);
		jPanel_port.setBorder(BorderFactory.createTitledBorder("端口号"));
		jPanel_port.setFont(font);
		jPanel_port.setLayout(null);
		jPanel_port.add(jt_port);

		jt_address.setBounds(80, 35, 190, 22);
		jt_address.setFont(font);
		jt_address.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {
				// 文本移除事件
				if (jt_address.getText().isEmpty())
					j_link.setEnabled(false);
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				// 文本插入事件
				if (jt_address.getText().isBlank() || jt_port.getText().isBlank())
					j_link.setEnabled(false);
				else {
					j_link.setEnabled(true);
				}
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				// 文本改变事件
			}
		});

		j_link = new JButton("连接");
		j_link.setBounds(130, 130, 70, 25);
		j_link.setFont(font);
		j_link.setEnabled(false);
		j_link.addActionListener(new connectTo());

		JButton j_cancel = new JButton("退出");
		j_cancel.setBounds(210, 130, 70, 25);
		j_cancel.setFont(font);
		j_cancel.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// 按钮：退出程序
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
		// 将一切显示出来
		jf.setVisible(true);
	}

	static void setWindowStyle() {
		// 切换窗口风格
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

		// 连接指定服务器
		@Override
		public void actionPerformed(ActionEvent e) {
			j_link.setEnabled(false);
			// 根据通配符判断ip地址格式
			String regex = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";
			if (!jt_address.getText().matches(regex)) {
				Toolkit.getDefaultToolkit().beep();
				JOptionPane.showMessageDialog(null, "无效的目标服务器地址", "提示", JOptionPane.INFORMATION_MESSAGE);
				j_link.setEnabled(true);
				return;
			}
			// 连接目标地址
			try {
				myClient.objip = jt_address.getText();
				myClient.socket = new Socket(myClient.objip, Integer.valueOf(jt_port.getText()));
				myClient.in = new DataInputStream(myClient.socket.getInputStream());
				myClient.name = jt_user.getText();
			} catch (ConnectException e3) {
				Toolkit.getDefaultToolkit().beep();
				JOptionPane.showMessageDialog(null, "连接超时或无法连接服务器", "提示", JOptionPane.INFORMATION_MESSAGE);
				j_link.setEnabled(true);
				return;
			} catch (IOException e3) {
				// e2.printStackTrace();
				Toolkit.getDefaultToolkit().beep();
				JOptionPane.showMessageDialog(null, "IO流错误", "提示", JOptionPane.INFORMATION_MESSAGE);
				j_link.setEnabled(true);
				return;
			}
			jf.setVisible(false);
			MainWindow mainWindow = new MainWindow(false, font, myClient);
			try {
				mainWindow.show();
			} catch (UnknownHostException e1) {
				// 未知错误
				e1.printStackTrace();
			}
		}
	}

	private class createServer implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			// 创建服务器套接字
			try {
				myServer.serverSocket = new ServerSocket(Integer.valueOf(jt_port.getText()));
				myServer.serverSocket.setSoTimeout(1000);
				myServer.name = jt_user.getText();
			} catch (BindException e2) {
				// 出错处理：重复创建服务器
				Toolkit.getDefaultToolkit().beep();
				JOptionPane.showMessageDialog(null, "无法创建服务器，可能该地址已被占用。", "提示", JOptionPane.ERROR_MESSAGE);
				return;
			} catch (IOException e2) {
				// 出错处理：创建套接字
				e2.printStackTrace();
			}
			jf.setVisible(false);
			MainWindow mainWindow = new MainWindow(true, font, myServer);
			try {
				mainWindow.show();
			} catch (UnknownHostException e1) {
				// 主界面抛出的未知错误
				e1.printStackTrace();
			}
		}
	}
}