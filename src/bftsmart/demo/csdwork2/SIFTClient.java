package bftsmart.demo.csdwork2;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Timer;
import java.util.TimerTask;

public class SIFTClient {

	private static String userID;
	private static IServer server;
	private static String[] myDirectories;
	private static String allPath;
	private static String myPath = "/home/osboxes/SIFTBoxMyClientBox";

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
		if ((args.length < 6) || (!args[0].equalsIgnoreCase("-u")) || (!args[2].equalsIgnoreCase("-a"))
				|| (!args[4].equalsIgnoreCase("-s"))) {
			System.out.println(args.length);
			System.out.println(args[0]);
			System.out.println(args[2]);
			System.out.println(args[4]);
			System.out.println("Use: java SIFTClient -u <userId> -a <serverAddress:port> -s <list of dirs>");
			System.exit(0);
		}

		userID = args[1];
		String serverHost = args[3];
		allPath = args[5];

		System.out.println(userID);
		System.out.println(serverHost);
		System.out.println(allPath);

		try {
			System.setProperty("javax.net.ssl.trustStore", "client.ks");
			System.setProperty("javax.net.ssl.trustStorePassword", "123456");

			server = (IServer) Naming.lookup("//" + serverHost + "/siftBoxServer");
			Timer timer = new Timer();

			// split the directories path

			setMyDirectories();
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
			server.putDirectory(a, userID);
			File f = new File(myPath + "/" + a);
			if (!f.exists() || !f.isDirectory()) {
				f.mkdir();
			}
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
		File[] tempServerSide;
		File[] tempClientSide;
		for (String a : myDirectories) {
			try {
				tempServerSide = server.getAll(a, userID);
				System.out.println("Server: " + tempServerSide.length);
				File f = new File(myPath + "/" + a);
				tempClientSide = f.listFiles();

				System.out.println("CLient: " + tempClientSide.length);
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
								if (clientFile.lastModified() > serverFile.lastModified()) {
									server.put(a, clientFile, userID);
								} else if (clientFile.lastModified() < serverFile.lastModified()) {
									createFileClient(a, serverFile);
								}
							}
						}
						if (!found) {
							System.out.println("NOT FOUND ON CLIENT");
							createFileClient(a, serverFile);
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
								if (clientFile.lastModified() > serverFile.lastModified()) {
									server.put(a, clientFile, userID);
								} else if (clientFile.lastModified() < serverFile.lastModified()) {
									createFileClient(a, serverFile);
								}
							}
						}
						if (!found) {
							System.out.println("NOT FOUND ON SERVER");
							server.put(a, clientFile, userID);
						}
					}
				}
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
