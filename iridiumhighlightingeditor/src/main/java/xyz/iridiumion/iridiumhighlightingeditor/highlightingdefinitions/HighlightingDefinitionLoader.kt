package xyz.iridiumion.iridiumhighlightingeditor.highlightingdefinitions

import xyz.iridiumion.iridiumhighlightingeditor.editor.HighlightingDefinition
import xyz.iridiumion.iridiumhighlightingeditor.highlightingdefinitions.definitions.*

/**
 * Author: 0xFireball
 */
class HighlightingDefinitionLoader {

    fun selectDefinitionFromFileExtension(selectedFileExt: String): HighlightingBase {
        when (selectedFileExt) {
            "js" -> return JavaScriptHighlightingDefinition()
            "java" -> return JavaHighlightingDefinition()
            "cs" -> return CSharpHighlightingDefinition()
            "cpp", "cxx" -> return CPlusPlusHighlightingDefinition()
            "lua" -> return LuaHighlightingDefinition()
            // "py" -> return PythonHighlightingDefinition() //Not yet ready!
            "txt" -> return NoHighlightingDefinition()
            else -> {
                return GenericHighlightingDefinition()
            }
        }
    }
}
