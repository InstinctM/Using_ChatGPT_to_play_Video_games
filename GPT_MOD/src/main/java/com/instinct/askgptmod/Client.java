package com.instinct.askgptmod;

import java.io.*;
import java.net.Socket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
* <h1>Client</h1>
* The class creates a client object and its corresponding methods 
* to communicate with a GPT model over a TCP protocol. 
*
* @author  Hei Wing Lee
* @version 1.0
* @since   2024-02-06
*/

public class Client {
    private Socket socket;

    public void init() throws IOException {
        /**
        * Initializes the client by creating a socket object for
        * connecting to a server on the specified address and port.
        *
        * @exception IOException on unsuccessful creation of the socket object
        * @see IOException
        */

        socket = new Socket("localhost", 8080);
        System.out.println("Client Connected");
    }

    public String createMessage(String question, String inventory, String disconnect) {
        /**
         * Constructs and returns a JSON message object,
         * encapsulating pertinent information for the prompt of a GPT model.
        *
        * @param  question The query presented by the player
        * @param  inventory The string representation of the player's current inventory
        * @param  disconnect A boolean encoded in a string format, indicating the client's intention to disconnect.
        * @return A string representation of a JSON object representing the message data.
        */

        JsonObject message = new JsonObject();
        message.addProperty("question", question);
        message.addProperty("inventory", inventory);
        message.addProperty("disconnect", disconnect);
        return new Gson().toJson(message);
    }

    public void sendMessage(String question, String inventory) throws IOException {
        /**
        * Prints a String representation of a JSON message object,
        * and prints it to the console. The output is captured
        * and sent to a server listening on the same address and port.
        *
        * @param  question The question queried by the player
        * @param  inventory The string representation of the player's inventory
        * @exception IOException on unconnected socket or illegal output stream
        * @see IOException
        */

        String playerInfo = createMessage(question, inventory, "False");
        System.out.println("Sending message: " + playerInfo);
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        writer.println(playerInfo);
    }

    public String receiveMessage() throws IOException {
        /**
        * Receives a message from the connected server and 
        * returns a String representation of the response.
        *         
        * @returns The response from a GPT model.        
        * @exception IOException on unsuccessful capturing of the input stream, or the socket is closed, or the socket is not connected
        * @see IOException
        */

        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String nextLine = reader.readLine();
        JsonElement responseJson = JsonParser.parseString(nextLine);
        JsonObject responseJsonObject = responseJson.getAsJsonObject();
        return responseJsonObject.get("response").toString();
    }

    public void close() {
        /**
         * Closes the connection with the server.
         * Sends a disconnect message to ask the server to disconnect.
         * 
         * @exception IOException on unconnected socket or illegal output stream
         * @see IOException
         */

        try {
            // Tell the python server to disconnect
            String disconnectMsg = createMessage("", "", "True");
            System.out.println("Sending message: " + disconnectMsg);
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            writer.println(disconnectMsg);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void runPythonServer() {
        /**
         * Starts the python server automatically in a new process.
         * Allows connection with java socket upon success execution.
         * 
         * @throws IOException on unsuccessful execution of python script.
         * @see IOException
         */

        try {
            // Start the python server in another process
            ProcessBuilder processBuilder = new ProcessBuilder("python", System.getProperty("user.dir") + "\\..\\PythonScripts\\server.py");
            // Write the output of the python server for debugging
            File output = new File(System.getProperty("user.dir") + "\\..\\PythonScripts\\log.txt");
            processBuilder.redirectErrorStream(true);
            processBuilder.redirectOutput(output);
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
