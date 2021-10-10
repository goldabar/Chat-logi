/**
 *
 *  @author Gołda Bartosz S16728
 *
 */

package zad1;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ChatClient {

	String id;
	InetSocketAddress isAdres;
	private volatile boolean isBlock;
	StringBuilder logChat;
	SocketChannel sc;
	public static Charset utf8 = StandardCharsets.UTF_8;
	private volatile boolean onRun;

	public ChatClient(String host,int port,String id) {
		this.id = id;		
		logChat = new StringBuilder("=== " + id + " chat view\n");	
		this.isAdres = new InetSocketAddress(host,port);	
		try 
		{			
			sc = SocketChannel.open(isAdres);
			sc.configureBlocking(false);
			
		} catch (IOException e) {			
			e.printStackTrace();
		}

		new Thread(()->{		
			onRun = true;
				while (onRun) {
					try {
						String response = getResp();
						if(!response.isEmpty()) {
							isBlock = false;
							logChat.append(response);
						}					
					}catch (IOException e){		
						e.printStackTrace();
					}
				}
				
		}).start();
	}

	public void login(){
		try {			
			send("/login " + id);
		} catch (IOException e) {	
			e.printStackTrace();
		}
	}
	
	public void send(String req) throws IOException{
		ByteBuffer bb = ByteBuffer.allocateDirect(req.getBytes().length);
		bb.put(utf8.encode(req));	
		bb.flip();
		sc.write(bb);	
		isBlock = true;
		while (isBlock);
	}

	public String getChatView() {
		return logChat.toString();
	}

	public String getResp() throws IOException {
		ByteBuffer bb = ByteBuffer.allocateDirect(2137);
		StringBuilder res = new StringBuilder();

		while(sc.read(bb) > 0){
			bb.flip();
			res.append(utf8.decode(bb));	
			bb.clear();
		}	
		return res.toString();
	}

	public void logout() throws IOException {
		send("/logout "+id);
		onRun = false;
		
		while(!logChat.toString().contains(id + " logged out")){
			for (String response = getResp();
			!response.isEmpty();
			response = getResp())																				logChat.append(response);
		}
	}
}
