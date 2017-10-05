package de.timmyrs.Shark;

public class SharkException extends Exception
{
	public SharkException(String message)
	{
		super(message);
	}

	@Override
	public void printStackTrace()
	{
		this.printStackTrace("");
	}

	void printStackTrace(String prepend)
	{
		System.out.println(prepend + this.getMessage());
	}
}
