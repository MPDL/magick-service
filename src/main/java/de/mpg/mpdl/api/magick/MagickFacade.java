package de.mpg.mpdl.api.magick;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Facade implementing the calling imagemagick
 * 
 * @author saquet
 *
 */
public class MagickFacade {
	private static String CONVERT_CMD = "convert";
	private Runtime runtime;
	private static final String DEFAULT_FORMAT = "png";
	private static final String COMMAMD_SEPARATOR = "XXX_SEPARATOR_XXX";
	private static Logger logger = LoggerFactory.getLogger(MagickFacade.class);

	/**
	 * DEfine what operation is done first: Resize (Default), or crop
	 * 
	 * @author saquet
	 *
	 */
	public enum Priority {
		CROP, RESIZE;

		/**
		 * Return a non null Value. If value is not valid, return default value
		 * (resize)
		 * 
		 * @param value
		 * @return
		 */
		public static Priority nonNullValueOf(String value) {
			try {
				return valueOf(value.toUpperCase());
			} catch (Exception e) {
				return RESIZE;
			}
		}
	}

	/**
	 * Constructor: Initialize the runtime and the configuration
	 */
	public MagickFacade() {
		runtime = Runtime.getRuntime();
		MagickConfiguration config = new MagickConfiguration();
		if (config.getImageMagickConvertBin() != null)
			CONVERT_CMD = config.getImageMagickConvertBin();
	}

	/**
	 * Do convert from an inputstream to an outputstream
	 * 
	 * @param in
	 * @param out
	 * @param name
	 * @param format
	 * @param size
	 * @param params
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void convert(InputStream in, OutputStream out, String name,
			String format, String size, String crop, Priority priority,
			String params1, String params2) throws IOException,
			InterruptedException {
		File temp = File.createTempFile("magick",
				"".equals(FilenameUtils.getExtension(name)) ? ".tmp" : "."
						+ FilenameUtils.getExtension(name));
		IOUtils.copy(in, new FileOutputStream(temp));
		IOUtils.copy(
				new FileInputStream(convert(temp, format, size, crop, priority,
						params1, params2)), out);
	}

	/**
	 * Convert a File to the specified format with the specified size
	 * 
	 * @param input
	 * @param format
	 * @param size
	 * @param params
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public File convert(File input, String format, String size, String crop,
			Priority priority, String params1, String params2)
			throws IOException, InterruptedException {
		File output = createOutputFile(format);
		String cmd = generateCommand(input, output, format, size, crop,
				priority, params1, params2);
		Process p = runtime.exec(
				cmd.replace(COMMAMD_SEPARATOR, " "));
		int res = p.waitFor();
		if (res != 0) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			IOUtils.copy(p.getErrorStream(), out);
			logger.error(out.toString());
		}
		return output;
	}

	/**
	 * Create the file where the result of the transformation will be written
	 * 
	 * @param format
	 * @return
	 * @throws IOException
	 */
	private File createOutputFile(String format) throws IOException {
		if (format == null || "".equals(format))
			format = DEFAULT_FORMAT;
		return File.createTempFile("magick_result", "." + format);
	}

	/**
	 * Generate an Imagemagick command Line following the template:<br/>
	 * convert -define jpeg:size=SIZE PARAMS1 INPUT PARAMS2 OUTPUT
	 * 
	 * @param input
	 * @param output
	 * @param format
	 * @param size
	 * @param cmd
	 * @return
	 */
	private String generateCommand(File input, File output, String format,
			String size, String crop, Priority priority, String params1,
			String params2) {

		params1 = params1 != null ? params1 : "";
		params2 = params2 != null ? params2 : "";
		String resizeAndCrop = priority == Priority.RESIZE ? getSizeAsParam(size)
				+ COMMAMD_SEPARATOR + getCropAsParam(crop)
				: getCropAsParam(crop) + " " + getSizeAsParam(size);
		return (CONVERT_CMD + COMMAMD_SEPARATOR + params1 + COMMAMD_SEPARATOR
				+ input.getAbsolutePath() + COMMAMD_SEPARATOR + resizeAndCrop
				+ COMMAMD_SEPARATOR + params2 + COMMAMD_SEPARATOR + output
					.getAbsolutePath()).trim();
	}

	/**
	 * Return the size param as following: -define jpeg:size=SIZE
	 * 
	 * @param size
	 * @return
	 */
	private String getSizeAsParam(String size) {
		if (size == null || "".equals(size))
			return "";
		return "-resize" + COMMAMD_SEPARATOR + size;
	}

	private String getCropAsParam(String crop) {
		if (crop == null || "".equals(crop))
			return "";
		return "-crop" + COMMAMD_SEPARATOR + crop;
	}
}
