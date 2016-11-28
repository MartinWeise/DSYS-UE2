package test.scenario;

import org.junit.internal.runners.statements.InvokeMethod;
import org.junit.runner.Description;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import test.Constants;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

public class ScenarioRunner extends BlockJUnit4ClassRunner {
	private static final char EXTENSION_SEPARATOR = '.';
	private static ScenarioRunner instance;

	private Description description;
	private Queue<Step> steps = new ArrayDeque<>();

	public static ScenarioRunner getInstance() {
		return instance;
	}

	public ScenarioRunner(Class<?> testClass) throws InitializationError {
		super(testClass);
		instance = this;
	}

	@Override
	protected Object createTest() throws Exception {
		return getTestClass().getJavaClass().getDeclaredConstructor().newInstance();
	}

	@Override
	protected Statement methodInvoker(final FrameworkMethod method, final Object test) {
		return new InvokeMethod(method, test) {
			@Override
			public void evaluate() throws Throwable {
				try {
					method.invokeExplosively(test);
				} catch (Throwable t) {
					// Print the stack trace in order to give an indication about the cause
					t.printStackTrace();
				}
			}
		};
	}

	@Override
	public Description getDescription() {
		if (description == null) {
			description = Description.createSuiteDescription(getName(), getRunnerAnnotations());
			Iterable<Path> scenarios = ScenarioUtils.listScenarios(Constants.SCENARIO_DIRECTORY);
			for (Path dir : scenarios) {
				addScenario(dir);
			}
		}
		return description;
	}

	@Override
	protected Description describeChild(FrameworkMethod method) {
		return getSteps().peek().getDescription();
	}

	@Override
	protected List<FrameworkMethod> getChildren() {
		return Collections.nCopies(steps.size(), super.getChildren().get(0));
	}

	/**
	 * Registers the scenario in the given directory with all its steps.
	 *
	 * @param scenarioDir the directory containing the step files
	 */
	private void addScenario(Path scenarioDir) {
		for (Path path : ScenarioUtils.listSteps(scenarioDir)) {
			Description stepDescription = Description.createTestDescription(getTestClass().getJavaClass().getName(),
					getBaseName(path.getFileName().toString()));
			description.addChild(stepDescription);
			steps.add(new Step(stepDescription, path));
		}
	}

	/**
	 * Returns the unqualified filename without extension.
	 *
	 * @param name the filename
	 * @return the base name
	 */
	private static String getBaseName(String name) {
		int index = name.indexOf(EXTENSION_SEPARATOR);
		return index < 0 ? name : name.substring(0, index);
	}

	public Queue<Step> getSteps() {
		return steps;
	}
}
