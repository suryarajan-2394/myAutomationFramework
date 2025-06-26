package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class utility {
	static WebDriver driver;
	static Properties properties;
	static InputStream input;
	static WebElement element;

	public static Properties loadProperties(String path) {
		try {
			input = new FileInputStream(path);
			properties = new Properties();
			properties.load(input);
			return properties;

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return properties;
	}

	public static String getScreenshot(WebDriver driver) throws IOException {
		String screenshotFilePath;
		screenshotFilePath = System.getProperty("user.dir") + "//AutomationReports//" + utility.timeStamp() + ".png";
		File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
		FileUtils.copyFile(screenshotFile, new File(screenshotFilePath));
		return screenshotFilePath;
	}

	public static String timeStamp() {
		Instant instant = Instant.now();
		return instant.toString().replace("-", "_").replace(":", "_").replace(".", "_");

	}
	

}
