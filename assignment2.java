import java.security.*;
import java.security.spec.*;
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
import java.util.zip.CRC32;
import java.nio.ByteBuffer;
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
                    case "-cp":
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
        KeyPair[] keys = new KeyPair[2];
        try {
            keys = registerKeys(serverURL, port, username);
            System.out.println("Successfully registered a public key for '" + username + "'.");
            System.out.println("Server connection successful. Type (h)elp for commands.");
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
                    try {
                        getAllMessages(keys, serverURL, port, username);
                    } catch (Exception e) {
                        System.out.println(e);
                        System.out.println("Failure in getting all messages");
                    }                   
                    break;
                case "c": case "compose":
                    String otherUser;
                    try {
                        otherUser = commands[1];
                    } catch (Exception e) {
                        System.out.println("must provide a recipient user");
                        break;
                    }
                    try {
                        System.out.println("Enter your message and hit return (empty line cancels message):");
                        String message = kb.nextLine();
                        composeMessage(keys, serverURL, port, username, otherUser, message);
                    } catch (Exception e) {
                        System.out.println(e);
                        System.out.println("Failure in sending message");
                    }
                    break;
                case "f": case "fingerprint":
                    try {
                        otherUser = commands[1];
                    } catch (Exception e) {
                        System.out.println("must provide a recipient user");
                        break;
                    }
                    try {
                        System.out.println("Your key fingerprint:");
                        getFingerPrint(serverURL, port, username);
                        System.out.println("\nFingerprint for user " + otherUser + ":");
                        getFingerPrint(serverURL, port, otherUser);
                        System.out.println();
                    } catch (Exception e) {
                        System.out.println(e);
                        System.out.println("Failure in getting user fingerprints");
                    }
                    break;
                case "l": case "list":
                    try {
                        getAllUsers(serverURL, port);
                    } catch (Exception e) {
                        System.out.println(e);
                        System.out.println("Failure in getting all users");
                    }
                    break;
                case "genkeys":
                    System.out.println("Generating a new keypair...");
                    try {
                        keys = registerKeys(serverURL, port, username);
                        System.out.println("Server connection successful. Type (h)elp for commands.)");

                    }catch (Exception e) {
                        System.out.println(e);
                        System.out.println("Failure in registering keys, now exiting");
                        System.exit(0);
                    }
                    break;
                case "h": case "help":
                    printCommands();
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
    
    private static void printCommands() {
        System.out.println("Available commands:");
        System.out.println("    get (or empty line)     - check for new messages");
        System.out.println("    c(ompose) <user>        - compose a message to <user>");
        System.out.println("    f(ingerprint) <user>    - return the key fingerprint of <user>");
        System.out.println("    l(ist)                  - lists all the users in the system");
        System.out.println("    genkeys                 - generates and registers a fresh keypair");
        System.out.println("    h(elp)                  - prints this listing");
        System.out.println("    q(uit)                  - exits");
    }
    
    private static void getFingerPrint(String serverURL, String port, String username)throws NoSuchAlgorithmException, NoSuchProviderException, IOException {
        //Get the utf8 encoded key string
        String keyString = getUserKeys(serverURL, port, username);
        
        //Use sha256 to hash the key string
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashedKeys = digest.digest(keyString.getBytes());
        for (int i = 0; i < 32; i++) {
            System.out.print(String.format("%02x", hashedKeys[i]) + " ");
        }
    }
    private static void composeMessage(KeyPair[] keys, String serverURL,String port, String username, String otherUser, String message) throws NoSuchAlgorithmException, NoSuchProviderException, IOException, ProtocolException, MalformedURLException {
        if (!message.equals("")) {
            String otherKeys = "";
            try {
                otherKeys = getUserKeys(serverURL, port, otherUser);
            } catch (Exception e) {
                System.out.println("Failure to retrieve other users keys");
                System.out.println(e);
                return;
            }
            
            String messagePacket;
            try {
                messagePacket = encrypt(keys, username, otherUser, otherKeys, message);
            } catch (Exception e) {
                System.out.println("Failure in encrypting the message");
                System.out.println(e);
                return;
            }
            
            //Open the connection to the server
            serverURL = "http://" + serverURL + ":" + port;
            serverURL += "/sendMessage/" + username;
            URL url = new URL(serverURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
    
            try( DataOutputStream os = new DataOutputStream(connection.getOutputStream())) {
                os.write(messagePacket.getBytes());
            }
            
            connection.getResponseMessage();
        }
    }
    
    private static String encrypt(KeyPair[] keys, String username, String otherUser, String otherKeys, String message) throws NoSuchAlgorithmException, NoSuchProviderException, IOException, ProtocolException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, SignatureException {
        Base64.Decoder decoder = Base64.getDecoder();
        Base64.Encoder encoder = Base64.getEncoder();
        //Create rsa publicKey and dsa publicKey Objects from otherKeys string
        String[] otherPublicKeys = otherKeys.split("%");
        byte[] rsaByteKey = decoder.decode(otherPublicKeys[0].getBytes());
        X509EncodedKeySpec rsaX509Key = new X509EncodedKeySpec(rsaByteKey);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey otherRSAPubKey = kf.generatePublic(rsaX509Key);
        
        //Get a key instance of AES size 128
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SecretKey aesKey = keyGen.generateKey();

        
        //Create c1
        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/Pkcs1Padding");
        rsaCipher.init(Cipher.ENCRYPT_MODE, otherRSAPubKey);
        byte[] c1 = rsaCipher.doFinal(aesKey.getEncoded());
        
        //Create Mformatted
        String mformatted = username + ":" + message;
        byte[] mformatRA = mformatted.getBytes();
        
        //Create the CRC32 value
        CRC32 crc = new CRC32();
        crc.update(mformatted.getBytes());
        long crcVal = crc.getValue();
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(crcVal);
        byte[] crcRA = Arrays.copyOfRange(buffer.array(), 4, 8);
        byte[] mcrc = concat(mformatRA, crcRA);
        
        //Add PKCS5 padding to the message
        byte[] mpadded = concat(mcrc,Pkcs(mcrc));

        //Create a secure random IV
        byte[] IV = new byte[16];
        new SecureRandom().nextBytes(IV);
        
        //Encrypt using AES CTR mode
        Cipher aes = null;
        byte[] c2 = null;
	    try {
	    	aes = Cipher.getInstance("AES/CTR/NoPadding");
	    	aes.init(aes.ENCRYPT_MODE, aesKey, new IvParameterSpec(IV));    
            c2 = aes.doFinal(mpadded);
	    } catch (Exception e){
	    	e.printStackTrace();
	    }
        c2 = concat(IV, c2);
        
        //Get the base64 encoded versions of c1 and c2
        byte[] c1base64 = encoder.encode(c1);
        byte[] c2base64 = encoder.encode(c2);
        String c1base64String = new String(c1base64);
        String c2base64String = new String(c2base64);
        String combined = c1base64String + " " + c2base64String;
        
        Signature dsaSig = Signature.getInstance("DSA");
        dsaSig.initSign(keys[1].getPrivate());
        dsaSig.update(combined.getBytes());
        byte[] signature = encoder.encode(dsaSig.sign());
        
        //Final ciphertext
        String output = combined + " " + new String(signature);
        
        //Create JsonObject to send to server
        JsonBuilderFactory factory = Json.createBuilderFactory(null);
        JsonObject obj = Json.createObjectBuilder().add("recipient", otherUser).add("messageID", "0").add("message", output).build();
        String objString = obj.toString();

        return objString;
    }
   
    /*Function for calculating the appropriate PKCS padding for a message*/
	private static byte[] Pkcs(byte[] messageX) {
		byte[] ps;
		int n = messageX.length % 16;
		ps = new byte[16 - n];
		Arrays.fill(ps, (byte)(16-n));
		return ps;
	}
    
    private static KeyPair[] registerKeys(String serverURL, String port, String username) throws NoSuchAlgorithmException, NoSuchProviderException, IOException, ProtocolException, MalformedURLException {
        Base64.Encoder encoder = Base64.getEncoder();
        //Make the rsa key pair and extract the public key
        KeyPairGenerator rsaKeyGen = KeyPairGenerator.getInstance("RSA");
        rsaKeyGen.initialize(1024);
        KeyPair rsa = rsaKeyGen.genKeyPair();
        byte[] rsaPublic = rsa.getPublic().getEncoded();
        String rsaPublicString = new String(encoder.encode(rsaPublic));
        //Make the dsa key pair and extract the public key
        KeyPairGenerator dsaKeyGen = KeyPairGenerator.getInstance("DSA");
        dsaKeyGen.initialize(1024);
        KeyPair dsa = dsaKeyGen.genKeyPair();
        byte[] dsaPublic = dsa.getPublic().getEncoded();
        String dsaPublicString = new String(encoder.encode(dsaPublic));
       
        String keys = rsaPublicString + "%" + dsaPublicString;
        
        //Open the connection to the server
        serverURL = "http://" + serverURL + ":" + port;
        serverURL += "/registerKey/" + username;
        URL url = new URL(serverURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        
        //Create a JSON object of our data, then send it
        JsonBuilderFactory factory = Json.createBuilderFactory(null);
        JsonObject obj = Json.createObjectBuilder().add("keyData", keys).build();
        String objString = obj.toString();
        
        try( DataOutputStream os = new DataOutputStream( connection.getOutputStream())) {
            os.write(objString.getBytes());
        }

        connection.getResponseMessage();
        KeyPair[] keyPairs = {rsa, dsa};
        return keyPairs;
    }
    
    private static String getUserKeys(String serverURL, String port, String otherUser) throws NoSuchAlgorithmException, NoSuchProviderException, IOException, ProtocolException, MalformedURLException {
        //Open the connection to the server
        serverURL = "http://" + serverURL + ":" + port;
        serverURL += "/lookupKey/" + otherUser.trim();
        URL url = new URL(serverURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");

        BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String keyString;
        
        keyString = input.readLine();
        input.close();
        //Create Json object from the json esque string
        JsonReader reader = Json.createReader(new StringReader(keyString));
        JsonObject userKeys = reader.readObject();
        reader.close();
       
        return userKeys.getString("keyData");
    }
    
    private static void getAllUsers(String serverURL, String port) throws NoSuchAlgorithmException, NoSuchProviderException, IOException, ProtocolException, MalformedURLException {
        //Open the connection to the server
        serverURL = "http://" + serverURL + ":" + port;
        serverURL += "/lookupUsers";
        URL url = new URL(serverURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");

        //Get all the usernames
        BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String users;
        users = input.readLine();
        input.close();
        
        //Create Json object from the json esque string
        JsonReader reader = Json.createReader(new StringReader(users));
        JsonObject allUsers = reader.readObject();
        reader.close();
        
        //Print all the users from the json object
        int numUsers = allUsers.getInt("numUsers");
        JsonArray userJsonArray = allUsers.getJsonArray("users");
        for (int i = 0; i < numUsers; i++) {
            System.out.println(i + ":" + userJsonArray.get(i));
        }
        System.out.println();
    }
    
    private static void getAllMessages(KeyPair[] keys, String serverURL, String port, String username) throws NoSuchAlgorithmException, NoSuchProviderException, IOException, ProtocolException, MalformedURLException {
        String origURL = serverURL;
        //Open the connection to the server
        serverURL = "http://" + serverURL + ":" + port;
        serverURL += "/getMessages/" + username;
        URL url = new URL(serverURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");

        //Get all the messages
        BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String messages = input.readLine();
        input.close();
        
        //Create Json object from the json esque string
        JsonReader reader = Json.createReader(new StringReader(messages));
        JsonObject allMessages = reader.readObject();
        reader.close();
        
        int numMessages = allMessages.getInt("numMessages");
        JsonArray messageMeta = allMessages.getJsonArray("messages");
        
        for (int i = 0; i < messageMeta.size(); i++) {
            reader = Json.createReader(new StringReader(messageMeta.get(i).toString()));
            JsonObject message = reader.readObject();
            try {
                decrypt(keys, username, origURL, port, message);
            } catch (Exception e) {
                System.out.println("Exception while attempting to decrypt message");
                System.out.println(e);
                continue;
            }
        }
    }
    
    private static boolean decrypt(KeyPair[] keys, String username, String serverURL, String port, JsonObject messageData) throws NoSuchAlgorithmException, NoSuchProviderException, IOException, ProtocolException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, SignatureException {
        Base64.Decoder decoder = Base64.getDecoder();
        //Get all thed aata from the messageData
        int time = messageData.getInt("sentTime");
        String senderID = messageData.getString("senderID");
        int messageID = messageData.getInt("messageID");
        String encryptedMessage = messageData.getString("message");
        String[] message = encryptedMessage.split(" ");
        
        //Split the message into its parts and decode
        String c1Base64 = message[0];
        String c2Base64 = message[1];
        byte[] c1 = decoder.decode(message[0]);
        byte[] c2 = decoder.decode(message[1]);
        byte[] sigma = decoder.decode(message[2].getBytes());
        
        String otherKeyString = getUserKeys(serverURL, port, senderID);
        String[] otherKeys = otherKeyString.split("%");
              
        //Verify dsa signature
        byte[] dsaByteKey = decoder.decode(otherKeys[1].getBytes());
        X509EncodedKeySpec dsaX509Key = new X509EncodedKeySpec(dsaByteKey);
        KeyFactory kf = KeyFactory.getInstance("DSA");
        PublicKey otherDSAPubKey = kf.generatePublic(dsaX509Key);
        
        //Verify the signature
        Signature dsaSig = Signature.getInstance("DSA");
        dsaSig.initVerify(otherDSAPubKey);
        dsaSig.update((c1Base64 + " " + c2Base64).getBytes());
        boolean verified = dsaSig.verify(sigma);
        if (!verified) {
            return false;
        }
        
        //Find K
        byte[] K = null;
        
        try {
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/Pkcs1Padding");
            rsaCipher.init(Cipher.DECRYPT_MODE, keys[0].getPrivate());
            K = rsaCipher.doFinal(c1);
        }catch (Exception e) {
            return false;
        }
        SecretKey aesKey = new SecretKeySpec(K, 0, K.length, "AES"); 
        
        //Find the Mpadded
        byte[] mpadded = null;
        try {
            byte[] IV = Arrays.copyOfRange(c2, 0, 16);
    	    Cipher aes = Cipher.getInstance("AES/CTR/NoPadding");
        	aes.init(aes.DECRYPT_MODE, aesKey, new IvParameterSpec(IV));    
            mpadded = aes.doFinal(Arrays.copyOfRange(c2, 16, c2.length));
        }catch (Exception e) {
            return false;
        }
        //Verify the pkcs padding
        byte endByte = mpadded[mpadded.length - 1];
        for (int i = 1; i < (int)endByte + 1; i++) {
            if (mpadded[mpadded.length - i] != endByte) {
                return false;
            }
        }
        //Compute mcrc and verify the crc
        byte[] mcrc = Arrays.copyOfRange(mpadded, 0, mpadded.length - (int)endByte);
        byte[] mformatted = Arrays.copyOfRange(mcrc, 0, mcrc.length - 4);
        byte[] crc = Arrays.copyOfRange(mcrc, mcrc.length - 4, mcrc.length);
        CRC32 verifyCrc = new CRC32();
        verifyCrc.update(mformatted);
        long crcVal = verifyCrc.getValue();
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(crcVal);
        byte[] crcRA = Arrays.copyOfRange(buffer.array(), 4, 8);
        
        for (int i = 0; i < 4; i++) {
            if (crcRA[i] != crc[i]) {
                return false;
            }
        }
        
        //Parse Mformatted as user message
        String mformmatedString = new String(mformatted);
        String[] messageParts = mformmatedString.split(":");
        if (!messageParts[0].equals(senderID)) {
            return false;
        }
        
        if (!messageParts[1].contains("READMESSAGE")) {
            composeMessage(keys, serverURL, port, username, senderID, "READMESSAGE " + messageID);
        }
        System.out.println("Message ID: " + messageID);
        System.out.println("From: " + senderID);
        System.out.println(mformmatedString.substring(messageParts[0].length() + 1, mformmatedString.length()));
        return true;
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
