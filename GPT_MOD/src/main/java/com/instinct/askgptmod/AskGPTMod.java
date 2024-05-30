package com.instinct.askgptmod;

import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
* <h1>AskGPTMod</h1>
* The base class of the Mod.
*
* @author  Hei Wing Lee
* @version 1.0
* @since   2024-02-07
*/

// The value here should match an entry in the META-INF/mods.toml file
@Mod("askgptmod")
public class AskGPTMod {
	public static final String MOD_ID = "askgptmod";

	public AskGPTMod() {
        /**
		* Register the mod to the server and some other game events. 
		*/
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	@SubscribeEvent
    public void GPTForSuggestions(ClientChatEvent event) {
        /**
		* The method is called when a chat message is sent.
		* If the chat message starts with "askGPT", it triggers
		* a query to a GPT model and returns the response to the player.
		* 
		* @param event A minecraft message event object
		*/
        String message = event.getMessage();
        
        // Query triggered if first word of message is askGPT
        String[] triggerWordAndQuestion = message.split(" ", 2);
        if (triggerWordAndQuestion[0].equals("askGPT")) {
            ChatGPT GPTClient = new ChatGPT();       
            String response = GPTClient.askGPT(message);
            GPTClient.sendResponseToPlayer(response);
        }
    }
}	

