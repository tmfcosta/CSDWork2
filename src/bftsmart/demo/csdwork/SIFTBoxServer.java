package bftsmart.demo.csdwork;

//import bftsmart 
import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;

//usefull java imports
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.TreeMap;

import bftsmart.demo.csdwork.RequestType;

public class SIFTBoxServer extends DefaultRecoverable {

	Map<String, File> fileSystem;
	private static String rootPath;

	public SIFTBoxServer(int serverId, String path) {
		fileSystem = new TreeMap<>();
		rootPath = path;
		System.out.println("Server " + serverId + " started!");
		new ServiceReplica(serverId, this, this);
	}

	/**
	 * HOW TO RUN: java bftsmart.demo.csdwork.SIFTBox serverId- id of the server
	 * path to the server dirs - used for default:
	 * /home/osboxes/SISFTBOXServerBFT/server(id)
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Usage: SIFTBoxServer <server id> <myPath>");
			System.exit(0);
		}

		new SIFTBoxServer(Integer.parseInt(args[0]), args[1]);
	}

	/**
	 * Install the snapshot Given the fact that this is a fileSystem we are
	 * using a map to keep the files When we need to install the snapshot we use
	 * the tree we receive (as a byte array) from the getSnapshot so that we can
	 * compare with the local files and synchronize it
	 * 
	 * @param state
	 *            - State that we receive from getSnapshot.. converted to Map
	 *            <String,File>
	 */
	@Override
	public void installSnapshot(byte[] state) {
		System.out.println("Installing Snapshot");
		ByteArrayInputStream bis = new ByteArrayInputStream(state);
		try {
			ObjectInput in = new ObjectInputStream(bis);
			fileSystem = (Map<String, File>) in.readObject();
			in.close();
			bis.close();
		} catch (ClassNotFoundException e) {
			System.out.print("Coudn't find Map: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.out.print("Exception installing the application state: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Method where we try to make a snapshot of the application Ín this case we
	 * will return our Tree<String, File>
	 */
	@Override
	public byte[] getSnapshot() {
		System.out.println("Getting snapshot");
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(bos);
			out.writeObject(fileSystem);
			out.flush();
			out.close();
			bos.close();
			return bos.toByteArray();
		} catch (IOException e) {
			System.out.println(
					"Exception when trying to take a + " + "snapshot of the application state" + e.getMessage());
			e.printStackTrace();
			return new byte[0];
		}
	}

	/**
	 * Method to exexute ordered commands, in this case we will execute on
	 * executeSingle
	 */
	@Override
	public byte[][] appExecuteBatch(byte[][] commands, MessageContext[] msgCtxs) {
		System.out.println("execute batch");
		byte[][] replies = new byte[commands.length][];
		for (int i = 0; i < commands.length; i++) {
			replies[i] = executeSingle(commands[i], msgCtxs[i]);
		}

		return replies;
	}

	/**
	 * Method to execute ordered commands, PUT_DIRECTORY to create directories
	 * and PUT to create or update files
	 */
	private byte[] executeSingle(byte[] command, MessageContext msgCtx) {
		System.out.println("Executing single");
		ByteArrayInputStream in = new ByteArrayInputStream(command);
		DataInputStream dis = new DataInputStream(in);
		int reqType;

		try {
			reqType = dis.readInt();
			if (reqType == RequestType.PUT_DIRECTORY) {
				String dir = dis.readUTF();
				String clientId = String.valueOf(dis.readInt());
				System.out.println("PUT_DIRECTORY REQUEST");
				putDirectory(dir, clientId);
			} else if (reqType == RequestType.PUT) {
				int clientid = dis.readInt();
				String dir = dis.readUTF();
				String filename = dis.readUTF();
				long size = dis.readLong();
				long lastModified = dis.readLong();

				System.out.println("PUT REQUEST");
				File f = new File(rootPath + "/" + dir + "/" + filename);
				if (f.exists() && hasRights(String.valueOf(clientid), dir)) {
					f.delete();
				}

				byte[] data = new byte[(int) size];
				for (int i = 0; i < (int) size; i++) {
					data[i] = dis.readByte();
				}

				if (hasRights(String.valueOf(clientid), dir)) {
					System.out.println(f.createNewFile());
					System.out.println("File " + f.getName() + " create on: " + dir);
					FileOutputStream output = new FileOutputStream(f);
					output.write(data);
					output.close();
					f.setLastModified(lastModified);
				}
			}
			if (reqType == RequestType.GET_ALL) {
				System.out.println("GETTING ALL FILES");
				String dir = dis.readUTF();
				int userId = dis.readInt();

				
				System.out.println("Getting dir: " + dir);
				File f = new File(rootPath + "/" + dir);
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(out);
				
				if (f.isDirectory()) {
					File[] temp = f.listFiles(new FileFilter() {
						public boolean accept(File pathname) {
							return !pathname.isHidden();
						}
					});
					System.out.println("Number of files: " + temp.length);
								
					dos.writeInt(temp.length);
					if (temp.length > 0) {
						for (int i = 0; i < temp.length; i++) {
							System.out.println("1");
							dos.writeUTF(temp[i].getName());
							System.out.println("2");
							dos.writeLong(temp[i].length());
							System.out.println("3");
							byte[] bytedata = new byte[(int) temp[i].length()];
							System.out.println("4");
							FileInputStream fp = new FileInputStream(temp[i]);
							System.out.println("5");
							fp.read(bytedata);
							System.out.println("6");
							fp.close();
							System.out.println("7");
							for (int j = 0; j < bytedata.length; j++) {
								dos.write(bytedata[j]);
							}
							System.out.println("8");
						}
					}
					System.out.println(out.toByteArray());
				}
				return out.toByteArray();
			}
		} catch (IOException e) {

		}
		return null;
	}

	/**
	 * Method to put the directory in the server This method is used at the
	 * start, and puts a directory if it doenst exists
	 * 
	 * @param dir
	 * @param clientId
	 */
	private void putDirectory(String dir, String clientId) {
		File f = new File(rootPath, dir);
		if (!f.exists()) {
			f.mkdir();
			System.out.println("DIRECTORY create: " + dir);
			File shareFile = new File(f, ".shares");
			try {
				shareFile.createNewFile();
				FileWriter fw = new FileWriter(shareFile);
				fw.write(clientId);
				fw.flush();
				fw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Method that executed unordered commands, in this case, only the GET_ALL
	 * that returns all the files in a given directory
	 */
	@Override
	public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
		System.out.println("execute unordered");
		ByteArrayInputStream in = new ByteArrayInputStream(command);
		DataInputStream dis = new DataInputStream(in);
		int reqType;

		try {
			reqType = dis.readInt();

			if (reqType == RequestType.GET_ALL) {
				System.out.println("GETTING ALL FILES");
				String dir = dis.readUTF();
				int userId = dis.readInt();

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(out);
				System.out.println("Getting dir: " + dir);
				File f = new File(rootPath + "/" + dir);
				if (f.isDirectory()) {
					File[] temp = f.listFiles(new FileFilter() {
						public boolean accept(File pathname) {
							return !pathname.isHidden();
						}
					});
					System.out.println("Number of files: " + temp.length);
					dos.writeInt(temp.length);
					if (temp.length > 0) {
						for (int i = 0; i < temp.length; i++) {
							System.out.println("1");
							dos.writeUTF(temp[i].getName());
							System.out.println("2");
							dos.writeLong(temp[i].length());
							System.out.println("3");
							byte[] bytedata = new byte[(int) temp[i].length()];
							System.out.println("4");
							FileInputStream fp = new FileInputStream(temp[i]);
							System.out.println("5");
							fp.read(bytedata);
							System.out.println("6");
							fp.close();
							System.out.println("7");
							for (int j = 0; j < bytedata.length; j++) {
								dos.write(bytedata[j]);
							}
							System.out.println("8");
						}
					}
					System.out.println(out.toByteArray());
				}
				return out.toByteArray();
			}

		} catch (IOException e) {

		}
		return null;
	}

	/**
	 * Checks if a client has rights to a certain directory
	 * 
	 * @param clientId
	 *            - the id of the client that made the request to check the
	 *            rights
	 * @param directory
	 *            - directory to check if the client has right
	 */
	private boolean hasRights(String clientID, String directory) throws RemoteException {
		boolean havePermissions = false;
		File f = new File(rootPath + "/" + directory + "/.shares");
		System.out.println("Checking permission on " + directory + " for client " + clientID);
		if (f.exists() && f.isFile()) {
			try {
				FileReader fileReader = new FileReader(f);
				BufferedReader bufferedReader = new BufferedReader(fileReader);
				String line;
				while ((line = bufferedReader.readLine()) != null) {
					if (line.equalsIgnoreCase(clientID)) {
						havePermissions = true;
					}
				}
				bufferedReader.close();
			} catch (IOException e) {
				System.err.println("no shares files");
			}
		}

		return havePermissions;
	}

}
