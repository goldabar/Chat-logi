/**
 *
 *  @author Gołda Bartosz S16728
 *
 */

package zad1;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.nio.channels.Selector;
import java.nio.charset.Charset;

public class ChatServer {

	Map<SocketChannel, Conn> polaczenia = new HashMap<>();
	private ServerSocketChannel ssChannel;
	Selector selektor;
	InetSocketAddress isAdres;
	volatile boolean onRun;
	StringBuilder logs = new StringBuilder();
	private static Charset utf8 = StandardCharsets.UTF_8;

	public ChatServer(String host, int port) {
		isAdres = new InetSocketAddress(host, port);
	}

	public void startServer() throws IOException {

		ssChannel = ServerSocketChannel.open();
		ssChannel.socket().bind(isAdres);
		ssChannel.configureBlocking(false);
		selektor = Selector.open();
		ssChannel.register(selektor, SelectionKey.OP_ACCEPT);

		new Thread(() -> {
			onRun = true;
			System.out.println("Server started\n");

			while (onRun) {
				try {
					selektor.select();
					Set<SelectionKey> selKey = selektor.selectedKeys();
					Iterator<SelectionKey> iteration = selKey.iterator();

					while (iteration.hasNext()) {
						SelectionKey sk = iteration.next();
						iteration.remove();

						if (sk.isAcceptable()) {
							SocketChannel scClient = ssChannel.accept();
							scClient.configureBlocking(false);
							scClient.register(selektor, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
							continue;
						}
						
						if (sk.isReadable()) {
							SocketChannel client = (SocketChannel) sk.channel();
							reqService(client);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	private void reqService(SocketChannel scClient) throws IOException {

		StringBuilder res = new StringBuilder();
		String req = getResp(scClient);

		logs.append(getTime()).append(" ");
		if (req.contains("/login ")) {
			String[] reqSplit = req.split(" ");
			polaczenia.putIfAbsent(scClient, new Conn(reqSplit[1]));
			res.append(reqSplit[1]).append(" logged in");
			logs.append(reqSplit[1]).append(" logged in");
		} else if (req.contains("/logout ")) {
			String[] reqSplit = req.split(" ");
			res.append(reqSplit[1]).append(" logged out");
			logs.append(polaczenia.get(scClient).id).append(": ");
			logs.append(reqSplit[1]).append(" logged out");
			odpowiedz(scClient, res.toString() + "\n");
			polaczenia.remove(scClient);
		} else {
			res.append(polaczenia.get(scClient).id).append(": ").append(req);
			logs.append(polaczenia.get(scClient).id).append(": ");
			logs.append(req);
		}
		
		logs.append("\n");
		res.append("\n");
		broadcastService(res.toString());

	}

	private static DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("HH:mm:ss:SSS");
	private String getTime() {
		return dateTimeFormat.format(LocalDateTime.now());
	}

	public void broadcastService(String msg) {
		polaczenia.forEach((sc, conn) -> {
			try {
				odpowiedz(sc, msg);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	public void odpowiedz(SocketChannel sc, String res) throws IOException {
		ByteBuffer bb = ByteBuffer.allocateDirect(res.getBytes().length);
		bb.put(utf8.encode(res));
		bb.flip();
		sc.write(bb);
	}

	public String getResp(SocketChannel sc) throws IOException {
		ByteBuffer bb = ByteBuffer.allocateDirect(2048);
		StringBuilder req = new StringBuilder();

		for (int x = sc.read(bb); x > 0; x = sc.read(bb)) {
			bb.flip();
			req.append(utf8.decode(bb));
		}
		return req.toString();
	}

	public void stopServer() {
		onRun = false;
		System.out.println("Server stopped");
	}

	public String getServerLog() {
		return logs.toString();
	}

	private class Conn {
		String id;
		public Conn(String id) {
			this.id = id;
		}
	}
}
