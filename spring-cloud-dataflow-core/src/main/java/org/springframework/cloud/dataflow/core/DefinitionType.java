/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.dataflow.core;

/**
 * Enumeration of definition types
 *
 */
public enum DefinitionType {

	/**
	 * A stream definition
	 */
	stream,

	/**
	 * A task definition
	 */
	task,

	/**
	 * A standalone definition
	 */
	standalone,

	/**
	 * An application type to process a Maven artifact containing
	 * all the applications and definitions needed for the stream, task or
	 * standalone applications.
	 */
	applicationGroup;

}
