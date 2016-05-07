package bftsmart.demo.csdwork2;

import java.math.BigInteger;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IServer
	extends Remote
{
	public void put(byte[] request, String username) throws RemoteException;
	//public File get(String directory, String name, String clientId) throws RemoteException;
	//public File[] getAll(String directory, String clientId) throws RemoteException;
	//public boolean checkDirectory(String directory) throws RemoteException;
	//public void putDirectory(String directory, String clientId) throws RemoteException;
	//public void delete(String directory, String name, String clientId) throws RemoteException;
	
	public void checkDirectoryOnServer(byte[] request, String username) throws RemoteException;
	public byte[] fetchFilesFromServer(byte[] request, String username) throws RemoteException;
	
	public boolean authenticate(String username, BigInteger digestclient, String userId) throws RemoteException;
	public BigInteger challenge(String username) throws RemoteException;
}
