package shark.Shark;

import shark.Shark.files.SharkDirectory;
import shark.Shark.files.SharkFile;

class SharkConsole
{
	static void executeCommand(String[] args)
	{
		switch(args[0].toLowerCase())
		{
			case "ls":
			case "list":
			case "tree":
				System.out.print(Main.REPOSITORY_URL);
				Main.REPOSITORY.recursiveList("  ");
				System.out.print("\n");
				break;

			case "cat":
				if(args.length != 2)
				{
					System.out.println("Syntax: cat <path>");
				}
				try
				{
					SharkFile file = Main.REPOSITORY.resolveRelativePath(args[1]);
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
					SharkFile file = Main.REPOSITORY.resolveRelativePath(args[1]);
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
					SharkFile file = Main.REPOSITORY.resolveRelativePath(args[1]);
					if(file == null)
					{
						System.out.println(args[1] + " doesn't exist.");
					}
					else
					{
						if(file instanceof SharkDirectory)
						{
							file.recursiveDownload("", "");
						}
						else
						{
							file.recursiveDownload("  ", file.localpath);
						}
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
				System.out.println("ls, list, tree        Recursively lists all files in the repository.");
				System.out.println("cat <path>            Shows the content of the file at the given path.");
				System.out.println("cat-bin <path>        Shows the binary data of the file at the given path.");
				System.out.println("dl, download <path>   Downloads the given file or folder into the current working directory.");
				System.out.println("exit, end, disconnect Ends the console session.");
		}
	}
}
