package shark.Shark;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import shark.Shark.files.SharkDirectory;
import shark.Shark.files.SharkFile;

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
	public static String REPOSITORY_URL;
	public static SharkDirectory REPOSITORY;

	public static void main(String[] args) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, InvalidKeySpecException, IOException, InvalidAlgorithmParameterException, ClassNotFoundException
	{
		if(args.length < 1)
		{
			System.out.println("java -jar shark.jar browse  Browse a Repository");
			System.out.println("java -jar shark.jar build   Create a Repository");
			return;
		}
		switch(args[0].toLowerCase())
		{
			case "browse":
				if(args.length < 3)
				{
					System.out.println("Syntax: java -jar shark.jar browse <repository-url> <action>");
					System.out.println("");
					System.out.println("Possible Actions:");
					System.out.println("--list                  Recursively lists all files in the repository.");
					System.out.println("--read <path>           Shows the content of the file at the given path.");
					System.out.println("--read-binary <path>    Shows the binary data of the file at the given path.");
					System.out.println("-dl, --download <path>  Downloads the given file into the current working directory.");
					System.out.println("--download-as-is <path> Downloads the given file into the same relative directory as it is in the repository.");
					return;
				}
				REPOSITORY_URL = args[1];
				if(REPOSITORY_URL.substring(REPOSITORY_URL.length() - 1).equals("/"))
				{
					REPOSITORY_URL = REPOSITORY_URL.substring(0, REPOSITORY_URL.length() - 1);
				}
				REPOSITORY = (SharkDirectory) SharkFile.construct("", "/dir", "", "", "");
				SharkFile file;
				switch(args[2].toLowerCase())
				{
					default:
						System.out.println("Unknown Argument: " + args[2]);
						return;

					case "--list":
						System.out.print("\n" + REPOSITORY_URL);
						recursiveList(REPOSITORY, "  ");
						System.out.print("\n");
						break;

					case "--read":
						if(args.length != 4)
						{
							System.out.println("Syntax: java -jar shark.jar browse <repository-url> --read <path>");
						}
						try
						{
							file = REPOSITORY.resolveRelativePath(args[3]);
							if(file == null)
							{
								System.out.println(args[3] + " doesn't exist.");
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

					case "--read-binary":
						if(args.length != 4)
						{
							System.out.println("Syntax: java -jar shark.jar browse <repository-url> --read-binary <path>");
						}
						try
						{
							file = REPOSITORY.resolveRelativePath(args[3]);
							if(file == null)
							{
								System.out.println(args[3] + " doesn't exist.");
							}
							else
							{
								int i = 0;
								try
								{
									for(byte b : file.getBinaryContent())
									{
										System.out.printf("%02x ", b);
										if(++i == 10)
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

					case "-dl":
					case "--download":
						if(args.length != 4)
						{
							System.out.println("Syntax: java -jar shark.jar browse <repository-url> --download <path>");
							return;
						}
						try
						{
							file = REPOSITORY.resolveRelativePath(args[3]);
							if(file == null)
							{
								System.out.println(args[3] + " doesn't exist.");
							}
							else
							{
								long millis = System.currentTimeMillis();
								file.downloadTo(Paths.get(file.getLocalName()));
								System.out.print(" Downloaded & decrypted in " + millis + "ms.");
							}
						}
						catch(SharkException e)
						{
							e.printStackTrace();
						}
						break;

					case "--download-as-is":
						if(args.length < 4)
						{
							System.out.println("Syntax: java -jar shark.jar browse <repository-url> --download-as-is <path>");
							return;
						}
						try
						{
							file = REPOSITORY.resolveRelativePath(args[3]);
							if(file == null)
							{
								System.out.println(args[3] + " doesn't exist.");
							}
							else
							{
								System.out.print("\n");
								if(file instanceof SharkDirectory)
								{
									recursiveDownload(file, "");
								}
								else
								{
									System.out.print(file.path);
									recursiveDownload(file, "  ");
								}
								System.out.print("\n");
							}
						}
						catch(SharkException e)
						{
							e.printStackTrace();
						}
				}
				break;

			case "build":
				if(args.length != 2)
				{
					System.out.println("Syntax: java -jar shark.jar build <directory>");
				}
				else
				{
					String inputFileName = args[1];
					if(inputFileName.substring(inputFileName.length() - 1).equals("/"))
					{
						inputFileName = inputFileName.substring(0, inputFileName.length() - 1);
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
						writer.write("-----BEGIN PRIVATE KEY-----\n");
						writer.write(new String(Base64.getEncoder().encode(publicKey.getEncoded()), "UTF-8"));
						writer.write("\n-----END PRIVATE KEY-----");
						writer.flush();
						writer.close();
						writer = new FileWriter(privateKeyFile);
						writer.write("-----BEGIN PUBLIC KEY-----\n");
						writer.write(new String(Base64.getEncoder().encode(privateKey.getEncoded()), "UTF-8"));
						writer.write("\n-----END PUBLIC KEY-----");
						writer.flush();
						writer.close();
					}
					System.out.println("Preparing Repository...");
					MessageDigest md = MessageDigest.getInstance("MD5");
					String name = String.format("%032x", new BigInteger(1, md.digest(inputFileName.getBytes("UTF-8"))));
					File outputFile = new File(inputFileName + "-built/" + name);
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
					writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(array));
					writer.flush();
					writer.close();
					byte[] aesKey;
					File aesKeyFile = new File(outputFile.getParentFile().getPath() + "/3f0fac6f871d37b7cbea85d4e3910099.bin");
					if(aesKeyFile.exists())
					{
						System.out.println("Reading AES Key...");
						Cipher cipher = Cipher.getInstance("RSA");
						cipher.init(Cipher.DECRYPT_MODE, publicKey);
						aesKey = new CipherInputStream(new FileInputStream(aesKeyFile), cipher).readAllBytes();
					}
					else
					{
						System.out.println("Generating AES Key...");
						Cipher cipher = Cipher.getInstance("RSA");
						cipher.init(Cipher.ENCRYPT_MODE, privateKey);
						aesKey = KeyGenerator.getInstance("AES").generateKey().getEncoded();
						OutputStream outputStream = new CipherOutputStream(new FileOutputStream(aesKeyFile), cipher);
						outputStream.write(aesKey);
						outputStream.flush();
						outputStream.close();
					}
					recursivelyMove(inputFile, outputFile, aesKey);
					System.out.println("\n" + inputFileName + " has been built.\n");
					System.out.println("You may now move the " + inputFileName + "-built folder on a webserver, optionally rename it and then");
					System.out.println("everyone with the full path and " + keyFileName + "-public.txt can access your repository.\n");
				}
				break;

			default:
				System.out.println("Unknown Argument: " + args[1]);
		}
	}

	private static void recursivelyMove(File inputFile, File outputFile, byte[] aesKey) throws IOException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchProviderException
	{
		JsonArray array = new JsonArray();
		//noinspection ConstantConditions
		for(File file : inputFile.listFiles())
		{
			JsonObject fileObj = new JsonObject();
			fileObj.addProperty("name", file.getName().split("\\.")[0]);
			MessageDigest md = MessageDigest.getInstance("MD5");
			String name = String.format("%032x", new BigInteger(1, md.digest(file.getName().split("\\.")[0].getBytes("UTF-8"))));
			if(file.isDirectory())
			{
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
						recursivelyMove(file, outFile, aesKey);
					}
				}
				else
				{
					System.out.println("Failed to create " + outFile.getAbsolutePath());
				}
			}
			else
			{
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
				fileObj.addProperty("type", file.getName().split("\\.")[1]);
			}
			fileObj.addProperty("use", "aes-key");
			array.add(fileObj);
		}
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");
		SecretKeySpec key = new SecretKeySpec(aesKey, "AES");
		cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec("AAAAAAAAAAAAAAAA".getBytes("UTF-8")));
		OutputStreamWriter writer = new OutputStreamWriter(new CipherOutputStream(new FileOutputStream(new File(outputFile.getPath() + "/6a992d5529f459a44fee58c733255e86.bin")), cipher));
		writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(array));
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

	private static void recursiveDownload(SharkFile file, String prepend)
	{
		try
		{
			System.out.print("\n" + prepend + file.getLocalName());
			if(file instanceof SharkDirectory)
			{
				for(SharkFile child : ((SharkDirectory) file).getChildren())
				{
					recursiveDownload(child, prepend + "  ");
				}
			}
			else
			{
				long millis = System.currentTimeMillis();
				file.downloadTo(Paths.get(System.getProperty("user.dir") + file.localpath + file.getLocalName()));
				System.out.print(": Downloaded & decrypted in " + (System.currentTimeMillis() - millis) + "ms.");
			}
		}
		catch(SharkException e)
		{
			System.out.print(": " + e.getMessage());
		}
	}

	private static void recursiveList(SharkDirectory directory, String prepend)
	{
		try
		{
			for(SharkFile file : directory.getChildren())
			{
				System.out.print("\n" + prepend + file.getLocalName());
				if(file instanceof SharkDirectory)
				{
					recursiveList((SharkDirectory) file, prepend + "  ");
				}
			}
		}
		catch(SharkException e)
		{
			System.out.print(" " + e.getMessage());
		}
	}
}
