package org.omt.labelmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.HiddenHttpMethodFilter;

@SpringBootApplication
public class LabelManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(LabelManagerApplication.class, args);
	}

    @Bean
    public HiddenHttpMethodFilter hiddenHttpMethodFilter() {
        return new HiddenHttpMethodFilter();
    }

}
