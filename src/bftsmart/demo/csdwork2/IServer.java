package bftsmart.demo.csdwork2;

import java.math.BigInteger;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IServer
	extends Remote
{
	/**
	 * Method that will receive a PUT request from the user and will perform it using the
	 * Service Proxy of that user - USES THE BFT-SMART LIBRARY AND COMMUNICATION
	 * @param request - request in bytes
	 * @param username - username of the user
	 * @throws RemoteException
	 */
	public void put(byte[] request, String username) throws RemoteException;

	/**
	 * Methods that will make a GET request from the Service Proxy of the user
	 * - USES THE BFT-SMART LIBRARY AND COMMUNICATION
	 * @param request
	 * @param username
	 * @throws RemoteException
	 */
	public void checkDirectoryOnServer(byte[] request, String username, BigInteger mySecret) throws RemoteException;
	public byte[] fetchFilesFromServer(byte[] request, String username, BigInteger mySecret) throws RemoteException;
	
	/**
	 * Authentication Methods, the first will create the challenge and the second will authenticate
	 * @param username
	 * @param digestclient
	 * @param userId
	 * @return
	 * @throws RemoteException
	 */
	public boolean authenticate(String username, BigInteger digestclient, String userId) throws RemoteException;
	public BigInteger challenge(String username) throws RemoteException;
	
	/**
	 * Method to check if the user is authenticated, doenst call the BFT-SmartLibrary
	 * @param username
	 * @return
	 * @throws RemoteException
	 */
	public boolean isAuthenticated(String username)throws RemoteException;
	
	
	
	//public File get(String directory, String name, String clientId) throws RemoteException;
	//public File[] getAll(String directory, String clientId) throws RemoteException;
	//public boolean checkDirectory(String directory) throws RemoteException;
	//public void putDirectory(String directory, String clientId) throws RemoteException;
	//public void delete(String directory, String name, String clientId) throws RemoteException;
	

}
