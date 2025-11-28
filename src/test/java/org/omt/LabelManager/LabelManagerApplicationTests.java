package org.omt.LabelManager;

import org.junit.jupiter.api.Test;
import org.omt.LabelManager.label.Label;
import org.omt.LabelManager.label.LabelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class LabelManagerApplicationTests {

    @LocalServerPort
    int port;

    @Autowired
    private RestTestClient restClient;

    @Autowired
    LabelRepository repo;

	@Test
	void contextLoads() {
	}

    @Test
    void labelsPageLoads() {
        restClient.get()
                .uri("http://localhost:" + port + "/labels")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(String.class)
                .consumeWith(response ->
                        assertThat(response.getResponseBody()).contains("Your Labels")
                );
    }

    @Test
    void deleteLabel_RemovesItFromDatabase() {
        // Arrange: insert a row
        var label = new Label("WronglyNamedLabel");
        repo.save(label);

        // Act: call DELETE
        restClient
                .post().uri("/labels/" + label.getId() +"/delete")
                .exchange()
                .expectStatus().is3xxRedirection();

        // Assert: database no longer contains it
        assertThat(repo.findAll().stream().map(Label::getName).toList())
                .doesNotContain("WronglyNamedLabel");
    }
}
