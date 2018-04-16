/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.cache.query.internal.parse;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.geode.test.junit.categories.UnitTest;

/**
 * TODO: class created to fix GEODE-3938. Add more tests for other queries, parameters, etc.
 */
@Category(UnitTest.class)
public class OQLParserTest {

  @Test
  public void testToDatePresetQueryFunction() throws Exception {
    String oqlSource =
        "SELECT * FROM /Portfolios WHERE createDate >= to_date('01/01/2000', 'MM/dd/yyyy')";
    OQLLexer lexer = new OQLLexer(new StringReader(oqlSource));
    OQLParser parser = new OQLParser(lexer);

    parser.queryProgram();
    assertThat(parser.getAST()).isNotNull();
  }

  @Test
  public void testToDatePresetQueryFunctionWithQueryParameter() throws Exception {
    String oqlSource = "SELECT * FROM /Portfolios WHERE createDate >= to_date($1, 'MM/dd/yyyy')";
    OQLLexer lexer = new OQLLexer(new StringReader(oqlSource));
    OQLParser parser = new OQLParser(lexer);

    parser.queryProgram();
    assertThat(parser.getAST()).isNotNull();
  }
}
