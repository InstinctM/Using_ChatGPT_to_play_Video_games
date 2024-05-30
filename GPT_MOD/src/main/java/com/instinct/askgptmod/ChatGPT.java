package com.instinct.askgptmod;

import java.io.IOException;
import java.util.HashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
* <h1>ChatGPT</h1>
* The class contains the methods to query ChatGPT,
* and the methods to capture necessary information from the player.
*
* @author  Hei Wing Lee
* @version 1.0
* @since   2024-02-07
*/

public class ChatGPT {
	public String askGPT(String message) {
		/**
		* Sends the player's information to a GPT model for a response.
		*
		* @exception IOException on unsuccessful capturing of the response, or the socket is closed, or the socket is not connected
		* @see IOException
		*/

		// Obtain player's question and inventory
		String question = getQuestion(message);
		String inventory = getInventory();

		Client client = new Client();
		try {
			// Start the server socket first
			client.runPythonServer();
			Thread.sleep(2000);
			// Send the information to the server
			client.init();
			client.sendMessage(question, inventory);
			// Get the response from GPT and disconnect
			String response = client.receiveMessage();
			System.out.println("Received message: " + response);
			client.close();
			return response;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Default response
		return "Cannot connect to ChatGPT!";
	}

	@SuppressWarnings("resource")
	public void sendResponseToPlayer(String response) {
		/**
		* Sends response from a GPT model to the player.
		*/

		Player player = Minecraft.getInstance().player;

		// Check Player from Minecraft.getInstance(); could be null
		if (player.getUUID() != null) {
			player.sendMessage(new TextComponent(response), player.getUUID());
		}
	}

	// Get the player's inventory
	private static String getInventory() {
		/**
		* Store a player's inventory in a hashmap, with the item's name and quantity.
		*
        	* @return A string representation of the inventory of a player.
		*/
		HashMap<String, Integer> inventory = new HashMap<String, Integer>();
		String inventoryList = "Player's inventory: ";

		Player player = Minecraft.getInstance().player;
		Inventory currentInventory = player.getInventory();

		if (currentInventory.isEmpty()) {
			return "Empty Inventory";
		} else {
			for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
				ItemStack item = currentInventory.getItem(i);
				// item id is in the format itemType.minecraft.itemName
				String[] itemTypeAndName = item.getDescriptionId().split("\\.");
				// Only put the item name and quantity in hashmap
				String itemName = itemTypeAndName[itemTypeAndName.length - 1];
				// If hashmap does not contain the item and the item is not an air block
				if (!itemName.equals("air")) {
					if (!inventory.containsKey(itemName)) {
						inventory.put(itemName, item.getCount());
					} else {
						inventory.put(itemName, item.getCount() + inventory.get(itemName));
					}
				}
			}

			for (String i : inventory.keySet()) {
				inventoryList = inventoryList + i + ":" + inventory.get(i) + ",";
			}
		}
		return inventoryList;
	}

	private static String getQuestion(String question) {
		/**
		* Separate the trigger word from the message.
		*
        	* @return A string representation of question raised by the player.
		*/
        return question.replace("askGPT ", "");
	}

	@SuppressWarnings("unused")
	private static String stubResponse() {
		/**
		* Stub response for development purposes.
		*
        	* @return A predefined string.
		*/
		return "This is a stub response";
	}
}
