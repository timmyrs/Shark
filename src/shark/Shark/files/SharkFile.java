package shark.Shark.files;

import shark.Shark.Main;
import shark.Shark.SharkException;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class SharkFile
{
	final String name;
	private final String type;
	final String path;
	public final String localpath;
	private final String use;
	final boolean shared;
	private byte[] content;

	SharkFile(String name, String type, String path, String localpath, String use, boolean shared)
	{
		this.name = name;
		this.type = type;
		this.path = path;
		this.localpath = localpath;
		this.use = use;
		this.shared = shared;
	}

	public static SharkFile construct(String name, String type, String path, String localpath, String use)
	{
		boolean shared = false;
		if(type.length() > 7 && type.substring(0, 7).equals("/shared"))
		{
			type = type.substring(7);
			shared = true;
		}
		if(type.equals("/dir"))
		{
			return new SharkDirectory(name, path, localpath, use, shared);
		}
		return new SharkFile(name, type, path, localpath, use, shared);
	}

	String getRemoteName()
	{
		if(this.name.equals(""))
		{
			return this.name;
		}
		String name = this.name;
		name = name.split("\\.")[0];
		try
		{
			MessageDigest md = MessageDigest.getInstance("MD5");
			name = String.format("%032x", new BigInteger(1, md.digest(name.getBytes("UTF-8"))));
		}
		catch(NoSuchAlgorithmException | UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		return name;
	}

	public String getLocalName()
	{
		if(type.substring(0, 1).equals("/"))
		{
			if(type.equals("/dir"))
			{
				return name + "/";
			}
			else
			{
				return name + (this.use.equals("") ? ".txt" : ".bin");
			}
		}
		else
		{
			return name + "." + type;
		}
	}

	public String getPlainContent() throws SharkException
	{
		try
		{
			return new String(this.getBinaryContent(), "UTF-8");
		}
		catch(UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		return "";
	}

	private Cipher getCipher() throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, UnsupportedEncodingException, InvalidAlgorithmParameterException, InvalidKeyException, InvalidKeySpecException, SharkException
	{
		if(this.content == null)
		{
			this.getBinaryContent();
		}
		Cipher cipher;
		if(this.type.length() > 5 && this.type.substring(0, 5).equals("/key/"))
		{
			String cipherType = this.type.substring(5).toUpperCase();
			if(this.content.length == 0)
			{
				if(this.shared)
				{
					throw new SharkException("You need the " + this.getLocalName() + " " + cipherType + " key to access this.");
				}
				else
				{
					throw new SharkException("The repository requires a key to access this which it doesn't provide.");
				}
			}
			switch(cipherType)
			{
				default:
					throw new SharkException("The repository requires the non-supported encryption \"" + cipherType + "\" to be used to access this.");

				case "AES":
					cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");
					SecretKeySpec key = new SecretKeySpec(content, "AES");
					cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec("AAAAAAAAAAAAAAAA".getBytes("UTF-8")));
					break;

				case "RSA":
					cipher = Cipher.getInstance("RSA");
					String encodedKey = new String(content, "UTF-8");
					encodedKey = encodedKey.replaceAll("\\r?\\n", "").replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "").trim();
					byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
					X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKey);
					KeyFactory kf = KeyFactory.getInstance("RSA");
					PublicKey publicKey = kf.generatePublic(keySpec);
					cipher.init(Cipher.DECRYPT_MODE, publicKey);
			}
		}
		else
		{
			throw new RuntimeException(this.getLocalName() + " is not a key.");
		}
		return cipher;
	}

	public byte[] getBinaryContent() throws SharkException
	{
		if(this.content != null)
		{
			return content;
		}
		try
		{
			if(this.shared)
			{
				content = new FileInputStream(new File(System.getProperty("user.dir") + this.path + this.getLocalName())).readAllBytes();
			}
			else
			{
				content = new URL(Main.REPOSITORY_URL + this.path + this.getRemoteName() + ".bin").openConnection().getInputStream().readAllBytes();
			}
		}
		catch(IOException e)
		{
			content = new byte[]{};
			if(!this.shared)
			{
				e.printStackTrace();
				throw new SharkException("The repository doesn't know " + this.getLocalName() + ".");
			}
		}
		if(!this.use.equals(""))
		{
			try
			{
				SharkFile keyFile = Main.REPOSITORY.resolveRelativePath(this.use);
				if(keyFile == null)
				{
					throw new SharkException("The key \"" + this.use + "\" was never defined.");
				}
				Cipher cipher = keyFile.getCipher();
				content = cipher.doFinal(content);
			}
			catch(NoSuchPaddingException | NoSuchAlgorithmException | UnsupportedEncodingException | NoSuchProviderException | InvalidKeyException | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException | InvalidKeySpecException e)
			{
				e.printStackTrace();
			}
		}
		return content;
	}

	public void download(String prepend, String localpath)
	{
		try
		{
			System.out.print("\n" + prepend + this.getLocalName());
			if(this instanceof SharkDirectory)
			{
				for(SharkFile child : ((SharkDirectory) this).getChildren())
				{
					child.download(prepend + "  ", localpath + this.getLocalName());
				}
			}
			else
			{
				long millis = System.currentTimeMillis();
				this.downloadTo(Paths.get(System.getProperty("user.dir") + "/" + localpath + this.getLocalName()));
				System.out.print(": Downloaded & decrypted in " + (System.currentTimeMillis() - millis) + "ms.");
			}
		}
		catch(SharkException e)
		{
			System.out.print(": " + e.getMessage());
		}
	}

	private void downloadTo(Path path) throws SharkException
	{
		File file = path.toFile();
		if(file.exists())
		{
			throw new SharkException("File already exists locally.");
		}
		if(file.getParentFile() != null && !file.getParentFile().exists())
		{
			if(!file.getParentFile().mkdirs())
			{
				throw new SharkException("Failed to create local directories.");
			}
		}
		if(this.shared)
		{
			throw new SharkException("Refusing to download shared file.");
		}
		try
		{
			InputStream inputStream = new URL(Main.REPOSITORY_URL + this.path + this.getRemoteName() + ".bin").openConnection().getInputStream();
			if(!this.use.equals(""))
			{
				try
				{
					SharkFile keyFile = Main.REPOSITORY.resolveRelativePath(this.use);
					if(keyFile == null)
					{
						throw new SharkException("The key \"" + this.use + "\" was never defined.");
					}
					Cipher cipher = keyFile.getCipher();
					inputStream = new CipherInputStream(inputStream, cipher);
				}
				catch(NoSuchPaddingException | NoSuchAlgorithmException | UnsupportedEncodingException | NoSuchProviderException | InvalidKeyException | InvalidAlgorithmParameterException | InvalidKeySpecException e)
				{
					e.printStackTrace();
				}
			}
			Files.copy(inputStream, path);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
}
