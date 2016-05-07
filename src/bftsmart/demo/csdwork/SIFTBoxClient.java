package bftsmart.demo.csdwork;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import bftsmart.demo.csdwork.RequestType;

//This is the class which sends requests to replicas
import bftsmart.tom.ServiceProxy;

public class SIFTBoxClient {

	static ServiceProxy clientProxy = null;
	private static int userID;
	private static String[] myDirectories;
	private static String allPath;
	private static String myPath = "";

	public SIFTBoxClient(int clientId) {
		clientProxy = new ServiceProxy(clientId);
	}

	/**
	 * Spliting the dirs
	 */
	private static void setMyDirectories() {
		myDirectories = allPath.split(",");
	}

	/**
	 * Main
	 * HOW TO USE Use: java csd.aula2.rmi.SIFTClient -u <userdId> -a
	 * <Path of the local dirs> -s <list of dirs>
	 * @param args
	 */
	public static void main(String[] args) {
		userID = Integer.parseInt(args[1]);
		myPath = args[3];
		myPath = "/home/osboxes/SIFTBoxMyClientBox";
		allPath = args[5];

		new SIFTBoxClient(userID);

		System.out.println("Setting directories");
		setMyDirectories();
		System.out.println("DONE Setting directories");
		System.out.println("Checking directories");
		checkDirectories();
		System.out.println("DONE Checking directories");

		Timer timer = new Timer();

		TimerTask myTask = new TimerTask() {
			public void run() {
				// DO THE CHECKINGS OF FILES AND SYNCHRONIZE
				try {
					System.out.println("Checking files");
					checkFiles();
					System.out.println("DONE Checking directories");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		timer.schedule(myTask, 2000, 50000);

	}

	/**
	 * Check directories on ServerSide and on ClientSide
	 * If the directories passed as arguments in the main doesnt exist creates them
	 */
	private static void checkDirectories() {
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
			clientProxy.invokeOrdered(out.toByteArray());
			System.out.println("Directory Created or Checked on Server: "+ dir);
			// MAYBE DEPOIS METER AQUI UMA ANSWER

		} catch (IOException e) {

		}
	}

	/**
	 * Checks all files and its modified dates ... This method takes care of the synchronization
	 * 1- Gets all files from the server side
	 * 2- Gets all files from the client side
	 * 3- Compares the Modified date
	 * 4- if local modified date > server modified date, upload the file to the server
	 * 5- else download the file to the client
	 * 
	 * This method uses the fetchFilesFromServer
	 * @throws Exception
	 */
	private static void checkFiles() throws Exception {

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

	/**
	 * Method called by checkFiles, it receives two file arrays, one from ClientSide and other from ServerSide that belong to a directory
	 * Compares the modified time of each file and uploads or downloads the file
	 * If the file doesnt exist on the server an upload is made
	 * and if the file doesnt exsit on the client side a download is made
	 * (The download is not actually made since we already have the file) 
	 * @param tempServerSide
	 * @param tempClientSide
	 * @param a - Directory name which we are comparing
	 * @throws IOException
	 */
	private static void compareFilesWithDatesAndUploadOrDownload(File[] tempServerSide, File[] tempClientSide, String a)
			throws IOException {
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
						} else if (clientFile.lastModified() < serverFile.lastModified()) {
							createFileClient(a, serverFile);
						}
						//PAREI AQUI
					}
					if (!found) {
						System.out.println("NOT FOUND CREATING ON CLIENT: "+a+" "+serverFile.getName());
						createFileClient(a, serverFile);
					}
				}
			}
		} else {
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

	/**
	 * Method called when there is a need to upload a file to the server
	 * It prepares a message consisting of the
	 * userId (client that is making the request),
	 * a (directory where the file belongs),
	 * clientFile.getName() (name of the file),
	 * clientfile.length() (for that we know how many bytes to read and to put on the message)
	 * clientFile.lastModified()
	 * @param a - directory name
	 * @param clientFile
	 * @param userId
	 */
	private static void createFileServer(String a, File clientFile, int userId) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(out);

		try {
			dos.writeInt(RequestType.PUT);
			dos.writeInt(userId);
			dos.writeUTF(a);
			dos.writeUTF(clientFile.getName());
			dos.writeLong(clientFile.length());
			dos.writeLong(clientFile.lastModified());

			byte[] file = new byte[(int) clientFile.length()];
			FileInputStream fileinput = new FileInputStream(clientFile);
			fileinput.read(file);
			fileinput.close();

			dos.write(file);
			System.out.println("Invoking create file "+clientFile.getName()+" on server.");
			clientProxy.invokeOrdered(out.toByteArray());
			//
		} catch (IOException ioe) {
		}
	}

	/**
	 * Method to create a file on the client. 
	 * If the file already exists it deletes it and creates a new one.
	 * @param a - directory of the file
	 * @param serverFile
	 * @throws IOException
	 */
	private static void createFileClient(String a, File serverFile) throws IOException {
		byte[] data = new byte[Math.round(serverFile.length())];
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(serverFile));
		bis.read(data, 0, data.length);
		bis.close();

		File file = new File(myPath + "/" + a + "/" + serverFile.getName());
		if (file.exists()) {
			file.delete();
		}
		System.out.println(file.createNewFile());
		System.out.println("Creating file: "+serverFile.getName()+ " on client.");
		FileOutputStream output = new FileOutputStream(file);
		output.write(data);
		output.close();
		file.setLastModified(serverFile.lastModified());
	}

	/**
	 * Method to get all the files of a directory from the Server
	 * This method calls the invokeUnordered since we are doing a get
	 * @param a - Directory from where we are getting the files
	 * @return an array of bytes with information about all the files
	 * 
	 * The returned bytes of this method is as follows and should be read as follows:
	 * readInt - the first will return the amount of files in the byte array
	 * After this we should use a for to collect the content of the files:
	 * readUTF - path of the file
	 * readLong - length of the file
	 * After this we should use a for to collect each byte of the file using its length
	 * readByte - we get a byte of the file
	 */
	private static byte[] fetchFilesFromServer(String a) {
		System.out.println("Fetching Files From Server");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(out);

		try {
			dos.writeInt(RequestType.GET_ALL);
			dos.writeUTF(a);
			dos.writeInt(userID);

			byte[] reply = clientProxy.invokeOrdered(out.toByteArray());
			System.out.println(reply);
			System.out.println("DONE Fetching Files From Server");	
			return reply;
		} catch (IOException e) {
			System.out.println("ERROR Fetching Files From Server");
			return null;
		}

	}
	
	
}
