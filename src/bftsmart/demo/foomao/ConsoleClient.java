package bftsmart.demo.foomao;

import java.io.Console;
import java.util.Scanner;


public class ConsoleClient {
	public static void main(String[] args) {
        if(args.length < 1) {
            System.out.println("Usage: ConsoleClient <client id>");
        }

        MapClient client = new MapClient(Integer.parseInt(args[0]));
        Console console = System.console();

        Scanner sc = new Scanner(System.in);

        while(true) {
            System.out.println("Select an option:");
            System.out.println("1. ADD A KEY AND VALUE TO THE MAP");
            System.out.println("2. READ A VALUE FROM THE MAP");
            System.out.println("3. REMOVE AND ENTRY FROM THE MAP");
            System.out.println("4. GET THE SIZE OF THE MAP");

            int cmd = sc.nextInt();

            switch(cmd) {
            case 1:
                System.out.println("Putting value in the map");
                String key = console.readLine("Enter the key:");
                String value = console.readLine("Enter the value:");
                String result =  client.put(key, value);
                System.out.println("Previous value: " + result);
                break;
            case 2:
                System.out.println("Reading value from the map");
                key = console.readLine("Enter the key:");
                result =  client.get(key);
                System.out.println("Value read: " + result);
                break;
            case 3:
                System.out.println("Removing value in the map");
                key = console.readLine("Enter the key:");
                result =  client.remove(key);
                System.out.println("Value removed: " + result);
                break;
            case 4:
                System.out.println("Getting the map size");
                int size = client.size();
                System.out.println("Map size: " + size);
                break;
            }
        }
    }
}
