package test.scenario;

import org.junit.runner.Description;

import java.nio.file.Path;

/**
 * Represents a single scenario step.
 */
public final class Step {
	private final Description description;
	private final Path file;

	public Step(Description description, Path file) {
		this.description = description;
		this.file = file;
	}

	public Description getDescription() {
		return description;
	}

	public Path getFile() {
		return file;
	}
}
