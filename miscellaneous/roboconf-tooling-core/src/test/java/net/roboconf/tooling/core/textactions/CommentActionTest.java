/**
 * Copyright 2016-2017 Linagora, Université Joseph Fourier, Floralis
 *
 * The present code is developed in the scope of the joint LINAGORA -
 * Université Joseph Fourier - Floralis research program and is designated
 * as a "Result" pursuant to the terms and conditions of the LINAGORA
 * - Université Joseph Fourier - Floralis research program. Each copyright
 * holder of Results enumerated here above fully & independently holds complete
 * ownership of the complete Intellectual Property rights applicable to the whole
 * of said Results, and may freely exploit it in any manner which does not infringe
 * the moral rights of the other copyright holders.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.roboconf.tooling.core.textactions;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class CommentActionTest {

	@Test
	public void testBlockComment() throws Exception {

		File f = TestUtils.findTestFile( "/textactions/non-commented.txt" );
		String s = Utils.readFileContent( f );

		CommentAction action = new CommentAction();
		String result = action.update( s, 58, 51 );

		f = TestUtils.findTestFile( "/textactions/commented2.txt" );
		s = Utils.readFileContent( f );

		Assert.assertEquals( s, result );
	}


	@Test
	public void testBlockComment_lineIsPartiallySelected() throws Exception {

		File f = TestUtils.findTestFile( "/textactions/non-commented.txt" );
		String s = Utils.readFileContent( f );

		CommentAction action = new CommentAction();
		String result = action.update( s, 58, 50 );

		f = TestUtils.findTestFile( "/textactions/commented2.txt" );
		s = Utils.readFileContent( f );

		Assert.assertEquals( s, result );
	}


	@Test
	public void testPropertyComment() throws Exception {

		File f = TestUtils.findTestFile( "/textactions/non-commented.txt" );
		String s = Utils.readFileContent( f );

		CommentAction action = new CommentAction();
		String result = action.update( s, 140, 5 );

		f = TestUtils.findTestFile( "/textactions/commented1.txt" );
		s = Utils.readFileContent( f );

		Assert.assertEquals( s, result );
	}


	@Test
	public void testPartialBlockComment() throws Exception {

		CommentAction action = new CommentAction();
		String initial = "c {\n\tinstaller: ";

		String result = action.update( "#" + initial, 0, 1 );
		Assert.assertEquals( "#" + initial, result );

		result = action.update( initial, 0, 50 );
		Assert.assertEquals( "#c {\n#\tinstaller: ", result );

		result = action.update( initial, 0, 1 );
		Assert.assertEquals( "#c {\n\tinstaller: ", result );

		result = action.update( initial, 2, 1 );
		Assert.assertEquals( "#c {\n\tinstaller: ", result );

		result = action.update( initial, 3, 1 );
		Assert.assertEquals( "#c {\n#\tinstaller: ", result );

		result = action.update( initial, 4, 1 );
		Assert.assertEquals( "c {\n#\tinstaller: ", result );

		result = action.update( initial, 4, 7 );
		Assert.assertEquals( "c {\n#\tinstaller: ", result );
	}


	@Test
	public void testEmptyLine() {

		CommentAction action = new CommentAction();
		String result = action.update( "", 58, 51 );
		Assert.assertEquals( "", result );
	}
}
