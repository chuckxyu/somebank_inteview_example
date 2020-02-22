import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;

import com.test.nubank.Authorizer;

public class AuthorizerTest {

	public AuthorizerTest() {
	}

	public static void main(String[] args) throws Exception {
		JUnitCore junit = new JUnitCore();
		junit.addListener(new TextListener(System.out));
		junit.run(Authorizer.class);
	}

}
