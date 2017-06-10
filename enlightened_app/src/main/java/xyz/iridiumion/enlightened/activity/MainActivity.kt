package xyz.iridiumion.enlightened.activity

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.TypedValue
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast

import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.github.jksiezni.permissive.PermissionsGrantedListener
import com.github.jksiezni.permissive.PermissionsRefusedListener
import com.github.jksiezni.permissive.Permissive
import com.nbsp.materialfilepicker.MaterialFilePicker
import com.nbsp.materialfilepicker.ui.FilePickerActivity

import java.io.File
import java.io.IOException

import xyz.iridiumion.enlightened.EnlightenedApplication
import xyz.iridiumion.enlightened.R
import xyz.iridiumion.iridiumhighlightingeditor.editor.HighlightingDefinition
import xyz.iridiumion.iridiumhighlightingeditor.editor.IridiumHighlightingEditorJ
import xyz.iridiumion.enlightened.fragment.EditorFragment
import xyz.iridiumion.iridiumhighlightingeditor.highlightingdefinitions.HighlightingDefinitionLoader
import xyz.iridiumion.enlightened.util.FileIOUtil
import xyz.iridiumion.enlightened.util.RandomUtils
import xyz.iridiumion.enlightened.widget.TouchThruDrawerLayout

class MainActivity : AppCompatActivity(), IridiumHighlightingEditorJ.OnTextChangedListener {
    private var editorFragment: EditorFragment? = null
    private var currentOpenFilePath: String? = null
    private var drawerLayout: TouchThruDrawerLayout? = null
    private var drawerToggle: ActionBarDrawerToggle? = null
    private var menuFrame: View? = null
    private var toolbar: Toolbar? = null
    private var recentFilesListView: ListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initSystemBars(this)
        initToolbar()
        initDrawer()
        initRecentList()

        val editorFragmentView = supportFragmentManager.findFragmentByTag(
                EditorFragment.TAG)

        if (savedInstanceState == null || editorFragmentView == null) {
            editorFragment = EditorFragment()

            supportFragmentManager
                    .beginTransaction()
                    .replace(
                            R.id.content_frame,
                            editorFragment,
                            EditorFragment.TAG)
                    .commit()
        } else {
            editorFragment = editorFragmentView as EditorFragment
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        val inflater = menuInflater
        inflater.inflate(R.menu.main, menu)

        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        drawerToggle!!.onConfigurationChanged(newConfig)
    }

    override fun onPostCreate(state: Bundle?) {
        super.onPostCreate(state)

        drawerToggle!!.syncState()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val insert_tab_visible_key = resources.getString(R.string.prefs_key_show_tab_tool)

        menu.findItem(R.id.insert_tab).isVisible = EnlightenedApplication.preferences!!
                .getBoolean(insert_tab_visible_key, true)

        /*
        menu.findItem(R.id.save_file)
                .setEnabled(currentOpenFilePath != null);
        */

        return super.onPrepareOptionsMenu(menu)
    }

    private fun initDrawer() {
        drawerLayout = findViewById(
                R.id.drawer_layout) as TouchThruDrawerLayout

        menuFrame = findViewById(R.id.menu_frame)

        drawerToggle = object : ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.drawer_open,
                R.string.drawer_close) {
            override fun onDrawerClosed(view: View?) {
                supportInvalidateOptionsMenu()
            }

            override fun onDrawerOpened(view: View?) {
                supportInvalidateOptionsMenu()
            }
        }

        drawerToggle!!.isDrawerIndicatorEnabled = true
        drawerLayout!!.addDrawerListener(drawerToggle!!)
    }


    private fun initRecentList() {
        recentFilesListView = findViewById(R.id.recent_files) as ListView
        recentFilesListView!!.emptyView = findViewById(R.id.no_files)
        recentFilesListView!!.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            selectRecentFile(id)
            closeDrawer()
        }
    }

    private fun selectRecentFile(id: Long) {

    }

    private fun closeDrawer() {
        if (drawerLayout == null)
            return

        drawerLayout!!.closeDrawer(menuFrame)
    }

    private fun openDrawer() {
        if (drawerLayout == null)
            return

        drawerLayout!!.openDrawer(menuFrame)
    }

    private fun initToolbar() {
        toolbar = findViewById(R.id.en_toolbar) as Toolbar
        setSupportActionBar(toolbar)
    }

    private fun updateUiToPreferences() {
        invalidateOptionsMenu()

        val editor_font_size_key = resources.getString(R.string.prefs_key_font_size)
        val newFontSize = java.lang.Float.parseFloat(EnlightenedApplication.preferences!!.getString(editor_font_size_key, "12.0"))
        editorFragment!!.editor.setTextSize(TypedValue.COMPLEX_UNIT_SP, newFontSize)
        val editor_tab_size_key = resources.getString(R.string.prefs_key_tab_size)
        val newTabSize = java.lang.Float.parseFloat(EnlightenedApplication.preferences!!.getString(editor_tab_size_key, "4.0")).toInt()
        editorFragment!!.editor.setTabWidth(newTabSize)
    }

    override fun onResume() {
        super.onResume()

        updateUiToPreferences()
        updateRecentFilesList()
    }

    override fun onTextChanged(text: String) {
        /*
        if (!ShaderEditorApplication
                .preferences
                .doesRunOnChange())
            return;
        */
        if (editorFragment!!.hasErrorLine()) {
            editorFragment!!.clearError()
            editorFragment!!.updateHighlighting()
        }
        val autosave_key = resources.getString(R.string.prefs_key_autosave)
        if (EnlightenedApplication.preferences!!
                .getBoolean(autosave_key, true) && currentOpenFilePath != null) {
            saveOpenFile(false, false)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.insert_tab -> {
                insertTab()
                return true
            }
            R.id.open_file -> {
                openFile()
                return true
            }
            R.id.save_file -> {
                saveOpenFile()
                return true
            }
            R.id.save_file_as -> {
                showSaveFileAsDialog()
                return true
            }
            R.id.settings -> {
                showSettings()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateRecentFilesList() {
        //TODO: actually implement
        showNoRecentFilesAvailable()
    }

    private fun showNoRecentFilesAvailable() {
        val progressView = findViewById(
                R.id.progress_bar)
        val textView = findViewById(
                R.id.no_recent_files_message)

        if ((progressView == null) || (textView == null))
            return

        progressView.visibility = View.GONE
        textView.visibility = View.VISIBLE
    }

    private fun saveOpenFile(showErrorIfAccident: Boolean = true, showToast: Boolean = true) {
        if (currentOpenFilePath == null && showErrorIfAccident) {
            /*
            new MaterialDialog.Builder(MainActivity.this)
                    .title("No file open")
                    .content("You must open a file in order to save it.")
                    .positiveText("Got it")
                    .show();
            */
            showSaveFileAsDialog()
            return
        }
        Permissive.Request(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .whenPermissionsGranted {
                    // given permissions are granted

                    val textToSave = editorFragment!!.editor.text.toString()
                    try {
                        FileIOUtil.writeAllText(currentOpenFilePath!!, textToSave)
                        if (showToast)
                            Toast.makeText(this@MainActivity, R.string.file_saved, Toast.LENGTH_SHORT).show()
                    } catch (e: IOException) {
                        //e.printStackTrace();
                        showExceptionDialog(e)
                    }
                }
                .whenPermissionsRefused {
                    // given permissions are refused
                    this@MainActivity.showPermissionMissing()
                }
                .execute(this@MainActivity)
    }

    private fun showSaveFileAsDialog() {
        val saveFileAsDialog = MaterialDialog.Builder(this)
                .title("Save File As")
                .customView(R.layout.dialog_save_file_as, true)
                .positiveText(R.string.save_file_as)
                .negativeText(R.string.cancel)
                .onPositive(MaterialDialog.SingleButtonCallback { dialog, which ->
                    val view = dialog.customView ?: return@SingleButtonCallback
                    val saveDirectoryInput = (view.findViewById(R.id.sfad_dir_path) as EditText).text.toString()
                    val saveFileName = (view.findViewById(R.id.sfad_file_name) as EditText).text.toString()
                    currentOpenFilePath = File(saveDirectoryInput, saveFileName).absolutePath
                    saveOpenFile()
                })
                .show()
        val view = saveFileAsDialog.customView
        var saveDirectoryInput: EditText? = null
        if (view != null) {
            saveDirectoryInput = view.findViewById(R.id.sfad_dir_path) as EditText
            saveDirectoryInput.setText(Environment.getExternalStorageDirectory().path)
        }
    }

    private fun showSettings() {
        val i = Intent(this, SettingsActivity::class.java)
        startActivity(i)
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun openFile() {
        Permissive.Request(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .whenPermissionsGranted {
                    // given permissions are granted
                    browseForFile()
                }
                .whenPermissionsRefused { this@MainActivity.showPermissionMissing() }
                .execute(this@MainActivity)
    }

    private fun showPermissionMissing() {
        // given permissions are refused
        MaterialDialog.Builder(this@MainActivity)
                .title(R.string.permission_not_granted)
                .content(R.string.load_save_permission_required)
                .positiveText(R.string.dialog_got_it)
                .show()
    }

    private fun browseForFile() {
        MaterialFilePicker()
                .withActivity(this)
                .withRequestCode(REQUEST_CODE_BROWSE_FOR_FILE)
                .start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_BROWSE_FOR_FILE && resultCode == Activity.RESULT_OK) {
            val selectedFilePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH)
            val selectedFile = File(selectedFilePath)
            val selectedFileExt = RandomUtils.getFileExtension(selectedFilePath)
            //Load file into editor
            try {
                val fileContent = FileIOUtil.readAllText(selectedFilePath)
                editorFragment!!.editor.setText(fileContent)
                val definitionLoader = HighlightingDefinitionLoader()
                val highlightingDefinition = definitionLoader.selectDefinitionFromFileExtension(selectedFileExt)
                editorFragment!!.editor.loadHighlightingDefinition(highlightingDefinition)
                currentOpenFilePath = selectedFilePath
                Toast.makeText(this, R.string.file_loaded, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                //e.printStackTrace();
                showExceptionDialog(e)
            }

        }
    }

    private fun showExceptionDialog(e: Exception) {
        MaterialDialog.Builder(this@MainActivity)
                .title(R.string.oops)
                .content(String.format("An unexpected error occurred: %s", if (e.message == null)
                    e.toString()
                else
                    e.message))
                .positiveText(R.string.dialog_got_it)
                .show()
    }

    private fun insertTab() {
        editorFragment!!.insertTab()
    }

    companion object {

        private val REQUEST_CODE_BROWSE_FOR_FILE = 1

        private fun initSystemBars(activity: AppCompatActivity) {

        }
    }
}
