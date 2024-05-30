from dotenv import load_dotenv
import base64
import json
import os
import socket
import asyncio
import time
import requests

SERVER = "localhost"
PORT = 8080

load_dotenv()
API_KEY = os.getenv("OPENAI_API_KEY")

async def run_server():
    """
    Starts a server and creates a TCP socket to send response and receive messages.
    Initialises a request to a GPT model and await for the response.
    Writes response and perspective to a file per query.
    """

    try:
        # Setup server
        server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server.bind((SERVER, PORT))
        server.listen()
        print(f"Listening on {SERVER}:{PORT}")

        # Waiting for client to connect
        client_socket, client_address = server.accept()
        print(f"Accepted connection from {client_address[0]}:{client_address[1]}")

        while True:
            # Receive the question and inventory from Java client
            question, inventory, disconnect = msgDecode(client_socket)

            # If client asks to disconnect
            if disconnect == "True":
                client_socket.close()
                print("Connection closed")
                server.close()
                break

            prompt = createPrompt(question, inventory)
            screenshot_path, perspective = getPerspective(os.path.dirname(os.getcwd()) + "/run/screenshots")
            responseStr, responseJson = await askGPT(prompt, perspective)
            response = responseJson.encode("utf-8")
            # response = stubResponse().encode("utf-8")
            print(responseStr)

            # Write response to a text file
            with open("response.txt", "w+") as f:
                f.write(responseStr)
                print("Response written!")

            # Sends a response back the client
            print("Sending response back...")
            client_socket.sendall(response)
            client_socket.sendall("\n".encode("utf-8"))

            # Saving the perspective in another directory (Base dir)
            # if os.path.exists(os.path.dirname(os.path.dirname(os.getcwd())) + "/Screenshots/perspective.png"):
            #     os.remove(os.path.dirname(os.path.dirname(os.getcwd())) + "/Screenshots/perspective.png")
            # os.rename(screenshot_path, os.path.dirname(os.path.dirname(os.getcwd())) + "/Screenshots/" + os.path.basename(screenshot_path))

    except Exception as e:
        print("Error:", e)

def msgDecode(socket):
    """
    Decodes the message sent from the client in utf-8 format.
    Splits message into three parts and return them.
    """

    message = socket.recv(2048).decode("utf-8")
    playerInfo = json.loads(message)
    question = playerInfo["question"]
    inventory = playerInfo["inventory"]
    disconnect = playerInfo["disconnect"]

    print(f"Received: {question}\n {inventory}")
    return question, inventory, disconnect

def createPrompt(question, inventory):
    """
    Creates a prompt in the order task, question and inventory.
    """

    task = "Task: Given the player's perspective in Minecraft, including their current location, surroundings, as well as their inventory, please provide contextualized instructions to assist the player and answer their question."
    question = "Question: " + question
    inventory = "Inventory: " + inventory
    feedback = "Generate the responses in a way that addresses these feedbacks:The responses are too general. Not specific enough to the current situation. Too much extra information. Too verbose. Could provided a more detailed explanation on how certain actions can be executed by describing the controls to cater beginner level players."
    prompt = task + question + inventory + feedback
    return prompt

# Function to encode the image
def encode_image(path):
    """
    Encode an image in base-64 format.
    Required format during query with ChatGPT 4.
    """
    with open(path, "rb") as image:
        return base64.b64encode(image.read()).decode('utf-8')

def getPerspective(perspectivePath):
    """
    Search for an screenshot image indicating the player's perspective in the screenshot folder.
    If the image does not exist, waits until the image exists before proceeding.

    Note: Press F2 before sending the message in Minecraft. Currently there is no method in forge that supports automatic screenshotting.
    """

    while True:
      screenshot_dir = os.listdir(perspectivePath)
      if len(screenshot_dir) != 0:
        screenshot = os.path.join(perspectivePath, screenshot_dir[0])
        new_screenshot = os.path.join(perspectivePath, "perspective.png")
        os.rename(screenshot, new_screenshot)
        print(f"File '{screenshot}' has been renamed to '{new_screenshot}'.")
        break
      print("Waiting for perspective...")
      time.sleep(1)
    return new_screenshot, encode_image(new_screenshot)

async def askGPT(prompt, playerPerspective):
    """
    Sends a request to ChatGPT 4 Vision with the created prompt and perspective.
    Returns a response generated by the language model.
    """

    headers = {"Content-Type": "application/json", "Authorization": f"Bearer {API_KEY}"}

    payload = {
        "model": "gpt-4-vision-preview",
        "messages": [
            {
                "role": "user",
                "content": [
                    {
                        "type": "text",
                        "text": prompt
                    },
                    {
                        "type": "image_url",
                        "image_url": {
                            "url": f"data:image/jpeg;base64,{playerPerspective}",
                            "detail": "high"
                        }
                    }
                ]
            }
        ],
        "max_tokens": 1000,
    }

    response = requests.post("https://api.openai.com/v1/chat/completions", headers=headers, json=payload)
    response = response.json()
    responseStr = " ".join(response['choices'][0]['message']['content'].split())
    responseJson = {"response": responseStr}
    return responseStr, json.dumps(responseJson)

def stubResponse():
    response = ''' To make a stone axe in Minecraft, you will need to collect some additional materials and use a crafting table. Based on your inventory, you already have stone. However, to craft a stone axe, you also require sticks, which are not listed in your inventory. Here are the steps to craft a stone axe:

    1. Gather wood if you don't have any sticks:
    - Chop down a tree by left-clicking on the trunk of any tree until it breaks and drops wood logs.
    
    2. Open your inventory and convert the wood logs into wooden planks:
    - Place the wood logs into one of the crafting slots to create wooden planks.

    3. Create sticks from the wooden planks:
    - Place one wooden plank above another in the crafting grid to obtain sticks.

    4. Access a crafting table:
    - If you don't have a crafting table, you can make one by placing four wooden planks in the 2x2 crafting grid in your inventory, filling all four squares. Then place the crafting table on the 
    ground and right-click to use it.

    5. Craft the stone axe:
    - Once you have the crafting table open, put 3 stones in the top row, filling the first and second boxes (from the left in a 3x3 grid) and also filling the center box in the second row with one stone. Then, put sticks in the center of the grid and the bottom middle slot representing the handle.

    6. Once you have placed the stone and the sticks in the correct pattern, the stone axe will appear as a craftable item. Drag it into your inventory to complete the process.

    Remember, in Minecraft, the correct pattern on the crafting table is essential to create the tool or item you want. Follow these instructions precisely to ensure you successfully craft a stone axe.
    '''
    response = " ".join(response.split())
    responseJson = {"response": response}
    return json.dumps(responseJson)

# if __name__ == "__main__":
    # asyncio.run(run_server())

asyncio.run(run_server())


