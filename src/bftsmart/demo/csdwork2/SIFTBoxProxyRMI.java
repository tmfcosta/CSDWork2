package bftsmart.demo.csdwork2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;


public class SIFTBoxProxyRMI extends UnicastRemoteObject implements IServer{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// public Map<Integer, File> map= new HashMap<Integer,File>();
	private static String rootPath;
	private static String myAddress;
	
	private static Map<String, String> passwords;
	private static Map<String, BigInteger> challenges;
	private static SecureRandom random;	

	protected SIFTBoxProxyRMI() throws RemoteException {
		super(9000, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());

	}

	
	public static void main(String[] args) {
		try {
			System.setProperty("java.security.policy", "src/csd/aula2/rmi/policy.all");
			System.setProperty("javax.net.ssl.keyStore", "server.ks");
			System.setProperty("javax.net.ssl.keyStorePassword", "123456");

			//some data to the passwords (adding some clients)
			passwords.put("Tiago", "ola");
			passwords.put("Luis","adeus");
			
			try { // start rmiregistry
				LocateRegistry.createRegistry(1099);
			} catch (RemoteException e) {
				// if not start it
				// do nothing - already started with rmiregistry
			}

			Naming.rebind("/siftBoxServer", new SIFTBoxProxyRMI());
			readConfigFile();
			System.out.println("SIFTBox server secure RMI bound in registry");
		} catch (Throwable th) {
			th.printStackTrace();
		}
	}
	
	/**
	 * Read the configuration file rootPath- path of the files myAddress -
	 * server address (not used)
	 */
	private static void readConfigFile() throws IOException {
		@SuppressWarnings("resource")
		BufferedReader br = new BufferedReader(new FileReader("config.txt"));

		rootPath = br.readLine();
		myAddress = br.readLine();
	}

	@Override
	public void put(String directory, File value, String clientId) throws RemoteException, Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public File get(String directory, String name, String clientId) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File[] getAll(String directory, String clientId) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void putDirectory(String directory, String clientId) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(String directory, String name, String clientId) throws RemoteException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public boolean authenticate(String username, BigInteger digestClient) throws RemoteException {
		
		if(!passwords.containsKey(username)){
			throw new RemoteException("Unknow user trying to authenticate.");
		}
		if(challenges.get(username) == null){
			throw new RemoteException("Unable to authenticate "+username+". Missing authentication challenge.");
		}
		
		try{
			MessageDigest mDigest = java.security.MessageDigest.getInstance("SHA-256");
			
			mDigest.reset();
			mDigest.update(challenges.get(username).toByteArray());
			mDigest.update(passwords.get(username).getBytes("UTF-16"));
			
			BigInteger digestServer = new BigInteger(1, mDigest.digest());
			
			if(digestClient.equals(digestServer)){
				System.out.println("Authentication suceeded for user: "+username );
			}
			return true;
		}catch (NoSuchAlgorithmException e){
			e.printStackTrace();
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
		}
		System.out.println("An attempt to authenticate failed for user:" + username);
		return false;
	}


	@Override
	public BigInteger challenge(String username) throws RemoteException {
		byte[] bytes = new byte[100];
		
		if(!passwords.containsKey(username))
			throw new RemoteException("Unknown user trying to authenticate");
		
		random.nextBytes(bytes);
		BigInteger serverChallenge = new BigInteger(1,bytes);
		challenges.put(username, serverChallenge);
		return serverChallenge;
	}

}
