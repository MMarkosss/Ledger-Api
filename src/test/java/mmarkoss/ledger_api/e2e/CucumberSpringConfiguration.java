package mmarkoss.ledger_api.e2e;

import io.cucumber.spring.CucumberContextConfiguration;
import mmarkoss.ledger_api.TestcontainersConfiguration;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
public class CucumberSpringConfiguration {
}
