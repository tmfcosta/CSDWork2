package bftsmart.demo.csdwork;

//import bftsmart 
import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
//usefull java imports
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.TreeMap;

import bftsmart.demo.csdwork.RequestType;

public class SIFTBoxServer extends DefaultRecoverable {

	Map<String, File> fileSystem;
	private static String rootPath;

	public SIFTBoxServer(int serverId, String path) {
		fileSystem = new TreeMap<>();
		rootPath = path;
		new ServiceReplica(serverId, this, this);
		System.out.println("Server "+serverId+" started!");
	}

	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Usage: HashMapServer <server id> <myPath>");
			System.exit(0);
		}

		new SIFTBoxServer(Integer.parseInt(args[0]), args[1]);
	}

	/**
	 * Install the snapshot
	 * Given the fact that this is a fileSystem we are using a map to keep the files
	 * When we need to install the snapshot we use the tree we receive (as a byte array)
	 * from the getSnapshot so that we can compare with the local files and synchronize it
	 * @param state - State that we receive from getSnapshot.. converted to Map<String,File>
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
	 * Method where we try to make a snapshot of the application
	 * Ín this case we will return our Tree<String, File>
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
	 * Method to exexute ordered commands, in this case we will
	 * execute on executeSingle
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
		System.out.println("execute single");
		ByteArrayInputStream in = new ByteArrayInputStream(command);
		DataInputStream dis = new DataInputStream(in);
		int reqType;

		try {
			reqType = dis.readInt();
			System.out.println(reqType);
			if (reqType == RequestType.PUT_DIRECTORY) {
				String dir = dis.readUTF();
				int clientId = dis.readInt();
				putDirectory(dir, clientId);
			} else if (reqType == RequestType.PUT) {
				int clientid = dis.readInt();
				String dir = dis.readUTF();
				String filename = dis.readUTF();
				long size = dis.readLong();
				long lastModified = dis.readLong();

				File f = new File(rootPath + "/" + dir + "/" + filename);
				if (f.exists()) {
					f.delete();
				}

				byte[] data = new byte[(int) size];
				for (int i = 0; i < (int) size; i++) {
					data[i] = dis.readByte();
				}

				f.createNewFile();
				FileOutputStream output = new FileOutputStream(f);
				output.write(data);
				output.close();
				f.setLastModified(lastModified);
			}
		} catch (IOException e) {

		}
		return null;
	}

	/**
	 * Method to put the directory in the server
	 * This method is used at the start, and puts a directory if it doenst exists
	 * @param dir
	 * @param clientId
	 */
	private void putDirectory(String dir, int clientId) {
		System.out.println("put");
		File f = new File(rootPath, dir);
		if (!f.exists()) {
			f.mkdir();

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
			
			if(reqType == RequestType.GET_ALL){
				String dir = dis.readUTF();
				int userId = dis.readInt();
				
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(out);
				
				File f = new File(rootPath + "/" + dir);
				if (f.isDirectory()) {
					File[] temp = f.listFiles(new FileFilter() {
						public boolean accept(File pathname) {
							return !pathname.isHidden();
						}
					});
						
					dos.writeInt(temp.length);
					for(int i=0; i<temp.length; i++){
						dos.writeUTF(temp[i].getPath());
						dos.writeLong(temp[i].length());
						
						byte[] bytedata = new byte[(int) temp[i].length()];
						FileInputStream fp = new FileInputStream(temp[i]);
						fp.read(bytedata);
						fp.close();
						
						for(int j=0; j<bytedata.length; j++){
							dos.write(bytedata[j]);
						}
					}
				}	
				return out.toByteArray();
			}
			
			
		} catch (IOException e) {

		}
		return null;
	}

}
