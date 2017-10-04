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

First of all, you need to get [the latest release](https://github.com/timmyrs/Shark/releases) of Shark.

Then you can try to start it using `java -jar shark.jar`. If you get an error somewhat related to `MajorMinorVersion`, that means you need to install Java 9.

### Browse a Repository

Shark Repositories are mostly just binary files, so pretty much everything should be capable of hosting one.

A Shark Repository might be available via HTTP (example: `http://example.com/shark-repository/`) so in order to list all files for that example URL you just run the following:

	java -jar shark.jar browse http://example.com/shark-repository/ --list

#### Downloading

If you want to download an entire repository, that's usually pretty simple, because all the contents are in a sub-folder, as `--list` will show us.

	http://example.com/shark-repository
	  shark-repository-public.txt
	  aes-key.bin
	  shark-repository/
	  	actual-content.txt

In this case the Shark Repository is called `shark-repository`.

If you want **to download everything**, you can run:

	java -jar shark.jar browse http://example.com/shark-repository/ --download-as-is shark-repository

which will create a shark-repository folder in the current working directory and then download & decrypt the contents.

If you want **to download a single file**, you can run:

	java -jar shark.jar browse http://example.com/shark-repository/ --download shark-repository/actual-content.txt

Which will just download the `actual-content.txt` into the current working directory.

**Pro Tip:** You can replace `--download` with `-dl`.

### Creating a Repository

Creating a new Shark Repository, is as simple as 1, 2, 3:

1. `mkdir shark-repository` Create a directory. In this case the Shark Repository is called `shark-repository`.

2. Fill your Shark Repository with data.

3. `java -jar shark.jar build shark-repository` To encrypt it and make it server-ready. This will create the `shark-repository-built` which you can upload to any hoster and the `shark-repository-public.txt` which grants access to the data.

Let's say you uploaded it on your webserver which runs on example.com and you've renamed the folder to `shark-repository` for simplicity, then you could give everyone you want to access that data the URL `http://example.com/shark-repository/` and the `shark-repository-public.txt`.

## Questions? Ideas? **Bugs?**

Shark is still very new, so if there is anything on your mind, don't hesitate to [create an issue](https://github.com/timmyrs/Shark/issues)! :D