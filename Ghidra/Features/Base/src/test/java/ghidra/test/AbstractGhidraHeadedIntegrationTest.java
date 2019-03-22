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
package ghidra.test;

import java.awt.Window;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import docking.*;
import docking.action.DockingActionIf;
import docking.widgets.fieldpanel.FieldPanel;
import docking.widgets.fieldpanel.field.Field;
import docking.widgets.fieldpanel.listener.FieldMouseListener;
import docking.widgets.fieldpanel.support.FieldLocation;
import ghidra.GhidraTestApplicationLayout;
import ghidra.app.plugin.core.analysis.AutoAnalysisManager;
import ghidra.app.plugin.core.codebrowser.CodeBrowserPlugin;
import ghidra.framework.ApplicationConfiguration;
import ghidra.framework.GhidraApplicationConfiguration;
import ghidra.framework.model.*;
import ghidra.framework.plugintool.Plugin;
import ghidra.framework.plugintool.PluginTool;
import ghidra.framework.plugintool.util.PluginException;
import ghidra.program.model.listing.Program;
import ghidra.util.TaskUtilities;
import ghidra.util.exception.AssertException;
import junit.framework.AssertionFailedError;
import util.CollectionUtils;
import utility.applicaiton.ApplicationLayout;

public abstract class AbstractGhidraHeadedIntegrationTest extends AbstractGhidraHeadlessIntegrationTest {

	public AbstractGhidraHeadedIntegrationTest() {
		super();

		// Ensure that all headed tests use swing popups when displaying errors.  Setting this
		// to false would force errors to only be written to the console.
		setErrorGUIEnabled(true);
	}

	@Override
	protected ApplicationLayout createApplicationLayout() {
		try {
			return new GhidraTestApplicationLayout(new File(getTestDirectoryPath()));
		}
		catch (IOException e) {
			throw new AssertException(e);
		}
	}

	@Override
	protected ApplicationConfiguration createApplicationConfiguration() {
		GhidraApplicationConfiguration config = new GhidraApplicationConfiguration();
		config.setShowSplashScreen(false);
		return config;
	}

	/**
	 * Flushes the given program's events before waiting for the swing update manager
	 * 
	 * @param program The program whose events will be flushed; may be null
	 */
	public static void waitForProgram(Program program) {
		if (program != null) {
			program.flushEvents();
		}

		waitForSwing();
	}

	/**
	 * Adds the given plugin to the tool and then returns the instance of the plugin that was
	 * added
	 * 
	 * @param tool the tool
	 * @param c the class of the plugin to add
	 * @return the newly added plugin
	 * @throws PluginException  if the plugin could not be constructed, or there was problem 
	 * 		   executing its init() method, or if a plugin of this class already exists in the tool
	 */
	public static <T extends Plugin> T addPlugin(PluginTool tool, Class<T> c)
			throws PluginException {

		tool.addPlugin(c.getName());
		return getPlugin(tool, c);
	}

	public static Plugin getPluginByName(PluginTool tool, String pluginName) {
		List<Plugin> list = tool.getManagedPlugins();
		Iterator<Plugin> it = list.iterator();
		while (it.hasNext()) {
			Plugin p = it.next();
			if (pluginName.equals(p.getName())) {
				return p;
			}
		}
		return null;
	}

	/**
	 * Finds the action by the given owner name and action name.  
	 * If you do not know the owner name, then use  
	 * the call {@link #getActions(DockingTool, String)} instead.
	 * 
	 * <P>Note: more specific test case subclasses provide other methods for finding actions 
	 * when you have an owner name (which is usually the plugin name).
	 * 
	 * @param tool the tool containing all system actions
	 * @param name the name to match
	 * @return the matching action; null if no matching action can be found
	 */
	public static DockingActionIf getAction(DockingTool tool, String owner, String name) {
		String fullName = name + " (" + owner + ")";
		List<DockingActionIf> actions = tool.getDockingActionsByFullActionName(fullName);
		if (actions.isEmpty()) {
			return null;
		}

		if (actions.size() > 1) {
			// This shouldn't happen
			throw new AssertionFailedError(
				"Found more than one action for name '" + fullName + "'");
		}

		return CollectionUtils.any(actions);
	}

	public static DockingActionIf getAction(Plugin plugin, String actionName) {
		return getAction(plugin.getTool(), plugin.getName(), actionName);
	}

	public static DockingActionIf getLocalAction(ComponentProvider provider, String actionName) {
		return getAction(provider.getTool(), provider.getName(), actionName);
	}

	public static PluginTool showTool(final PluginTool tool) {
		runSwing(() -> {
			boolean wasErrorGUIEnabled = isUseErrorGUI();
			setErrorGUIEnabled(false);// disable the error GUI while launching the tool
			tool.setVisible(true);
			setErrorGUIEnabled(wasErrorGUIEnabled);
		});
		waitForBusyTool(tool);
		return tool;
	}

	/**
	 * Shows the given DialogComponentProvider using the given tool's 
	 * {@link PluginTool#showDialog(DialogComponentProvider)} method.  After calling show on a 
	 * new thread the method will then wait for the dialog to be shown by calling
	 * {@link TestEnv#waitForDialogComponent(Window, Class, int)}.
	 * 
	 * @param tool The tool used to show the given provider.
	 * @param provider The DialogComponentProvider to show.
	 * @return The provider once it has been shown, or null if the provider is not shown within
	 *         the given maximum wait time.
	 */
	public static DialogComponentProvider showDialogWithoutBlocking(PluginTool tool,
			DialogComponentProvider provider) {

		executeOnSwingWithoutBlocking(() -> tool.showDialog(provider));

		// this call waits for the dialog to be shown
		DialogComponentProvider dialog = waitForDialogComponent(provider.getClass());
		waitForSwing();
		return dialog;
	}

	/**
	 * Waits for the tool to finish executing commands and tasks
	 * 
	 * @param tool the tool
	 * @throws AssertionFailedError if the tool does not finish work within a reasonable limit
	 */
	public static void waitForBusyTool(PluginTool tool) {

		waitForSwing(); // let any posted tasks have a chance to be registered

		int timeout = PRIVATE_LONG_WAIT_TIMEOUT * 2;
		int totalTime = 0;
		while (tool.isExecutingCommand() || TaskUtilities.isExecutingTasks()) {

			totalTime += sleep(DEFAULT_WAIT_DELAY);
			if (totalTime >= timeout) {
				throw new AssertionFailedError("Timed-out waiting for tool to finish tasks");
			}
		}

		// let any pending Swing work finish
		waitForSwing();
	}

	/**
	 * @deprecated use {@link #waitForBusyTool(PluginTool)} instead
	 */
	@Deprecated
	public static void waitForAnalysis() {
		@SuppressWarnings("unchecked")
		Map<Program, AutoAnalysisManager> programToManagersMap =
			(Map<Program, AutoAnalysisManager>) getInstanceField("managerMap",
				AutoAnalysisManager.class);

		Collection<AutoAnalysisManager> managers = programToManagersMap.values();
		for (AutoAnalysisManager manager : managers) {
			while (manager.isAnalyzing()) {
				sleep(DEFAULT_WAIT_DELAY);
			}
		}
	}

	/**
	 * Save the given tool to the project tool chest.  If the tool already exists, then it will
	 * be overwritten with the given tool. 
	 * 
	 * @param project The project which with the tool is associated.
	 * @param tool The tool to be saved
	 */
	public static PluginTool saveTool(final Project project, final PluginTool tool) {
		AtomicReference<PluginTool> ref = new AtomicReference<>();
		runSwing(() -> {
			ToolChest toolChest = project.getLocalToolChest();
			ToolTemplate toolTemplate = tool.saveToolToToolTemplate();
			toolChest.replaceToolTemplate(toolTemplate);

			ToolManager toolManager = project.getToolManager();
			Workspace workspace = toolManager.getActiveWorkspace();
			tool.close();
			ref.set((PluginTool) workspace.runTool(toolTemplate));
		});

		return ref.get();
	}

	/**
	 * Triggers a browser click at the current cursor location.  Thus, this method should be 
	 * called only after the browser location is set the the desired field.
	 * 
	 * @param codeBrowser the CodeBrowserPlugin
	 * @param clickCount the click count
	 */
	public void click(CodeBrowserPlugin codeBrowser, int clickCount) {
		click(codeBrowser, clickCount, true);
	}

	public void click(CodeBrowserPlugin codeBrowser, int clickCount, boolean wait) {

		// make sure that the code browser is ready to go--sometimes it is not, due to timing
		// during the testing process, like when the tool is first loaded.
		codeBrowser.updateNow();
		click(codeBrowser.getFieldPanel(), clickCount, wait);
		codeBrowser.updateNow();
	}

	@SuppressWarnings("unchecked")
	protected void click(FieldPanel fp, int clickCount, boolean wait) {
		MouseEvent ev = new MouseEvent(fp, 0, System.currentTimeMillis(), 0, 0, 0, clickCount,
			false, MouseEvent.BUTTON1);

		runSwing(() -> {

			FieldLocation loc = fp.getCursorLocation();
			Field field = fp.getCurrentField();

			List<FieldMouseListener> listeners =
				(List<FieldMouseListener>) getInstanceField("fieldMouseListeners", fp);
			for (FieldMouseListener l : listeners) {
				l.buttonPressed(loc, field, ev);
			}
		}, wait);
	}
}