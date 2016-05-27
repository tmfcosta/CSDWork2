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
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import bftsmart.tom.ServiceProxy;


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
	
	private static Map<String, ServiceProxy> clientsConnections;
	private int numUsers = 1;

	protected SIFTBoxProxyRMI() throws RemoteException {
		super(9000, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
	}

	
	public static void main(String[] args) {
		try {
			System.setProperty("java.security.policy", "src/csd/aula2/rmi/policy.all");
			System.setProperty("javax.net.ssl.keyStore", "server.ks");
			System.setProperty("javax.net.ssl.keyStorePassword", "123456");

			random = new SecureRandom();
			passwords = new HashMap<String, String>();
			challenges = new HashMap<String, BigInteger>();
			clientsConnections = new HashMap<String, ServiceProxy>();
			
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
			
			Timer timer = new Timer();
			
			TimerTask myTask = new TimerTask() {
				public void run() {
					// DO THE CHECKINGS OF FILES AND SYNCHRONIZE
					try {
						resetAuthentications();
						System.out.println("DONE Checking files");
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			};
			timer.schedule(myTask, 2000, 10*60000);
			
			//readConfigFile();
			System.out.println("SIFTBox server secure RMI bound in registry");
		} catch (Throwable th) {
			th.printStackTrace();
		}
	}
	
	private static void resetAuthentications() {
		clientsConnections = new HashMap<String, ServiceProxy>();
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
	public void put(byte[] request, String username) throws RemoteException{
		ServiceProxy cp = clientsConnections.get(username);
		System.out.println("Invoking create file on server.");
		cp.invokeOrdered(request);
	}

	@Override
	public boolean authenticate(String username, BigInteger digestClient, String userId) throws RemoteException {
		
		if(!passwords.containsKey(username)){
			//throw new RemoteException("Unknow user trying to authenticate.");
			return false;
		}
		if(challenges.get(username) == null){
			//throw new RemoteException("Unable to authenticate "+username+". Missing authentication challenge.");
			return false;
		}
		
		try{
			MessageDigest mDigest = java.security.MessageDigest.getInstance("SHA-256");
			
			mDigest.reset();
			mDigest.update(challenges.get(username).toByteArray());
			mDigest.update(passwords.get(username).getBytes("UTF-16"));
			
			BigInteger digestServer = new BigInteger(1, mDigest.digest());
			
			if(digestClient.equals(digestServer)){
				System.out.println("Authentication suceeded for user: "+username );
				newClientConnection(userId);
				return true;
			}
		}catch (NoSuchAlgorithmException e){
			e.printStackTrace();
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
		}
		System.out.println("An attempt to authenticate failed for user:" + username);
		return false;
	}


	private void newClientConnection(String username) {
		clientsConnections.put(username, new ServiceProxy(numUsers));
		numUsers++;
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


	@Override
	public void checkDirectoryOnServer(byte[] request, String username, BigInteger clientSecret) throws RemoteException {
		if(challenges.get(username).equals(clientSecret)){
			ServiceProxy cp = clientsConnections.get(username);
			cp.invokeOrdered(request);	
		}
	}


	@Override
	public byte[] fetchFilesFromServer(byte[] request, String username, BigInteger clientSecret) {
		byte[] tmp = null;
		if(challenges.get(username).equals(clientSecret)){
			ServiceProxy cp = clientsConnections.get(username);
			tmp = cp.invokeOrdered(request);
		}
		return tmp;
	}
	
	public boolean isAuthenticated(String username){
		return clientsConnections.containsKey(username);
	}

}
