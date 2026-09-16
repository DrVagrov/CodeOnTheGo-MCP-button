/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.itsaky.androidide.actions.etc

import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.itsaky.androidide.actions.ActionItem
import com.itsaky.androidide.actions.ActionsRegistry
import com.itsaky.androidide.activities.TerminalActivity
import com.itsaky.androidide.utils.EditorActivityActions
import com.termux.app.TermuxService
import com.termux.shared.shell.command.ExecutionCommand.Runner
import com.termux.shared.shell.command.ExecutionCommand.ShellCreateMode
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_SERVICE
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StartMcpActionTest {
	private val context = ApplicationProvider.getApplicationContext<Context>()
	private val shellPath = "/data/data/com.itsaky.androidide/files/usr/bin/bash"
	private val projectPath = "/storage/emulated/0/CodeOnTheGoProjects/Sample"

	@Test
	fun `MCP action is placed after Gradle tasks in the editor toolbar`() {
		EditorActivityActions.register(context)

		val action =
			ActionsRegistry
				.getInstance()
				.getActions(ActionItem.Location.EDITOR_TOOLBAR)[StartMcpAction.ID]

		assertThat(action).isNotNull()
		assertThat(action?.order).isEqualTo(4)
	}

	@Test
	fun `MCP button launches the named tunnel profile in a terminal session`() {
		val intents =
			StartMcpAction.createMcpLaunchIntents(
				context = context,
				shellPath = shellPath,
				workingDirectory = projectPath,
			)

		assertThat(intents.service.component).isEqualTo(ComponentName(context, TermuxService::class.java))
		assertThat(intents.service.action).isEqualTo(TERMUX_SERVICE.ACTION_SERVICE_EXECUTE)
		assertThat(intents.service.data?.path).isEqualTo(shellPath)
		assertThat(intents.service.getStringExtra(TERMUX_SERVICE.EXTRA_WORKDIR)).isEqualTo(projectPath)
		assertThat(intents.service.getStringExtra(TERMUX_SERVICE.EXTRA_RUNNER))
			.isEqualTo(Runner.TERMINAL_SESSION.runnerName)
		assertThat(intents.service.getStringExtra(TERMUX_SERVICE.EXTRA_SESSION_ACTION))
			.isEqualTo(
				TERMUX_SERVICE.VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_DONT_OPEN_ACTIVITY
					.toString(),
			)
		assertThat(intents.service.getStringExtra(TERMUX_SERVICE.EXTRA_SHELL_NAME))
			.isEqualTo(StartMcpAction.SESSION_NAME)
		assertThat(intents.service.getStringExtra(TERMUX_SERVICE.EXTRA_SHELL_CREATE_MODE))
			.isEqualTo(ShellCreateMode.NO_SHELL_WITH_NAME.mode)

		val arguments = intents.service.getStringArrayExtra(TERMUX_SERVICE.EXTRA_ARGUMENTS)
		assertThat(arguments?.first()).isEqualTo("-lc")
		assertThat(arguments?.last()).contains(
			"tunnel-client run --profile ${StartMcpAction.TUNNEL_PROFILE}",
		)
		assertThat(intents.terminal.component).isEqualTo(ComponentName(context, TerminalActivity::class.java))
	}

	@Test
	fun `MCP launch command keeps credentials out of the application`() {
		val command =
			StartMcpAction
				.createMcpLaunchIntents(context, shellPath, projectPath)
				.service
				.getStringArrayExtra(TERMUX_SERVICE.EXTRA_ARGUMENTS)
				?.last()

		assertThat(command).doesNotContain("CONTROL_PLANE_API_KEY")
		assertThat(command).doesNotContain("sk-")
	}

	@Test
	fun `terminal activity stays private because it can host commands`() {
		val info =
			context.packageManager.getActivityInfo(
				ComponentName(context, TerminalActivity::class.java),
				0,
			)

		assertThat(info.exported).isFalse()
	}

	@Test
	fun `Termux command service stays private`() {
		val info =
			context.packageManager.getServiceInfo(
				ComponentName(context, TermuxService::class.java),
				0,
			)

		assertThat(info.exported).isFalse()
	}
}
