package de.timmyrs.Shark;

import de.timmyrs.Shark.files.SharkDirectory;
import de.timmyrs.Shark.files.SharkFile;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.*;

public class UnitTests
{
	@Test
	public void browseTest()
	{
		Main.REPOSITORY_URL = "https://timmyrs.de/shark-repository";
		Main.REPOSITORY = (SharkDirectory) SharkFile.construct("", "/dir", "", "", "");
		Main.executeCommand("debug");
		assertTrue(Main.DEBUG);
		Main.executeCommand("debug");
		assertFalse(Main.DEBUG);
		Main.executeCommand("cd");
		assertEquals("", Main.USER_CD_PATH);
		Main.executeCommand("cd shark-repository");
		assertEquals("shark-repository/", Main.USER_CD_PATH);
		Main.executeCommand("cd /");
		assertEquals("", Main.USER_CD_PATH);
		Main.executeCommand("cd shark-repository");
		assertEquals("shark-repository/", Main.USER_CD_PATH);
		Main.executeCommand("cd ..");
		assertEquals("", Main.USER_CD_PATH);
		Main.executeCommand("ls");
		Main.executeCommand("tree");
		Main.executeCommand("cat shark-repository/actual-content.txt");
		Main.executeCommand("cat-bin shark-repository/actual-content.txt");
		Main.executeCommand("cd shark-repository");
		assertEquals("shark-repository/", Main.USER_CD_PATH);
		Main.executeCommand("cat actual-content.txt");
		Main.executeCommand("cat-bin actual-content.txt");
		File downloadFile = new File("actual-content.txt");
		if(downloadFile.exists())
		{
			assertTrue(downloadFile.delete());
		}
		Main.executeCommand("dl actual-content.txt");
		assertTrue(downloadFile.exists());
		if(downloadFile.exists())
		{
			assertTrue(downloadFile.delete());
		}
	}
}
