/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.cloud.dataflow.server.config.features;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.dataflow.server.repository.RdbmsStandaloneDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.StandaloneDefinitionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Donovan Muller
 */
@Configuration
// If the application group feature is enabled then the standalone feature is automatically enabled by default
// as the two are seen as complimentary
@ConditionalOnExpression("#{"
			+ "'${" + FeaturesProperties.FEATURES_PREFIX + "." + FeaturesProperties.STANDALONE_ENABLED
			+ ":true}'.equalsIgnoreCase('true') "
			+ "|| "
			+ "'${" + FeaturesProperties.FEATURES_PREFIX + "." + FeaturesProperties.APPLICATION_GROUPS_ENABLED
			+ ":true}'.equalsIgnoreCase('true')" +
		"}")
public class StandaloneConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public StandaloneDefinitionRepository standaloneDefinitionRepository(DataSource dataSource) {
		return new RdbmsStandaloneDefinitionRepository(dataSource);
	}
}
