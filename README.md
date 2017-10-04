# Shark

Upload your data with confidence and access it with ease.

## What is Shark?

Shark is a Java Console Application you can use to create Shark Repositories.

A Shark Repository is hosted data, encrypted by a shared secret which is not stored on the server.

### About the Encryption

A lot of things claim to be encrypted, but you rarely see the specs, and you almost never see the source - here you see both.

The raw data is encrypted using AES. The AES key sits on the server and can be downloaded by everyone, however, it is encrypted using RSA, so you need to have the public key to retreive the AES key.

The file names are hashed using MD5. Because the folder and file names are not clear, it is also not clear what content you are storing.

## Getting Started

1. Download [the latest release](https://github.com/timmyrs/Shark/releases) of Shark (`shark.jar`).

2. Try to start it using `java -jar shark.jar`.

	1. If you get an error somewhat related to `MajorMinorVersion`, you need to install [Java 9](http://www.oracle.com/technetwork/java/javase/downloads/jre9-downloads-3848532.html).

### Browse a Repository

Shark Repositories are mostly just binary files, so pretty much everything should be capable of hosting one, so a Shark Repository might be available via HTTP, like the example we will use throughout this guide (`https://timmyrs.de/shark-repository/`).

First of all we will _connect_ to the Shark Repository as follows:

	java -jar shark.jar connect https://timmyrs.de/shark-repository/

#### File Hierarchy

In order to see all files in a repository, we just input `ls`, `list` or `tree` in the console.

	$ ls

Resulting in a response like this:

	https://timmyrs.de/shark-repository
	  shark-repository-public.txt
	  aes-key.bin
	  shark-repository/
	  	actual-content.txt

As you might be able to tell, the actual content is in a subdirectory which has the same name as the Shark Repository, which brings us to...

#### Downloading

Downloading is pretty simple. You can just input `dl` or `download` in the console, followed by the path of a file or directory. All downloads will be stored in the current working directory.

Since we know that the actual content is in a folder called `shark-repository`, we can easially download everything as follows:

	$ dl shark-repository

...but if we only care about the `actual-content.txt`, we can download it as follows:

	$ dl shark-repository/actual-content.txt

### Creating a Repository

Creating a new Shark Repository, is as simple as 1, 2, 3:

1. **Create a directory.** `mkdir shark-repository`. In this case the Shark Repository is called `shark-repository`.

2. Fill your Shark Repository with data. **Note:** Avoid duplicate file name, because Shark saves everything as `.bin`, so `hi.txt` would overwrite `hi.csv`!

3. **Encrypt it.** `java -jar shark.jar build shark-repository`. This will create the `shark-repository-built` folderm which you can upload to any hosterm and the `shark-repository-public.txt` which has to be present in the working directory for you to be able to access the data.

Let's say you uploaded it on your webserver which runs under timmyrs.de and you've renamed `shark-repository-built` to `shark-repository` for simplicity, then you could give everyone you want to access that data the URL (`https://timmyrs.de/shark-repository/`) and the `shark-repository-public.txt`.

**Nerd Bonus:** As you might've noticed, you could `ls` and `dl` from `https://timmyrs.de/shark-repository/` without having the `shark-reposity-public.txt`. This is because the `built` makes the public key a shared file by default, but you can change that in the `6a992d5529f459a44fee58c733255e86.bin` (`index.json`).

## Questions? Ideas? Bugs?!

Shark is still very new, so if there is anything on your mind, don't hesitate to [create an issue](https://github.com/timmyrs/Shark/issues)! :D
