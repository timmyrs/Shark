package de.timmyrs.Shark;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.timmyrs.Shark.files.SharkDirectory;
import de.timmyrs.Shark.files.SharkFile;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

public class Main
{
	private static Gson GSON = new Gson();
	public static boolean DEBUG = false;
	public static String REPOSITORY_URL;
	public static SharkDirectory REPOSITORY;
	static String USER_CD_PATH = "";

	public static void main(String[] args) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, InvalidKeySpecException, IOException, InvalidAlgorithmParameterException, ClassNotFoundException
	{
		if(args.length < 1)
		{
			System.out.println("java -jar shark.jar browse  Browse a Repository");
			System.out.println("java -jar shark.jar build   Create a Repository");
			System.out.println("\nDocumentation: https://github.com/timmyrs/Shark/blob/master/README.md");
			return;
		}
		switch(args[0].toLowerCase())
		{
			case "connect":
			case "console":
			case "browse":
				if(args.length < 2)
				{
					System.out.println("Syntax: java -jar shark.jar browse <repository-url> [command]\n");
					executeCommand("help");
					return;
				}
				REPOSITORY_URL = args[1];
				if(REPOSITORY_URL.substring(REPOSITORY_URL.length() - 1).equals("/"))
				{
					REPOSITORY_URL = REPOSITORY_URL.substring(0, REPOSITORY_URL.length() - 1);
				}
				REPOSITORY = (SharkDirectory) SharkFile.construct("", "/dir", "", "", "");
				System.out.println("\nConnected to " + REPOSITORY_URL);
				System.out.println("Type 'help' for a list of commands.");
				if(args.length > 2)
				{
					String command = String.join(" ", args).substring(args[0].length() + args[1].length() + 2);
					System.out.println("\n$ " + command + "\n");
					executeCommand(command);
				}
				int b = -1;
				do
				{
					if(System.in.available() > 0)
					{
						if(b == -1)
						{
							b = System.in.read();
						}
						byte[] inArr = new byte[System.in.available() + 1];
						inArr[0] = (byte) b;
						int d = 0;
						for(int i = System.in.available(); i > 0; i--)
						{
							inArr[++d] = (byte) System.in.read();
						}
						String input = new String(inArr, "UTF-8").trim();
						if(!input.equals(""))
						{
							System.out.print("\n");
							if(input.startsWith("exit") || input.startsWith("end") || input.startsWith("disconnect"))
							{
								System.out.println("Goodbye.");
								break;
							}
							executeCommand(input);
						}
						b = -1;
					}
					else
					{
						System.out.print("\n$ ");
						b = System.in.read();
					}
				} while(true);
				break;

			case "build":
				if(args.length < 2)
				{
					System.out.println("Syntax: java -jar shark.jar build <directory> [options]");
					System.out.println("\nOptions:");
					System.out.println("--output <directory>   Sets the output directory.");
				}
				else
				{
					String inputFileName = args[1];
					if(args.length > 2)
					{
						String nextArg = "";
						int i = 0;
						for(String arg : args)
						{
							if(i++ < 2)
							{
								continue;
							}
							if(nextArg.equals("output"))
							{
								SharkBuildOptions.output = arg;
								nextArg = "";
							}
							else
							{
								if(arg.equalsIgnoreCase("--output"))
								{
									nextArg = "output";
								}
								else
								{
									System.out.println("Unknown Argument: " + arg);
								}
							}
						}
						if(!nextArg.equals(""))
						{
							System.out.println("Missing Argument.");
							return;
						}
					}
					if(inputFileName.endsWith("/"))
					{
						inputFileName = inputFileName.substring(0, inputFileName.length() - 1);
					}
					if(SharkBuildOptions.output == null)
					{
						SharkBuildOptions.output = inputFileName + "-built";
					}
					else if(SharkBuildOptions.output.endsWith("/"))
					{
						SharkBuildOptions.output = SharkBuildOptions.output.substring(0, SharkBuildOptions.output.length() - 1);
					}
					File inputFile = new File(inputFileName);
					if(!inputFile.exists() || !inputFile.isDirectory())
					{
						System.out.println(args[1].replace("/", "") + " doesn't exist.");
						return;
					}
					String keyFileName = args[1].split("/")[args[0].split("/").length - 1];
					File publicKeyFile = new File(keyFileName + "-public.txt");
					String privateKeyFileName = keyFileName + "-private-dont-share.txt";
					File privateKeyFile = new File(privateKeyFileName);
					Key publicKey;
					PrivateKey privateKey;
					boolean generatedKeys = false;
					if(publicKeyFile.exists() && privateKeyFile.exists())
					{
						System.out.println("Loading RSA Keypair...");
						String encodedKey = String.join("", Files.readAllLines(Paths.get(privateKeyFileName)));
						encodedKey = encodedKey.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").trim();
						byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
						PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
						KeyFactory kf = KeyFactory.getInstance("RSA");
						privateKey = kf.generatePrivate(keySpec);
						publicKey = kf.generatePublic(new RSAPublicKeySpec(((RSAPrivateCrtKey) privateKey).getModulus(), ((RSAPrivateCrtKey) privateKey).getPublicExponent()));
					}
					else
					{
						System.out.println("Generating RSA Keypair...");
						//noinspection ResultOfMethodCallIgnored
						publicKeyFile.delete();
						//noinspection ResultOfMethodCallIgnored
						privateKeyFile.delete();
						KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
						kpg.initialize(2048);
						KeyPair kp = kpg.genKeyPair();
						publicKey = kp.getPublic();
						privateKey = kp.getPrivate();
						FileWriter writer = new FileWriter(publicKeyFile);
						writer.write("-----BEGIN PUBLIC KEY-----\n");
						writer.write(new String(Base64.getEncoder().encode(publicKey.getEncoded()), "UTF-8"));
						writer.write("\n-----END PUBLIC KEY-----");
						writer.flush();
						writer.close();
						writer = new FileWriter(privateKeyFile);
						writer.write("-----BEGIN PRIVATE KEY-----\n");
						writer.write(new String(Base64.getEncoder().encode(privateKey.getEncoded()), "UTF-8"));
						writer.write("\n-----END PRIVATE KEY-----");
						writer.flush();
						writer.close();
						generatedKeys = true;
					}
					System.out.println("Preparing Repository...");
					MessageDigest md = MessageDigest.getInstance("MD5");
					String name = String.format("%032x", new BigInteger(1, md.digest(inputFileName.getBytes("UTF-8"))));
					File outputFile = new File(SharkBuildOptions.output + "/" + name);
					if(!outputFile.exists() || !outputFile.isDirectory())
					{
						if(!outputFile.mkdirs())
						{
							System.out.println("Failed to create output directory.");
							return;
						}
					}
					JsonArray array = new JsonArray();
					JsonObject fileObj = new JsonObject();
					fileObj.addProperty("name", keyFileName + "-public");
					fileObj.addProperty("type", "/shared/key/rsa");
					array.add(fileObj);
					fileObj = new JsonObject();
					fileObj.addProperty("name", "aes-key");
					fileObj.addProperty("type", "/key/aes");
					fileObj.addProperty("use", keyFileName + "-public");
					array.add(fileObj);
					fileObj = new JsonObject();
					fileObj.addProperty("name", keyFileName);
					fileObj.addProperty("type", "/dir");
					fileObj.addProperty("use", "aes-key");
					array.add(fileObj);
					OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outputFile.getParentFile().getPath() + "/6a992d5529f459a44fee58c733255e86.bin"));
					writer.write(Main.GSON.toJson(array));
					writer.flush();
					writer.close();
					byte[] aesKey;
					File aesKeyFile = new File(outputFile.getParentFile().getPath() + "/3f0fac6f871d37b7cbea85d4e3910099.bin");
					if(aesKeyFile.exists() && !generatedKeys)
					{
						System.out.println("Reading AES Key...");
						Cipher cipher = Cipher.getInstance("RSA");
						cipher.init(Cipher.DECRYPT_MODE, publicKey);
						aesKey = new CipherInputStream(new FileInputStream(aesKeyFile), cipher).readAllBytes();
					}
					else
					{
						System.out.println("Generating AES Key...");
						if(aesKeyFile.exists())
						{
							if(!aesKeyFile.delete())
							{
								System.out.println("Failed to delete " + aesKeyFile.getPath());
								return;
							}
						}
						Cipher cipher = Cipher.getInstance("RSA");
						cipher.init(Cipher.ENCRYPT_MODE, privateKey);
						aesKey = KeyGenerator.getInstance("AES").generateKey().getEncoded();
						OutputStream outputStream = new CipherOutputStream(new FileOutputStream(aesKeyFile), cipher);
						outputStream.write(aesKey);
						outputStream.flush();
						outputStream.close();
					}
					recursivelyCopy(inputFile, outputFile, aesKey);
					System.out.println("\n" + inputFileName + " has been built.\n");
					System.out.println("You may now move the " + SharkBuildOptions.output + " folder on a web server, optionally rename it and then");
					System.out.println("everyone with the full path and " + keyFileName + "-public.txt can access your repository.");
				}
				break;

			default:
				System.out.println("Unknown Argument: " + args[1]);
		}
	}

	static void executeCommand(String input)
	{
		String[] args;
		if(input.contains(" "))
		{
			args = new String[]{input.split(" ")[0], input.substring(input.split(" ")[0].length() + 1)};
		}
		else
		{
			args = new String[]{input};
		}
		SharkDirectory parent;
		switch(args[0].toLowerCase())
		{
			case "debug":
				if(DEBUG)
				{
					DEBUG = false;
					System.out.println("No longer in debug mode.");
				}
				else
				{
					DEBUG = true;
					System.out.println("Now in debug mode.");
				}
				break;

			case "cd":
				if(args.length > 1)
				{
					if(args[1].equals("/"))
					{
						USER_CD_PATH = "";
					}
					else if(args[1].startsWith("/"))
					{
						args[1] = args[1].substring(1);
					}
					try
					{
						SharkFile target = REPOSITORY.resolveRelativePath(USER_CD_PATH + args[1]);
						if(target instanceof SharkDirectory)
						{
							USER_CD_PATH = target.localpath + target.getLocalName();
						}
						else
						{
							System.out.println("You can't cd into file.");
						}
					}
					catch(SharkException e)
					{
						e.printStackTrace();
					}
					if(USER_CD_PATH.startsWith("/"))
					{
						USER_CD_PATH = USER_CD_PATH.substring(1);
					}
				}
				System.out.println("You are operating in '" + USER_CD_PATH + "'.");
				break;

			case "ls":
			case "dir":
				parent = Main.REPOSITORY;
				try
				{
					if(args.length > 1)
					{
						parent = (SharkDirectory) Main.REPOSITORY.resolveRelativePath(USER_CD_PATH + args[1]);
					}
					else
					{
						parent = (SharkDirectory) Main.REPOSITORY.resolveRelativePath(USER_CD_PATH);
					}
				}
				catch(SharkException e)
				{
					e.printStackTrace();
				}
				System.out.print(parent.localpath + parent.getLocalName());
				try
				{
					for(SharkFile child : parent.getChildren())
					{
						System.out.print("\n  " + child.getLocalName());
					}
				}
				catch(SharkException e)
				{
					e.printStackTrace();
				}
				System.out.print("\n");
				break;

			case "tree":
				parent = Main.REPOSITORY;
				try
				{
					if(args.length > 1)
					{
						parent = (SharkDirectory) Main.REPOSITORY.resolveRelativePath(USER_CD_PATH + args[1]);
					}
					else
					{
						parent = (SharkDirectory) Main.REPOSITORY.resolveRelativePath(USER_CD_PATH);
					}
				}
				catch(SharkException e)
				{
					e.printStackTrace();
				}
				System.out.print(parent.localpath + parent.getLocalName());
				parent.recursiveList("  ");
				System.out.print("\n");
				break;

			case "cat":
				if(args.length != 2)
				{
					System.out.println("Syntax: cat <path>");
				}
				try
				{
					SharkFile file = Main.REPOSITORY.resolveRelativePath(USER_CD_PATH + args[1]);
					if(file == null)
					{
						System.out.println(args[1] + " doesn't exist.");
					}
					else
					{
						try
						{
							System.out.println(file.getPlainContent());
						}
						catch(SharkException e)
						{
							e.printStackTrace(file.getLocalName() + ": ");
						}
					}
				}
				catch(SharkException e)
				{
					e.printStackTrace();
				}
				break;

			case "cat-bin":
				if(args.length != 2)
				{
					System.out.println("Syntax: cat-bin <path>");
				}
				try
				{
					SharkFile file = Main.REPOSITORY.resolveRelativePath(USER_CD_PATH + args[1]);
					if(file == null)
					{
						System.out.println(args[1] + " doesn't exist.");
					}
					else
					{
						int i = 0;
						try
						{
							for(byte b : file.getBinaryContent())
							{
								System.out.printf("%02x ", b);
								if(++i == 16)
								{
									System.out.print("\n");
									i = 0;
								}
							}
						}
						catch(SharkException e)
						{
							e.printStackTrace(file.getLocalName() + ": ");
						}
						if(i > 0)
						{
							System.out.print("\n");
						}
					}
				}
				catch(SharkException e)
				{
					e.printStackTrace();
				}
				break;

			case "dl":
			case "download":
				if(args.length != 2)
				{
					System.out.println("Syntax: download <path>");
					return;
				}
				try
				{
					SharkFile file = Main.REPOSITORY.resolveRelativePath(USER_CD_PATH + args[1]);
					if(file == null)
					{
						System.out.println(args[1] + " doesn't exist.");
					}
					else
					{
						if(file instanceof SharkDirectory)
						{
							System.out.print(Main.REPOSITORY_URL);
						}
						else
						{
							System.out.print(file.localpath);
						}
						file.download("  ", "");
						System.out.print("\n");
					}
				}
				catch(SharkException e)
				{
					e.printStackTrace();
				}
				break;

			default:
				System.out.println("Unknown Command '" + args[0].toLowerCase() + "'.\n");
			case "help":
				System.out.println("Commands:\n");
				System.out.println("cd                     Change the current directory.");
				System.out.println("ls, dir [path]         Lists all files in the current directory or path.");
				System.out.println("tree [path]            Recursively lists all files in the current directory or path.");
				System.out.println("cat <path>             Shows the content of the file at the given path.");
				System.out.println("cat-bin <path>         Shows the binary data of the file at the given path.");
				System.out.println("dl, download <path>    Downloads the given file or folder into the current working directory.");
				System.out.println("exit, end, disconnect  Ends the console session.");
		}
	}

	private static void recursivelyCopy(File inputFile, File outputFile, byte[] aesKey) throws IOException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchProviderException
	{
		JsonArray array = new JsonArray();
		//noinspection ConstantConditions
		for(File file : inputFile.listFiles())
		{
			JsonObject fileObj = new JsonObject();
			MessageDigest md = MessageDigest.getInstance("MD5");
			String name;
			if(file.isDirectory())
			{
				name = String.format("%032x", new BigInteger(1, md.digest(file.getName().getBytes("UTF-8"))));
				fileObj.addProperty("name", file.getName());
				fileObj.addProperty("type", "/dir");
				File outFile = new File(outputFile.getPath() + "/" + name);
				if((outFile.exists() && outFile.isDirectory()) || outFile.mkdir())
				{
					File indexJson = new File(outFile.getPath() + "/6a992d5529f459a44fee58c733255e86.bin");
					if(!indexJson.exists() && !indexJson.createNewFile())
					{
						System.out.println("Failed to create " + indexJson.getAbsolutePath());
					}
					else
					{
						recursivelyCopy(file, outFile, aesKey);
					}
				}
				else
				{
					System.out.println("Failed to create " + outFile.getAbsolutePath());
				}
			}
			else
			{
				name = String.format("%032x", new BigInteger(1, md.digest(file.getName().split("\\.")[0].getBytes("UTF-8"))));
				fileObj.addProperty("name", file.getName().split("\\.")[0]);
				File outFile = new File(outputFile.getPath() + "/" + name + ".bin");
				if(!outFile.exists())
				{
					long millis = System.currentTimeMillis();
					System.out.print("Encrypting " + file.getName() + "...");
					Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");
					SecretKeySpec key = new SecretKeySpec(aesKey, "AES");
					cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec("AAAAAAAAAAAAAAAA".getBytes("UTF-8")));
					InputStream inputStream = new CipherInputStream(new FileInputStream(file), cipher);
					Files.copy(inputStream, Paths.get(outputFile.getPath() + "/" + name + ".bin"));
					System.out.println(" Done in " + (System.currentTimeMillis() - millis) + "ms.");
				}
				fileObj.addProperty("type", file.getName().substring(file.getName().split("\\.")[0].length() + 1));
			}
			fileObj.addProperty("use", "aes-key");
			array.add(fileObj);
		}
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");
		SecretKeySpec key = new SecretKeySpec(aesKey, "AES");
		cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec("AAAAAAAAAAAAAAAA".getBytes("UTF-8")));
		OutputStreamWriter writer = new OutputStreamWriter(new CipherOutputStream(new FileOutputStream(new File(outputFile.getPath() + "/6a992d5529f459a44fee58c733255e86.bin")), cipher));
		writer.write(Main.GSON.toJson(array));
		writer.flush();
		writer.close();
		//noinspection ConstantConditions
		for(File file : outputFile.listFiles())
		{
			if(!file.getName().equals("6a992d5529f459a44fee58c733255e86.bin"))
			{
				boolean relevant = false;
				for(JsonElement elm : array)
				{
					JsonObject obj = (JsonObject) elm;
					MessageDigest md = MessageDigest.getInstance("MD5");
					String name = String.format("%032x", new BigInteger(1, md.digest(obj.get("name").getAsString().getBytes("UTF-8"))));
					if(file.getName().substring(0, name.length()).equals(name))
					{
						relevant = true;
						break;
					}
				}
				if(!relevant)
				{
					if(file.isDirectory())
					{
						deleteFolder(file);
					}
					else if(!file.delete())
					{
						System.out.println("Failed to delete " + file.getAbsolutePath());
					}
				}
			}
		}
	}

	private static void deleteFolder(File folder)
	{
		//noinspection ConstantConditions
		for(File file : folder.listFiles())
		{
			if(file.isDirectory())
			{
				deleteFolder(file);
			}
			else
			{
				if(!file.delete())
				{
					System.out.println("Failed to delete " + file.getAbsolutePath());
				}
			}
		}
		if(!folder.delete())
		{
			System.out.println("Failed to delete " + folder.getAbsolutePath());
		}
	}
}
