import java.security.*;
import java.io.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.net.*;
import javax.json.*;
import java.util.Random;
import java.util.Scanner;
import java.util.Arrays;
import java.util.Base64;
import java.io.ByteArrayOutputStream;
import java.util.Scanner;
import java.math.BigInteger;
import javax.xml.bind.DatatypeConverter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class assignment2 {

	public static void main(String[] args) throws IOException{
        //This code is used to take in the arguments according to their flags.
        String serverURL = "";
        String port = "";
        String username = "";
        String password = "";
        if (args.length >= 6) {
            for (int i = 0; i < args.length / 2; i++) {
                switch(args[2*i]) {
                    case "-s": case "--server":
                        serverURL = args[2*i+1];
                        break;
                    case "-p": case "--port":
                        port = args[2*i+1];
                        break;
                    case "-u": case "--username":
                        username = args[2*i+1];
                        break;
                    case "-w": case "--password":
                        password = args[2*i+1];
                        break;
                    default:
                        System.out.println("Invalid input flag:" + args[2*i]);
                        System.exit(0);
                }
            }
        } else {
            System.out.println("Not enough arguments");
            printUsage();
        }
        if (serverURL == "") {
            System.out.println("Missing required option: s");
            printUsage();
        }    
        if (port == "") {
            System.out.println("Missing required option: p");
            printUsage();
        }    
        if (username == "") {
            System.out.println("Missing required option: u");
            printUsage();
        }
        //Register a public key given the username
        try {
            registerKeys(serverURL, port, username);
        }catch (Exception e) {
            System.out.println(e);
            System.out.println("Failure in registering keys, now exiting");
            System.exit(0);
        }
        //Ask for command input
		Scanner kb = new Scanner(System.in);
        System.out.print("\nenter command>");
        String command; String[] commands;
        while (true){ 
            command = kb.nextLine();
            commands = command.split(" ");
            switch(commands[0]){
                case "get": case "":
                    break;
                case "c": case "compose":
                    break;
                case "f": case "fingerprint":
                    break;
                case "l": case "list":
                    break;
                case "genkeys":
                    break;
                case "h": case "help":
                    break;
                case "q": case "quit":
                    System.out.println("Shutting down...");
                    System.exit(0);
                    break;
                default:                 
            }
            System.out.print("enter command>");
	    }  
    }
    
    
    private static void printUsage(){
        System.out.println("usage: msgclient");
        System.out.println("-p,--port <arg>     server port (default 8000)");
        System.out.println("-s,--server <arg>   server name");
        System.out.println("-u,--username <arg> username");
        System.out.println("-w,--password       password (default is none)");   
        System.exit(0);  
    }
    
    private static KeyPairGenerator[] registerKeys(String serverURL, String port, String username) throws NoSuchAlgorithmException, NoSuchProviderException, IOException, ProtocolException, MalformedURLException {
        Base64.Encoder encoder = Base64.getEncoder();
        //Make the rsa key pair and extract the public key
        KeyPairGenerator rsaKeyGen = KeyPairGenerator.getInstance("RSA");
        rsaKeyGen.initialize(1024);
        byte[] rsaPublic = rsaKeyGen.genKeyPair().getPublic().getEncoded();
        String rsaPublicString = encoder.encodeToString(rsaPublic);
        //Make the dsa key pair and extract the public key
        KeyPairGenerator dsaKeyGen = KeyPairGenerator.getInstance("DSA");
        dsaKeyGen.initialize(1024);
        byte[] dsaPublic = dsaKeyGen.genKeyPair().getPublic().getEncoded();
        String dsaPublicString = encoder.encodeToString(dsaPublic);
        
       
        String keys = rsaPublicString + " " + dsaPublicString;
        
        //Open the connection to the server
        URL url = new URL(serverURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Accept", "application/json");
        
        //Create a JSON object of our data, then send it
        JsonBuilderFactory factory = Json.createBuilderFactory(null);
        JsonObject obj = Json.createObjectBuilder().add("keyData", keys).build();
        OutputStream os = connection.getOutputStream();
        try (ByteArrayOutputStream byteS = new ByteArrayOutputStream(); ObjectOutput out = new ObjectOutputStream(byteS)) {
            out.writeObject(obj);
            os.write(byteS.toByteArray());
        } 
        connection.setDoOutput(true);
        
        int responseCode = connection.getResponseCode();
        System.out.println(responseCode);
        KeyPairGenerator[] keyPairs = {rsaKeyGen, dsaKeyGen};
        return keyPairs;
    }
    //Function for XORing a byte array
	private static byte[] XorRA(byte[] a, byte[] b, int length) {
		byte[] ra = new byte[length];
		for (int i = 0; i < length; i++) {
			ra[i] = (byte)((int)a[i] ^ (int)b[i]);
		}
		return ra;
	}
	
    //Function for concatenating two byte arrays
	private static byte[] concat(byte[] a, byte[] b) {
		int total = a.length + b.length;
		byte[] c = new byte[total];
		for (int i = 0; i < total; i++) {
			if (i < a.length) {
				c[i] = a[i];
			} else {
				c[i] = b[i- a.length];
			}
		}
		return c;
	}
}
