package bftsmart.demo.csdwork2;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.TimerTask;

import java.util.jar.*;
import java.net.*;

import javax.swing.plaf.synth.SynthSplitPaneUI;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;

public class Loader {

	static protected Class<?> clazz;
	static protected SIFTBoxServer replica;
	static protected URLClassLoader loader;
	static protected Timer timer;
	static protected String[] BFTSmartArgs;
	static protected boolean isRunning;
	private static JarFile jarFile = null;

	public static void main(String[] args)
			throws IOException, InstantiationException, IllegalAccessException, InterruptedException {
		// Initialize timer
		timer = new Timer();
		isRunning = false;

		final URL jarUrl = new URL("jar:file:/home/osboxes/git/CSDWork2/bin/BFT-Smart.jar!/");
		final JarURLConnection connection = (JarURLConnection) jarUrl.openConnection();
		jarFile = connection.getJarFile();
		final URL url = connection.getJarFileURL();

		// Store the arguments passed by BFTSmart for future replica resets.
		final String[] BFTSmartArgs = args;
		
		if(!testSignedJar()){
			throw new SecurityException("The provider is not signed");
		}
		
		// Set the loader for the replica
		loader = new URLClassLoader(new URL[] { url }, SIFTBoxServer.class.getClassLoader());

		// Task that resets the replica
		TimerTask myTask = new TimerTask() {
			public void run() {
				// Reset the replica proactively
				try {
					reset();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};

		// Task that starts the replica
		TimerTask myTask2 = new TimerTask() {
			public void run() {
				// Start the replica
				try {
					Thread thread = new Thread() {
						public void run() {
							replica.main(BFTSmartArgs);
						}
					};
					thread.start();
					System.out.println(isRunning);
					isRunning = true;	
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};

		// 5sec Delay + 5sec between successive executions of the same task
		timer.schedule(myTask, 5000, 5000);
		// The server starts after each reboot
		timer.schedule(myTask2, 5000, 5000);
	}

	// Dumb old instance of the replica for a new one
	protected static void reset() throws InstantiationException, IllegalAccessException, InterruptedException, IOException {
		System.out.println("Rebooting...");
		try {
			if(isRunning){
				loader.close();
				System.out.println("im closing");
				System.gc();
				final URL jarUrl = new URL("jar:file://home/osboxes/git/CSDWork2/bin/BFT-Smart.jar!/");
				final JarURLConnection connection = (JarURLConnection) jarUrl.openConnection();
				final URL url = connection.getJarFileURL();
				loader = new URLClassLoader(new URL[] { url }, SIFTBoxServer.class.getClassLoader());
			}
			
			
			clazz = Class.forName("bftsmart.demo.csdwork2.SIFTBoxServer", true, loader);
			replica = (SIFTBoxServer) clazz.newInstance();

		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Rebooted!");
	}
	
	public static boolean testSignedJar() throws IOException{
		boolean isSigned = false;
		
		Manifest man = jarFile.getManifest();
		System.out.println(jarFile.getName());
		if(man == null){
			throw new SecurityException("The provider is not signed");
		}
		Map map = man.getEntries();
		for(Iterator it = map.keySet().iterator(); it.hasNext();){
			String entryName = (String) it.next();
			if(entryName != ""){
				isSigned = true;
			}
		}
		
		return isSigned;
	}

}
