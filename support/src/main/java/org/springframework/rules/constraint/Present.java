/*
 * Copyright 2002-2006 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.rules.constraint;

import org.springframework.core.closure.Constraint;

/**
 * A simple constraint to separate required and present semantics
 * 
 * @author Mathias Broekelmann
 * 
 */
public class Present extends Required {
	private static final Present instance = new Present();

	public String toString() {
		return "present";
	}

	public static Constraint instance() {
		return instance;
	}
}