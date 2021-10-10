/**
 *
 *  @author Gołda Bartosz S16728
 *
 */

package zad1;


import java.io.IOException;
import java.util.List;
import java.util.concurrent.FutureTask;

public class ChatClientTask extends FutureTask<String> 
{
	ChatClient chClient;

	public ChatClientTask(ChatClient c, List<String> msgs, int wait) 
	{	
		super(()->{
			c.login();		
			if(wait != 0)
				Thread.sleep(wait);
				msgs.forEach( message -> {				
					try {		
						c.send(message);
						if(wait != 0)
							Thread.sleep(wait);
					}
					catch (IOException | InterruptedException e) 
					{				
						e.printStackTrace();
					}
				});		
				c.logout();
				
				return null;
		});	
		chClient = c;
	}

	public static ChatClientTask create(ChatClient c, List<String> msgs, int wait) {
		return new ChatClientTask(c,msgs,wait);
	}

	public ChatClient getClient() {
		return chClient;
	}
}
