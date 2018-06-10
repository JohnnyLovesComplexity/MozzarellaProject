package org.jlc.arar.mozzarella.pizza;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.regex.Pattern;

public class MimeTypeManager {

	@Nullable
	public static String parse(@NotNull String filename) {
		String[] parts = filename.split(Pattern.quote("."));
		StringBuilder suffix = new StringBuilder();
		
		if (parts.length <= 0)
			return null;
		else if (parts.length == 1)
			suffix = new StringBuilder(filename);
		else
			for (int i = 1; i < parts.length; i++)
				suffix.append(parts[i]);

		switch (suffix.toString())
		{
			case "txt":
				return "text/txt";
			case "html":
			case "htm":
				return "text/html";
			case "jpeg":
				return "image/jpeg";
			case "jpg":
				return "image/jpg";
			case "png":
				return "image/png";
			default:
				return null;
		}
	}
	@Nullable
	public static String parse(@NotNull File f) {
		return parse(f.toString());
	}
}
