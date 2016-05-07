package bftsmart.demo.csdwork2;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.util.Timer;
import java.util.TimerTask;

import bftsmart.demo.csdwork.RequestType;

public class SIFTClient {

	private static int userID;
	private static IServer server;
	private static String[] myDirectories;
	private static String allPath;
	private static String myPath = "/home/osboxes/SIFTBoxMyClientBox";
	private static String username;

	/**
	 * Spliting the dirs
	 */
	private static void setMyDirectories() {
		myDirectories = allPath.split(",");
	}

	/*
	 * private void uploadFile(File f, String directory) throws RemoteException,
	 * Exception { server.put(directory, f, userID); }
	 * 
	 * private void downloadFile(String directory, File f) throws IOException {
	 * File file = server.get(directory, f.getName(), userID);
	 * 
	 * byte[] data = new byte[(int) file.getTotalSpace()]; BufferedInputStream
	 * bis = new BufferedInputStream(new FileInputStream(file)); bis.read(data,
	 * 0, data.length); bis.close();
	 * 
	 * File fr = new File(file.getName());
	 * System.out.println(fr.createNewFile()); FileOutputStream output = new
	 * FileOutputStream(fr); output.write(data); output.close(); }
	 * 
	 * private void deleteFile(String directory, File f) throws RemoteException
	 * { server.delete(directory, f.getName(), userID); }
	 */

	/**
	 * HOW TO USE Use: java csd.aula2.rmi.SIFTClient -u <userdId> -a
	 * <serverAddress> -s <list of dirs>
	 * 
	 */
	public static void main(String[] args) {
		
		MessageDigest digest;
		
		if ((args.length < 8) || (!args[0].equalsIgnoreCase("-u")) || (!args[2].equalsIgnoreCase("-a"))
				|| (!args[4].equalsIgnoreCase("-s") || (!args[6].equalsIgnoreCase("-user")))) {
			System.out.println(args.length);
			System.out.println(args[0]);
			System.out.println(args[2]);
			System.out.println(args[4]);
			System.out.println(args[6]);
			System.out.println("Use: java SIFTClient -u <userID> -a <serverAddress:port> -s <list of dirs> -user <username>");
			System.exit(0);
		}

		userID = Integer.parseInt(args[1]);
		String serverHost = args[3];
		//Falta aqui meter a escolhe de um path inicial
		myPath = "/home/osboxes/SIFTBoxMyClientBox";
		allPath = args[5];
		username = args[6];
		
		System.out.println(userID);
		System.out.println(serverHost);
		System.out.println(allPath);
		System.out.println(username);
		
		Console console = System.console();
		if(console == null){
			System.out.println("Couldn't get Console instance");
			System.exit(0);
		}
		char passwordArray[] = console.readPassword("Enter your secret password: ");
		String password = new String(passwordArray);
		console.printf("Password entered was: %s%n", password);

		try {
			System.setProperty("javax.net.ssl.trustStore", "client.ks");
			System.setProperty("javax.net.ssl.trustStorePassword", "123456");

			server = (IServer) Naming.lookup("//" + serverHost + "/siftBoxServer");
			
			//Check the authentication
			for(int i=0; i<3; i++){
				digest = java.security.MessageDigest.getInstance("SHA-256");
				digest.reset();
				digest.update(server.challenge(username).toByteArray());
				digest.update(password.getBytes("UTF-16"));
				BigInteger digestClient = new BigInteger(1,digest.digest());
				
				boolean isAuth = false;
				isAuth = server.authenticate(username, digestClient, username);
				
				if(isAuth){
					System.out.println("Logged in sucessfully");
					break;
				}
				else if(!isAuth && i<2){
					System.out.println("Your username or password is wrong... you have "+ (2-i) + " attemps");
					console.printf("Username: %n");
					username = console.readLine();
					
					console.printf("Password: %n");
					char passwordArray2[] = console.readPassword("Enter your secret password: ");
					password = new String(passwordArray2);
				}
				else {
					System.out.println("You failed to authenticate");
					System.exit(0);
				}
			}			
			
			Timer timer = new Timer();

			// split the directories path
			System.out.println("Setting directories");
			setMyDirectories();
			System.out.println("DONE Setting directories");
			try {
				// check locally and on server if the directories exists
				System.out.println("Checking directories");
				checkDirectories();
				System.out.println("DONE Setting directories");
			} catch (RemoteException e1) {
				e1.printStackTrace();
			}

			TimerTask myTask = new TimerTask() {
				public void run() {
					// DO THE CHECKINGS OF FILES AND SYNCHRONIZE
					try {
						System.out.println("Checking files");
						checkFiles();
						System.out.println("DONE Checking files");
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			};
			timer.schedule(myTask, 2000, 50000);
		} catch (Exception e) {
			System.err.println("Erro: " + e.getMessage());
		}
	}

	/**
	 * Check to see if it has all the directories passed as arguments
	 * 
	 * @throws RemoteException
	 */
	private static void checkDirectories() throws RemoteException {
		for (String a : myDirectories) {
			File f = new File(myPath + "/" + a);
			if (!f.exists() || !f.isDirectory()) {
				System.out.println("Making dir :"+a+" on client.");
				f.mkdir();
			}
			checkDirectoryOnServer(a);
		}
	}
	
	/**
	 * Check the directory on the ServerSide, if it doesnt exist it create it
	 * @param dir
	 */
	private static void checkDirectoryOnServer(String dir) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(out);

			dos.writeInt(RequestType.PUT_DIRECTORY);
			dos.writeUTF(dir);
			dos.writeInt(userID);
			byte[] request = out.toByteArray();
			server.checkDirectoryOnServer(request, username);
			System.out.println("Directory Created or Checked on Server: "+ dir);
			// MAYBE DEPOIS METER AQUI UMA ANSWER

		} catch (IOException e) {

		}
	}

	/**
	 * Checks all files and its modified dates ... This method takes care of the
	 * synchronization 1- Gets all files from the server side 2- Gets all files
	 * from the client side 3- Compares the Modified date 4- if local modified
	 * date > server modified date, upload the file to the server 5- else
	 * download the file to the client
	 * 
	 * @throws Exception
	 */
	private static void checkFiles() throws Exception {
		//File[] tempServerSide;
		File[] tempClientSide;
		for (String a : myDirectories) {
			System.out.println("CHECKING FILES ON: "+a);
			File f = new File(myPath + "/" + a);
			tempClientSide = f.listFiles();
			
			// Go Fetch the files from the server
			byte[] reply = fetchFilesFromServer(a);
			if (reply != null) {
				ByteArrayInputStream in = new ByteArrayInputStream(reply);
				DataInputStream dis = new DataInputStream(in);
				System.out.println("Starting to read reply");
				
				int size = dis.readInt();
				int count = 0;
				File[] tempServerSide = new File[size];
				System.out.println("Number of files in: "+a+"equals on server: "+size+" and on client: "+tempClientSide.length);
				System.out.println("Number of files in: "+a+ " equals: "+size);
				
				for (int i = 0; i < size; i++) {
					String name = dis.readUTF();
					long fileSize = dis.readLong();
					System.out.println("File: "+name);
					System.out.println("File size: "+fileSize);
					byte[] data = new byte[(int) fileSize];
					int countData = 0;

					for (int j = 0; j < (int) fileSize; j++) {
						data[countData] = dis.readByte();
						countData++;
					}
					File file = new File(name);
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					fileOutputStream.write(data);
					fileOutputStream.close();

					tempServerSide[count] = file;
					count++;
				}
				compareFilesWithDatesAndUploadOrDownload(tempServerSide, tempClientSide, a);
				
			}
		}
	}

	private static void compareFilesWithDatesAndUploadOrDownload(File[] tempServerSide, File[] tempClientSide,
			String a) throws IOException {
		File serverFile;
		File clientFile;
		
		if (tempServerSide.length > tempClientSide.length) {
			for (int i = 0; i < tempServerSide.length; i++) {
				boolean found = false;
				serverFile = tempServerSide[i];
				for (int j = 0; j < tempClientSide.length && !found; j++) {
					clientFile = tempClientSide[j];
					if (clientFile.getName().equalsIgnoreCase(serverFile.getName())) {
						found = true;
						System.out.println("File: "+clientFile.getName()+" SERVER: "+ serverFile.lastModified()+"CLIENT: "+clientFile.lastModified());
						if (clientFile.lastModified() > serverFile.lastModified()) {
							// server.put(a, clientFile, userID);
							createFileServer(a, clientFile, userID);
						}else if (clientFile.lastModified() < serverFile.lastModified()) {
							createFileClient(a, serverFile);
						}
					}
					if (!found) {
						System.out.println("NOT FOUND CREATING ON CLIENT: "+a+" "+serverFile.getName());
						createFileClient(a, serverFile);
					}
				}
			}
		}else {
			for (int i = 0; i < tempClientSide.length; i++) {
				boolean found = false;
				clientFile = tempClientSide[i];
				for (int j = 0; j < tempServerSide.length && !found; j++) {
					serverFile = tempServerSide[j];
					if (clientFile.getName().equalsIgnoreCase(serverFile.getName())) {
						found = true;
						System.out.println("File: "+clientFile.getName()+" SERVER: "+ serverFile.lastModified()+"CLIENT: "+clientFile.lastModified());
						if (clientFile.lastModified() > serverFile.lastModified()) {
							createFileServer(a, clientFile, userID);
						} else if (clientFile.lastModified() < serverFile.lastModified()) {
							createFileClient(a, serverFile);
						}
					}
				}
				if (!found) {
					System.out.println("NOT FOUND CREATING ON CLIENT: "+a+" "+clientFile.getName());
					createFileServer(a, clientFile, userID);
				}
			}
		}
		
	}

	private static void createFileServer(String a, File clientFile, int userID) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(out);
		
		try {
			dos.writeInt(RequestType.PUT);
			dos.writeInt(userID);
			dos.writeUTF(a);
			dos.writeUTF(clientFile.getName());
			dos.writeLong(clientFile.length());
			dos.writeLong(clientFile.lastModified());
			
			byte[] file = new byte[(int) clientFile.length()];
			FileInputStream fileinput = new FileInputStream(clientFile);
			fileinput.read(file);
			fileinput.close();

			dos.write(file);
			byte[] request = out.toByteArray();
			server.put(request, username);
			
		} catch (IOException e) {
			System.out.println("Problems creating file on server.");
		}
	}

	private static byte[] fetchFilesFromServer(String a) {
		System.out.println("Fetching Files From Server");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(out);
		try{
			dos.writeInt(RequestType.GET_ALL);
			dos.writeUTF(a);
			dos.writeInt(userID);
			byte[] request = out.toByteArray();
			byte[] reply = server.fetchFilesFromServer(request, username);
			System.out.println(reply);
			System.out.println("DONE Fetching Files From Server");	
			return reply;
			
		}catch (IOException e) {
			System.out.println("ERROR Fetching Files From Server");
			return null;
		}
	}

	/**
	 * Called by the checkFiles, when there is a file that exists in the server
	 * but not on the client
	 * 
	 * @param directory
	 * @param f
	 * @throws IOException
	 */
	private static void createFileClient(String directory, File f) throws IOException {
		byte[] data = new byte[Math.round(f.length())];
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
		bis.read(data, 0, data.length);
		bis.close();

		File file = new File(myPath + "/" + directory + "/" + f.getName());
		if (file.exists()) {
			file.delete();
		}
		System.out.println(file.createNewFile());
		System.out.println(data.length);
		FileOutputStream output = new FileOutputStream(file);
		output.write(data);
		output.close();
		file.setLastModified(f.lastModified());
	}

}
