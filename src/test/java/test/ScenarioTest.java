package test;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import test.scenario.ScenarioRunner;
import test.scenario.ScenarioUtils;
import test.scenario.Step;
import test.util.Flag;
import test.util.PatternMatcher;
import util.TestInputStream;
import util.TestOutputStream;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.assertThat;
import static test.scenario.ScenarioUtils.listSteps;
import static util.TestUtils.*;

/**
 * Executes the test scenarios.
 */
@RunWith(ScenarioRunner.class)
public class ScenarioTest {

	static SpelExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
	StandardEvaluationContext ctx;

	static final ComponentFactory factory = new ComponentFactory();
	static final Map<String, CliComponent> componentMap = new HashMap<>();
	static CliComponent component;

	@Before
	public void setUp() {
		ctx = new StandardEvaluationContext(this);
	}

	@Test
	public void test() throws Throwable {

		ScenarioRunner scenarioRunner = ScenarioRunner.getInstance();
		if (scenarioRunner == null) {
			for (Path scenarioFile : ScenarioUtils.listScenarios(Constants.SCENARIO_DIRECTORY)) {
				for (Path stepFile : listSteps(scenarioFile)) {
					runStep(stepFile);
				}
			}
			return;
		}

		Queue<Step> steps = scenarioRunner.getSteps();
		Step step = steps.poll();
		runStep(step.getFile());
		System.out.println(repeat('#', 80));
	}

	void runStep(Path stepFile) throws Throwable {
		List<String> lines = Files.readAllLines(stepFile, Charset.defaultCharset());
		System.out.println(stepFile.getFileName().toString());
		for (String line : lines) {
			/*
			 * Comment
			 */
			if (line == null || line.isEmpty() || line.startsWith("#")) {
				// Intentionally do nothing
			}
			/*
			 * Component
			 */
			else if (line.startsWith("*")) {
				String[] parts = line.split(":?\\s+", 3);
				String instruction = "create" + parts[1];
				String componentName = parts[2];

				Method method = factory.getClass().getMethod(instruction, String.class, TestInputStream.class, TestOutputStream.class);
				if (method == null) {
					throw new IllegalArgumentException(String.format("Method '%s' not found.", instruction));
				}

				TestInputStream in = new TestInputStream();
				TestOutputStream out = new TestOutputStream(System.out);
				Object component = method.invoke(factory, componentName, in, out);
				CliComponent cliComponent = new CliComponent(component, in, out);
				componentMap.put(componentName, cliComponent);
				if (component instanceof Runnable) {
					new Thread((Runnable) component).start();
				}
				Thread.sleep(Constants.WAIT_FOR_COMPONENT_STARTUP);
			}
			/*
			 * SpEL expression
			 */
			else if (line.startsWith(">")) {
				try {
					parser.parseExpression((line.substring(1).trim())).getValue(ctx);
				} catch (EvaluationException e) {
					Throwable cause = e;
					while (cause.getCause() != null) {
						cause = cause.getCause();
					}
					if (cause instanceof AssertionError) {
						System.err.println(cause.getClass().getSimpleName() + ": " + cause.getMessage());
					} else {
						throw cause;
					}
				}
			}
			/*
			 * CLI
			 */
			else {
				String[] parts = line.split(":?\\s+", 2);
				component = componentMap.get(parts[0]);
				if (component == null) {
					throw new IllegalStateException(String.format(
							"Cannot find component '%s'. Please start it before using it.", parts[0]));
				}
				component.in.addLine(parts[1].trim());
				Thread.sleep(500);
			}
		}
	}

	/**
	 * Verifies that the output of the recently used component matches a certain condition.
	 * <p/>
	 * The data written to the {@link PrintStream} of component is compared against a {@link Matcher} built by the
	 * expected String and {@link Flag}s. If it does not satisfy the condition, an {@link AssertionError} is thrown
	 * with the reason and information about the matcher and failing value.
	 *
	 * @param expected the condition
	 * @param flags    the flags defining the type of the matcher.
	 */
	public void verify(String expected, Flag... flags) {
		List<String> lines = component.out.reset();
		while (lines.isEmpty()) {
			lines = component.out.reset();
		}
		if (contains(Flag.LAST, (Object[]) flags)) {
			lines = Collections.singletonList(lines.isEmpty() ? "" : lines.get(lines.size() - 1));
		}
		String actual = join("\n", lines);

		Matcher<String> matcher;
		if (contains(Flag.REGEX, (Object[]) flags)) {
			matcher = new PatternMatcher(expected);
		} else {
			matcher = CoreMatchers.containsString(expected.toLowerCase());
			actual = actual.toLowerCase();
		}

		String msg = String.format("String must %s%s '%s' but was:%s",
				contains(Flag.NOT, (Object[]) flags) ? "NOT " : "",
				contains(Flag.REGEX, (Object[]) flags) ? "match pattern" : "contain",
				expected,
				lines.size() > 1 ? "\n" + actual : String.format(" '%s'", actual));

		matcher = contains(Flag.NOT, (Object[]) flags) ? CoreMatchers.not(matcher) : matcher;

		assertThat(msg, actual, matcher);
	}

	/**
	 * Represents a single component and its input and output streams.
	 */
	static class CliComponent {
		Object component;
		TestInputStream in;
		TestOutputStream out;

		public CliComponent(Object component, TestInputStream in, TestOutputStream out) {
			this.component = component;
			this.out = out;
			this.in = in;
		}
	}
}
