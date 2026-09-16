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

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import com.itsaky.androidide.R
import com.itsaky.androidide.actions.ActionData
import com.itsaky.androidide.actions.EditorActivityAction
import com.itsaky.androidide.activities.TerminalActivity
import com.itsaky.androidide.idetooltips.TooltipTag
import com.itsaky.androidide.projects.IProjectManager
import com.itsaky.androidide.utils.Environment
import com.itsaky.androidide.utils.applyMultiWindowFlags
import com.itsaky.androidide.utils.flashError
import com.termux.app.TermuxService
import com.termux.shared.shell.command.ExecutionCommand.Runner
import com.termux.shared.shell.command.ExecutionCommand.ShellCreateMode
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_SERVICE
import java.io.File

/** Opens the terminal and starts the configured Code On The Go MCP tunnel. */
class StartMcpAction(
	context: Context,
	override val order: Int,
) : EditorActivityAction() {
	override val id: String = ID

	init {
		label = context.getString(R.string.action_start_mcp)
		icon = ContextCompat.getDrawable(context, R.drawable.ic_mcp)
		requiresUIThread = true
	}

	override fun retrieveTooltipTag(isReadOnlyContext: Boolean): String =
		TooltipTag.EDITOR_TOOLBAR_START_MCP

	override suspend fun execAction(data: ActionData): Any {
		val activity = data.getActivity() ?: return false
		val workingDirectory =
			IProjectManager.getInstance().projectDirPath ?: Environment.HOME.absolutePath

		return runCatching {
			val intents =
				createMcpLaunchIntents(
					context = activity,
					shellPath = Environment.BASH_SHELL.absolutePath,
					workingDirectory = workingDirectory,
				)
			checkNotNull(activity.startService(intents.service))
			activity.startActivity(intents.terminal)
			true
		}.getOrElse { error ->
			activity.flashError(
				activity.getString(
					R.string.error_start_mcp,
					error.message ?: error.javaClass.simpleName,
				),
			)
			false
		}
	}

	companion object {
		const val ID = "ide.editor.mcp.start"
		internal const val SESSION_NAME = "Code On The Go MCP"
		internal const val TUNNEL_PROFILE = "code-on-the-go"

		internal fun createMcpLaunchIntents(
			context: Context,
			shellPath: String,
			workingDirectory: String,
		): McpLaunchIntents {
			val command =
				"if command -v tunnel-client >/dev/null 2>&1; then " +
					"exec tunnel-client run --profile $TUNNEL_PROFILE; " +
					"else printf '\\ntunnel-client is not installed. Install it and create profile $TUNNEL_PROFILE.\\n'; " +
					"exec '$shellPath' -l; fi"
			val serviceIntent =
				Intent(TERMUX_SERVICE.ACTION_SERVICE_EXECUTE, Uri.fromFile(File(shellPath))).apply {
					setClass(context, TermuxService::class.java)
					putExtra(TERMUX_SERVICE.EXTRA_ARGUMENTS, arrayOf("-lc", command))
					putExtra(TERMUX_SERVICE.EXTRA_WORKDIR, workingDirectory)
					putExtra(TERMUX_SERVICE.EXTRA_RUNNER, Runner.TERMINAL_SESSION.runnerName)
					putExtra(
						TERMUX_SERVICE.EXTRA_SESSION_ACTION,
						TERMUX_SERVICE.VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_DONT_OPEN_ACTIVITY
							.toString(),
					)
					putExtra(TERMUX_SERVICE.EXTRA_SHELL_NAME, SESSION_NAME)
					putExtra(
						TERMUX_SERVICE.EXTRA_SHELL_CREATE_MODE,
						ShellCreateMode.NO_SHELL_WITH_NAME.mode,
					)
					putExtra(TERMUX_SERVICE.EXTRA_COMMAND_LABEL, SESSION_NAME)
				}
			val terminalIntent =
				Intent(context, TerminalActivity::class.java).applyMultiWindowFlags(context)

			return McpLaunchIntents(serviceIntent, terminalIntent)
		}
	}
}

internal data class McpLaunchIntents(
	val service: Intent,
	val terminal: Intent,
)
