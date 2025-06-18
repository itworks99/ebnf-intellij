/**
 * Service for managing EBNF plugin settings.
 */
package com.github.itworks99.ebnf.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * Service that manages the persistent state of EBNF plugin settings.
 * This service is application-level and stores settings in the workspace.xml file.
 */
@Service(Service.Level.APP)
@State(
    name = "com.github.itworks99.ebnf.settings.EbnfSettingsService",
    storages = [Storage("ebnfSettings.xml")]
)
class EbnfSettingsService : PersistentStateComponent<EbnfSettingsState> {
    private var myState = EbnfSettingsState()

    override fun getState(): EbnfSettingsState {
        return myState
    }

    override fun loadState(state: EbnfSettingsState) {
        myState = state
    }

    companion object {
        /**
         * Gets the instance of the settings service.
         *
         * @return The settings service instance.
         */
        fun getInstance(): EbnfSettingsService {
            return ApplicationManager.getApplication().getService(EbnfSettingsService::class.java)
        }
    }
}