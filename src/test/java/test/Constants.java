package test;

import java.nio.file.Path;
import java.nio.file.Paths;

public interface Constants {

	/**
	 * The directory containing the test scenarios.
	 */
	public static final Path SCENARIO_DIRECTORY = Paths.get("src", "test", "resources");

	/**
	 * Time to wait (in milliseconds) after starting a new component.
	 */
	public static final int WAIT_FOR_COMPONENT_STARTUP = 2000;

}
