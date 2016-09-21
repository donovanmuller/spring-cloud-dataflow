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

package org.springframework.cloud.dataflow.server.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import javax.sql.DataSource;

import org.springframework.cloud.dataflow.core.StandaloneDefinition;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.Assert;

/**
 * RDBMS implementation of {@link StandaloneDefinitionRepository}.
 *
 */
public class RdbmsStandaloneDefinitionRepository extends AbstractRdbmsKeyValueRepository<StandaloneDefinition> implements StandaloneDefinitionRepository {

	public RdbmsStandaloneDefinitionRepository(DataSource dataSource) {
		super(dataSource, "STANDALONE_", "DEFINITIONS", new RowMapper<StandaloneDefinition>() {
			@Override
			public StandaloneDefinition mapRow(ResultSet resultSet, int i) throws SQLException {
				return new StandaloneDefinition(
						resultSet.getString("DEFINITION_NAME"), resultSet.getString("DEFINITION"));
			}
		}, "DEFINITION_NAME", "DEFINITION");
	}

	@Override
	public StandaloneDefinition save(StandaloneDefinition definition) {
		Assert.notNull(definition, "definition must not be null");
		if (exists(definition.getRegisteredAppName())) {
			throw new DuplicateStandaloneDefinitionException(
					String.format("Cannot create standalone application %s because another one has already " +
									"been created with the same name",
							definition.getName()));
		}
		Object[] insertParameters = new Object[]{definition.getRegisteredAppName(), definition.getDslText()};
		jdbcTemplate.update(saveRow, insertParameters, new int[]{Types.VARCHAR, Types.CLOB});
		return definition;
	}

	@Override
	public void delete(StandaloneDefinition definition) {
		Assert.notNull(definition, "definition must not null");
		delete(definition.getRegisteredAppName());
	}
}
