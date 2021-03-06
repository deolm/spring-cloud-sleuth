/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.sleuth.instrument.web.issues.issue971;

import brave.Tracer;
import brave.sampler.Sampler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.sleuth.DisableSecurity;
import org.springframework.cloud.sleuth.util.ArrayListSpanReporter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.BDDAssertions.then;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = DemoSleuthSkipApplicationTests.Config.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"management.endpoints.web.exposure.include:*",
		"server.servlet.context-path:/context-path",
		"spring.sleuth.http.legacy.enabled:true" })
public class DemoSleuthSkipApplicationTests {

	@Autowired
	ArrayListSpanReporter accumulator;

	@Autowired
	Tracer tracer;

	@LocalServerPort
	int port;

	@Test
	public void should_not_sample_skipped_endpoint_with_context_path() {
		new RestTemplate().getForObject(
				"http://localhost:" + this.port + "/context-path/actuator/health",
				String.class);

		then(this.tracer.currentSpan()).isNull();
		then(this.accumulator.getSpans()).hasSize(0);
	}

	@Test
	public void should_sample_non_actuator_endpoint_with_context_path() {
		new RestTemplate().getForObject(
				"http://localhost:" + this.port + "/context-path/something",
				String.class);

		then(this.tracer.currentSpan()).isNull();
		then(this.accumulator.getSpans()).hasSize(1);
	}

	@EnableAutoConfiguration(exclude = RabbitAutoConfiguration.class)
	@Configuration
	@DisableSecurity
	@RestController
	public static class Config {

		@GetMapping("something")
		void doNothing() {
		}

		@Bean
		ArrayListSpanReporter reporter() {
			return new ArrayListSpanReporter();
		}

		@Bean
		Sampler sampler() {
			return Sampler.ALWAYS_SAMPLE;
		}

	}

}