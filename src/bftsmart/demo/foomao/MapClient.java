package bftsmart.demo.foomao;

import bftsmart.demo.foomao.RequestType;
//This is the class which sends requests to replicas
import bftsmart.tom.ServiceProxy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class MapClient implements Map<String, String>{

	ServiceProxy clientProxy = null;
	
	public MapClient(int clientId){
		clientProxy = new ServiceProxy(clientId);
	}
	
	@Override
	public void clear() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean containsKey(Object arg0) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean containsValue(Object arg0) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Set<java.util.Map.Entry<String, String>> entrySet() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String get(Object key) {
		try {
	        ByteArrayOutputStream out = new ByteArrayOutputStream();
	        DataOutputStream dos = new DataOutputStream(out);
	        dos.writeInt(RequestType.GET);
	        dos.writeUTF(String.valueOf(key));
	        byte[] reply = clientProxy.invokeUnordered(out.toByteArray());
	        String value = new String(reply);
	        return value;
	    } catch(IOException ioe) {
	        System.out.println("Exception getting value from the hashmap: " + ioe.getMessage());
	        return null;
	    }
	}

	@Override
	public boolean isEmpty() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Set<String> keySet() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String put(String key, String value) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(out);
		
		try {
	        dos.writeInt(RequestType.PUT);
	        dos.writeUTF(key);
	        dos.writeUTF(value);
	        byte[] reply = clientProxy.invokeOrdered(out.toByteArray());
	        
	        if(reply != null) {
	            String previousValue = new String(reply);
	            return previousValue;
	        }
	        return null;
	    } catch(IOException ioe) {
	        System.out.println("Exception putting value into hashmap: " + ioe.getMessage());
	        return null;
	    }
	}

	@Override
	public void putAll(Map<? extends String, ? extends String> arg0) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String remove(Object key) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
	    DataOutputStream dos = new DataOutputStream(out);
	    try {
	        dos.writeInt(RequestType.REMOVE);
	        dos.writeUTF(String.valueOf(key));
	        byte[] reply = clientProxy.invokeOrdered(out.toByteArray());
	        if(reply != null) {
	            String removedValue = new String(reply);
	            return removedValue;
	        }
	        return null;
	    } catch(IOException ioe) {
	        System.out.println("Exception removing value from the hashmap: " + ioe.getMessage());
	        return null;
	    }
	}

	@Override
	public int size() {
		try {
	        ByteArrayOutputStream out = new ByteArrayOutputStream();
	        DataOutputStream dos = new DataOutputStream(out);
	        dos.writeInt(RequestType.SIZE);
	        byte[] reply = clientProxy.invokeUnordered(out.toByteArray());
	        ByteArrayInputStream in = new ByteArrayInputStream(reply);
	        DataInputStream dis = new DataInputStream(in);
	        int size = dis.readInt();
	        return size;
	    } catch(IOException ioe) {
	        System.out.println("Exception getting the size the hashmap: " + ioe.getMessage());
	        return -1;
	    }
	}

	@Override
	public Collection<String> values()  {
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
