package shark.Shark.files;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import shark.Shark.SharkException;

import java.util.ArrayList;

public class SharkDirectory extends SharkFile
{
	private SharkFile index;
	private ArrayList<SharkFile> children;

	SharkDirectory(String name, String path, String localpath, String use, boolean shared)
	{
		super(name, "/dir", path, localpath, "", shared);
		index = SharkFile.construct("index", "json", this.path + this.getRemoteName() + "/", this.localpath + this.getLocalName() + "/", use);
	}

	public ArrayList<SharkFile> getChildren() throws SharkException
	{
		if(children == null)
		{
			children = new ArrayList<>();
			JsonArray rawFiles = new JsonParser().parse(index.getPlainContent()).getAsJsonArray();
			for(JsonElement rawFileElement : rawFiles)
			{
				JsonObject rawFile = (JsonObject) rawFileElement;
				children.add(SharkFile.construct(
						rawFile.get("name").getAsString(),
						rawFile.get("type").getAsString(),
						this.path + this.getRemoteName() + "/",
						this.localpath + this.getLocalName() + "/",
						(rawFile.has("use") ? rawFile.get("use").getAsString() : "")
				));
			}
		}
		return children;
	}

	public SharkFile resolveRelativePath(String path) throws SharkException
	{
		String[] pathArr = path.split("/");
		if(pathArr.length == 0 || pathArr[0].equals(""))
		{
			return this;
		}
		for(SharkFile file : this.getChildren())
		{
			if(file.name.equals(pathArr[0]) || file.getLocalName().equals(pathArr[0]))
			{
				if(pathArr.length > 1 && !pathArr[1].equals(""))
				{
					if(file instanceof SharkDirectory)
					{
						return ((SharkDirectory) file).resolveRelativePath(path.substring(pathArr[0].length() + 1));
					}
					else
					{
						throw new RuntimeException(file.getLocalName() + " is not a directory.");
					}
				}
				else
				{
					return file;
				}
			}
		}
		return null;
	}
}
