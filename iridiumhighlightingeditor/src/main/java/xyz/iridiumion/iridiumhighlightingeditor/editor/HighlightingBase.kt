package xyz.iridiumion.iridiumhighlightingeditor.highlightingdefinitions.definitions

import java.util.regex.Pattern

import xyz.iridiumion.iridiumhighlightingeditor.editor.HighlightingDefinition
import java.util.ArrayList
/**
 * @author kevin
 */
abstract class HighlightingBase : HighlightingDefinition {
    val array : ArrayList<String> = ArrayList()
    fun getKeywords(): ArrayList<String> {

        if(array.size > 0)
            return array

        val pattern = Pattern.compile("\\((.*?)\\)")
        if(preprocessorPattern.toString().contains("(")){
            val m = pattern.matcher(preprocessorPattern.toString())
            if(m.find()){
                array.addAll(m.group(1).split("|"))
            }
        }

        if(keywordPattern.toString().contains("(")){
            val m = pattern.matcher(keywordPattern.toString())
            if(m.find()){
                array.addAll(m.group(1).split("|"))
            }
        }
        return array
    }
}