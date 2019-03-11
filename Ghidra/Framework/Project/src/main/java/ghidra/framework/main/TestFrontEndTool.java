/* ###
 * IP: GHIDRA
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
package ghidra.framework.main;

import ghidra.framework.model.ProjectManager;

/**
 * A test version of the {@link FrontEndTool} that disables some functionality
 */
public class TestFrontEndTool extends FrontEndTool {

	public TestFrontEndTool(ProjectManager pm) {
		super(pm);
	}

	@Override
	protected void doExit() {
		// don't call super--it calls System.exit()
	}

	@Override
	public void close() {
		setVisible(false);
	}
}
