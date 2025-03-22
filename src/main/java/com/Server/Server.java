package com.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class Server
{
	private static Map<String, ClientHandler> clients = new HashMap<>();
	
	public static void main(String[] args)
	{
		System.out.println("Chat Server started...");
		try (ServerSocket serverSocket = new ServerSocket(8000, 0, InetAddress.getByName("0.0.0.0"));)
		{
			while (true)
			{
				Socket clientSocket = serverSocket.accept();
				System.out.println("New client connected");
				Thread.startVirtualThread(new ClientHandler(clientSocket));
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	static class ClientHandler implements Runnable
	{
		private Socket clientSocket;
		private String clientName;
		private BufferedReader input;
		private PrintWriter output;
		
		public ClientHandler(Socket clientSocket)
		{
			this.clientSocket = clientSocket;
			try
			{
				this.input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				this.output = new PrintWriter(clientSocket.getOutputStream(), true);
				
				output.println("Enter your name: ");
				clientName = input.readLine();
				
				if (clientName != null && !clientName.trim().isEmpty())
				{
					synchronized (clients)
					{
						if (clients.containsKey(clientName))
						{
							return;
						}
						clients.put(clientName, this);
					}
				}
				else
				{
					clientSocket.close();
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		@Override
		public void run()
		{
			try
			{
				String line;
				while ((line = input.readLine()) != null)
				{
					processMessage(line);
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			finally
			{
				disconnect();
			}
		}
		
		private void processMessage(String messageJson)
		{
			Gson gson = new Gson();
			try
			{
				ChatMessage chatMessage = gson.fromJson(messageJson, ChatMessage.class);
				if (chatMessage == null)
				{
					return;
				}
				
				String sender = chatMessage.getSender();
				String receiver = chatMessage.getReceiver();
				String messageContent = chatMessage.getMessage();
				
				if (sender == null || receiver == null || messageContent == null)
				{
					return;
				}
				
				synchronized (clients)
				{
					if (clients.containsKey(receiver))
					{
						ClientHandler recipient = clients.get(receiver);
						if (recipient != this)
						{
							ForwardedMessage forwardedMsg = new ForwardedMessage(sender, messageContent);
							String forwardedJson = gson.toJson(forwardedMsg);
							recipient.output.println(forwardedJson);
						}
					}
					else
					{
						output.println("Error: Receiver '" + receiver + "' not found.");
					}
				}
			}
			catch (JsonSyntaxException e)
			{
				e.printStackTrace();
			}
		}
		
		private void disconnect()
		{
			try
			{
				clientSocket.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			synchronized (clients)
			{
				clients.remove(clientName);
			}
			System.out.println("client has disconnected.");
		}
	}
	
	public static class ChatMessage
	{
		private String sender;
		private String receiver;
		private String message;
		
		public String getSender()
		{
			return sender;
		}
		
		public String getReceiver()
		{
			return receiver;
		}
		
		public String getMessage()
		{
			return message;
		}
	}
	
	public static class ForwardedMessage
	{
		private String sender;
		private String message;
		
		public ForwardedMessage(String sender, String message)
		{
			this.sender = sender;
			this.message = message;
		}
		
		public String getSender()
		{
			return sender;
		}
		
		public String getMessage()
		{
			return message;
		}
	}
}