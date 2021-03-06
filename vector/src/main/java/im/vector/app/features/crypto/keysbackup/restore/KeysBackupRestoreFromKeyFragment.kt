/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.app.features.crypto.keysbackup.restore

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.lifecycle.Observer
import butterknife.BindView
import butterknife.OnClick
import butterknife.OnTextChanged
import com.google.android.material.textfield.TextInputLayout
import im.vector.app.R
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.startImportTextFromFileIntent
import org.matrix.android.sdk.api.extensions.tryOrNull
import javax.inject.Inject

class KeysBackupRestoreFromKeyFragment @Inject constructor()
    : VectorBaseFragment() {

    override fun getLayoutResId() = R.layout.fragment_keys_backup_restore_from_key

    private lateinit var viewModel: KeysBackupRestoreFromKeyViewModel
    private lateinit var sharedViewModel: KeysBackupRestoreSharedViewModel

    @BindView(R.id.keys_backup_key_enter_til)
    lateinit var mKeyInputLayout: TextInputLayout

    @BindView(R.id.keys_restore_key_enter_edittext)
    lateinit var mKeyTextEdit: EditText

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = fragmentViewModelProvider.get(KeysBackupRestoreFromKeyViewModel::class.java)
        sharedViewModel = activityViewModelProvider.get(KeysBackupRestoreSharedViewModel::class.java)
        mKeyTextEdit.setText(viewModel.recoveryCode.value)
        mKeyTextEdit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onRestoreFromKey()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        mKeyInputLayout.error = viewModel.recoveryCodeErrorText.value
        viewModel.recoveryCodeErrorText.observe(viewLifecycleOwner, Observer { newValue ->
            mKeyInputLayout.error = newValue
        })
    }

    @OnTextChanged(R.id.keys_restore_key_enter_edittext)
    fun onRestoreKeyTextEditChange(s: Editable?) {
        s?.toString()?.let {
            viewModel.updateCode(it)
        }
    }

    @OnClick(R.id.keys_restore_button)
    fun onRestoreFromKey() {
        val value = viewModel.recoveryCode.value
        if (value.isNullOrBlank()) {
            viewModel.recoveryCodeErrorText.value = context?.getString(R.string.keys_backup_recovery_code_empty_error_message)
        } else {
            viewModel.recoverKeys(sharedViewModel)
        }
    }

    @OnClick(R.id.keys_backup_import)
    fun onImport() {
        startImportTextFromFileIntent(requireContext(), textFileStartForActivityResult)
    }

    private val textFileStartForActivityResult = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val dataURI = activityResult.data?.data ?: return@registerStartForActivityResult
            tryOrNull(message = "Failed to read recovery kay from text") {
                activity
                        ?.contentResolver
                        ?.openInputStream(dataURI)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?.let {
                            mKeyTextEdit.setText(it)
                            mKeyTextEdit.setSelection(it.length)
                        }
            }
        }
    }
}
