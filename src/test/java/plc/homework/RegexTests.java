package plc.homework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Contains JUnit tests for {@link Regex}. A framework of the test structure 
 * is provided, you will fill in the remaining pieces.
 *
 * To run tests, either click the run icon on the left margin, which can be used
 * to run all tests or only a specific test. You should make sure your tests are
 * run through IntelliJ (File > Settings > Build, Execution, Deployment > Build
 * Tools > Gradle > Run tests using <em>IntelliJ IDEA</em>). This ensures the
 * name and inputs for the tests are displayed correctly in the run window.
 */
public class RegexTests {

    /**
     * This is a parameterized test for the {@link Regex#EMAIL} regex. The
     * {@link ParameterizedTest} annotation defines this method as a
     * parameterized test, and {@link MethodSource} tells JUnit to look for the
     * static method {@link #testEmailRegex()}.
     *
     * For personal preference, I include a test name as the first parameter
     * which describes what that test should be testing - this is visible in
     * IntelliJ when running the tests (see above note if not working).
     */
    @ParameterizedTest
    @MethodSource
    public void testEmailRegex(String test, String input, boolean success) {
        test(input, Regex.EMAIL, success);
    }

    /**
     * This is the factory method providing test cases for the parameterized
     * test above - note that it is static, takes no arguments, and has the same
     * name as the test. The {@link Arguments} object contains the arguments for
     * each test to be passed to the function above.
     */
    public static Stream<Arguments> testEmailRegex() {
        return Stream.of(
                Arguments.of("Alphanumeric", "thelegend27@gmail.com", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("Underscore", "bobby_Shmerda@HoTmail.org", true),
                Arguments.of("Period in Username", "gotmilk.@bingsucks.cat", true),
                Arguments.of("Dash on Middle Section Domain", "hercomesthe@bing.bo-om.pow", true),
                Arguments.of("Many More Domain Names", "dunno@bing.bang.boom.pow", true),
                Arguments.of("Number in Domain", "numero@6esmyfav0rit0kachow.net", true),

                Arguments.of("Missing Domain Dot", "missingdot@gmailcom", false),
                Arguments.of("Symbols", "symbols#$%@gmail.com", false),
                Arguments.of("Not @ Symbol", "money$printer.fax", false),
                Arguments.of("Newline", "cream\ncheese@bread.yee", false),
                Arguments.of("More than 3 Ending", "ringworms@arereally.baaad", false),
                Arguments.of("Number in Domain Extension", "IneedSleep@mybed.911", false),
                Arguments.of("Symbols in Domain", "mybad@I'mkidding.sry", false),
                Arguments.of("Symbols in Domain Extension", "justThinking@smarts.w/l", false)


        );
    }

    @ParameterizedTest
    @MethodSource
    public void testOddStringsRegex(String test, String input, boolean success) {
        test(input, Regex.ODD_STRINGS, success);
    }

    public static Stream<Arguments> testOddStringsRegex() {
        return Stream.of(
                // what have eleven letters and starts with gas?
                Arguments.of("11 Characters", "automobiles", true),
                Arguments.of("13 Characters", "i<3pancakes13", true),
                Arguments.of("Symbols", "!@#$%^&*()<", true),
                Arguments.of("Line Break", "un\nli\nneb\nrea\nker", true),
                Arguments.of("spaces", "i < 3pa n c ak es 13", true),
                Arguments.of("Consecutive White Space", "a   utom\n\nobil   es", true),

                Arguments.of("5 Characters", "5five", false),
                Arguments.of("14 Characters", "i<3pancakes14!", false),
                Arguments.of("Only spaces", "               ", false),
                Arguments.of("21 Characters", "morethantheallowedcha", false),
                Arguments.of("No characters", "", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testCharacterListRegex(String test, String input, boolean success) {
        test(input, Regex.CHARACTER_LIST, success);
    }

    public static Stream<Arguments> testCharacterListRegex() {
        return Stream.of(
                Arguments.of("Single Element", "['a']", true),
                Arguments.of("Multiple Elements", "['a','b','c']", true),
                Arguments.of("Escapes", "['\u000B', '\n']", true),
                Arguments.of("Empty Brackets", "[]", true),
                Arguments.of("Spaces Inbetween", "['a', 'b', 'c']", true),

                Arguments.of("Missing Brackets", "'a','b','c'", false),
                Arguments.of("Missing Commas", "['a' 'b' 'c']", false),
                Arguments.of("Empty and Newline", "['', '', '\n']", false),
                Arguments.of("Missing Single Quotes", "['a',b','c']", false),
                Arguments.of("Spaces", "[' ',b','c']", false),
                Arguments.of("Mutiple Character Single Quotes", "['afr','b','c']", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testDecimalRegex(String test, String input, boolean success) {
        //throw new UnsupportedOperationException(); //TODO
        test(input, Regex.DECIMAL, success);
    }

    public static Stream<Arguments> testDecimalRegex() {
        //throw new UnsupportedOperationException(); //TODO
        return Stream.of(
                Arguments.of("Multiple Digit Decimal", "10100.001", true),
                Arguments.of("Negative Decimal", "-1.0", true),
                Arguments.of("Leading Zeros", "1.2000000", true),
                Arguments.of("Initial Zero", "0.3", true),
                Arguments.of("Zero", "0.0", true),

                Arguments.of("Integer Only", "1", false),
                Arguments.of("Leading Decimal", ".5", false),
                Arguments.of("Nothing After Decimal", "2.", false),
                Arguments.of("Double Decimal", "3..0", false),
                Arguments.of("Only Decimal", ".", false),
                Arguments.of("Double Negative", "--3.0", false),
                Arguments.of("Letters", "abe.kie", false),
                Arguments.of("Symbols", "*#.#*", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testStringRegex(String test, String input, boolean success) {
        //throw new UnsupportedOperationException(); //TODO
        test(input, Regex.STRING, success);
    }

    public static Stream<Arguments> testStringRegex() {
        //throw new UnsupportedOperationException(); //TODO
        return Stream.of(
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Hello World!", "\"Hello, World!\"", true),
                Arguments.of("1\\t2", "\"1\\t2\"", true),
                Arguments.of("Numbers", "\"123456\"", true),
                Arguments.of("NewLine", "\"23\n23\"", true),

                Arguments.of("Invalid", "\"unterminated", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false),
                Arguments.of("Blank", "", false),
                Arguments.of("Weird Quotes", "\"'''\"", false),
                Arguments.of("Single Quotes on Outside", "\'good\'", false)
        );
    }

    /**
     * Asserts that the input matches the given pattern. This method doesn't do
     * much now, but you will see this concept in future assignments.
     */
    private static void test(String input, Pattern pattern, boolean success) {
        Assertions.assertEquals(success, pattern.matcher(input).matches());
    }

}
