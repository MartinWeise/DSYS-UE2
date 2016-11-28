package test.scenario;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.sort;

/**
 * Provides utility methods for test scenarios.
 * <p/>
 * A <i>scenario</i> is a directory that contains any number of <i>steps</i>.
 *
 * A <i>step</i> is a file containing any number of instructions that are executed by the {@link test.ScenarioTest}.
 * The file name has to match the following regular expression {@code \d+_.*} (for example {@code 00_login.txt} or
 * {@code 17_shutdown.txt}).
 */
public final class ScenarioUtils {

	private ScenarioUtils() {
	}

	/**
	 * Lists all scenarios located in the given directory path.<br/>
	 * Note that any readable directory is considered to be a scenario.
	 * In other words, it has to contain one or more <i>scenario steps</i>.
	 *
	 * @param path the directory containing the scenarios
	 * @return all scenario directories within the given path sorted in lexicographical order
	 * @see #listSteps(Path)
	 */
	public static Iterable<Path> listScenarios(Path path) {
		if (!Files.isDirectory(path)) {
			throw new IllegalArgumentException(String.format("Path '%s' is not a directory.",
					path.toAbsolutePath()));
		}
		File[] list = path.toFile().listFiles(DIRECTORY_FILTER);
		if (list == null || list.length == 0) {
			throw new IllegalStateException("No test scenarios found in directory: " + path.toAbsolutePath());
		}
		sort(list);

		list = new File[]{path.resolve("scenario").toFile()};
		return toPaths(list);
	}

	/**
	 * Lists all scenario steps within the given directory path.
	 *
	 * @param scenarioDirectory the directory to scan
	 * @return all steps sorted in lexicographical order
	 */
	public static Iterable<Path> listSteps(Path scenarioDirectory) {
		if (!Files.isDirectory(scenarioDirectory)) {
			throw new IllegalArgumentException(String.format("Path '%s' is not a directory.",
					scenarioDirectory.toAbsolutePath()));
		}
		File[] list = scenarioDirectory.toFile().listFiles(STEP_FILTER);
		if (list == null || list.length == 0) {
			throw new IllegalStateException(String.format("No test steps found in directory: '%s'",
					scenarioDirectory.toAbsolutePath()));
		}
		sort(list);
		return toPaths(list);
	}

	/**
	 * Converts the given files to a sequence of paths.
	 *
	 * @param files the files
	 * @return the paths
	 */
	private static Iterable<Path> toPaths(File... files) {
		List<Path> paths = new ArrayList<>(files.length);
		for (File file : files) {
			paths.add(file.toPath());
		}
		return paths;
	}

	private static final FileFilter DIRECTORY_FILTER = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return pathname.isDirectory() && pathname.canRead();
		}
	};

	private static final FileFilter STEP_FILTER = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return pathname.isFile() && pathname.canRead() && pathname.getName().matches("\\d+_.*");
		}
	};
}
