
// Copyright (C) 2011 Volker Boerchers
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 2 of the License, or
// (at your option) any later version.


////////////////////////////////////////////////////////////////////////////////
// This script checks the current map for conformity to the add-on standards.
// Notes are overridden without further notice. So either never run this script
// or write comments into the node details
////////////////////////////////////////////////////////////////////////////////

import groovy.io.FileType

import java.awt.Dimension

import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JScrollPane

import org.freeplane.plugin.script.proxy.Proxy
import org.freeplane.plugin.script.FreeplaneScriptBaseClass.ConfigProperties

def config = new ConfigProperties()

messages = []
// a List<String>
filesToUninstall = []

def addMessage(String message) {
    messages << message
    logger.info(message)
}

private int showConfirmDialog(String message, String title) {
    JLabel label = new JLabel(message)
    JScrollPane scrollPane = new JScrollPane(label)
    scrollPane.setPreferredSize(new Dimension(700, 500))
    return ui.showConfirmDialog(null, scrollPane, title, JOptionPane.YES_NO_OPTION)
}

def File mapFile = node.map.file
if (mapFile) {
    try {
        def String backup = mapFile.path + '.bak'
        new File(mapFile.path + '.bak').bytes = mapFile.bytes
        addMessage("Created backup file " + backup)
    }
    catch (Exception e) {
        addMessage("Couldn't create backup file due to " + e.message)
    }
}

def LEFT = true
def RIGHT = false

def createMissingAttributes(Proxy.Node node, List<String> attributes) {
    attributes.each {
        def name = (it instanceof List) ? it[0] : it
        def value = (it instanceof List) ? it[1] : ""
        if (!node.attributes.containsKey(name)) {
            node[name] = value
            addMessage("Created attribute '$name' = '$value' in '${node.plainText}'")
        }
    }
    node.attributes.optimizeWidths()
}

def toPhrase(String texto){
    def words = texto.split("(?<!^)(?=[A-Z])")
    words = words.take(1)*.capitalize() + words.drop(1)*.toLowerCase()
    return words.join(' ')
}

Proxy.Node findOrCreate(Proxy.Node parent, String name, boolean isLeft) {
    def node = parent.children.find{ it.plainText == name }
    if (node == null) {
        node = parent.createChild(name)
        addMessage("Created node " + name)
    }
    if (node.left != isLeft)
        node.left = isLeft
    return node
}

String withBody(String body) {
    return '''<html>
  <head>

  </head>
  <body>
''' + body + '''
  </body>
</html>
'''
}


def createPreferencesXmlText(nodo, name){
    def texto = """<?xml version="1.0" encoding="UTF-8"?>
<preferences_structure>
         <tabbed_pane>
                  <tab name="plugins">
                            <separator name = "$name">
"""
    def leadingSpaces = ' ' * 36
    nodo.attributes.each{a -> 
        def vals = a.value.toString().replace(' ','').split(',')
        switch(vals[0].toString().toLowerCase()){
            case ['boolean','string'] :
                texto += leadingSpaces + "<${vals[0].toString().toLowerCase()} name = \"${name}_${a.key.toString().replace(' ','')}\"/>\n"
                break
           case 'number'  :
                texto += vals.size() == 3 ? 
                            leadingSpaces + "<number name  = \"${name}_${a.key.toString()}\" min=\"${vals[1]}\" max=\"${vals[2]}\"/>\n"
                            : "\n\n-------- Attribute has wrong number of parameters: -------\n ${a.key}   :   ${a.value} \n${'-' * 50}\n\n"
                break
             default :
                texto += "\n\n-------- Attribute not recognized: -------\n ${a.key}   :   ${a.value} \n${'-' * 50}\n\n"
                break
        }
    }
    texto += '''                           </separator>
                  </tab>
         </tabbed_pane>
</preferences_structure>'''
    return texto.toString()
}

// ======================================================================
//                               MAIN
// ======================================================================
def root = node.map.root

//
// ============ name ============
//
String addOnNameOrig = root.plainText
String addOnName = JOptionPane.showInputDialog(ui.frame, "Please enter the add-on name (e.g. 'My first add-on')!", addOnNameOrig)
if (!addOnName) {
    ui.errorMessage("Can't continue without a proper name")
    return
}
else if (!addOnName.equals(addOnNameOrig)) {
    root.text = addOnName
    addMessage("Set add-on name to $addOnName")
}
String addOnTechName = root['name']
if (!addOnTechName) {
    addOnTechName = addOnName.
        replaceAll('[\\W_]+(\\w)'){ match, letter -> letter.toString().toUpperCase() }.
        replaceAll('\\W', '')
        addOnTechName = addOnTechName.substring(0, 1).toLowerCase() +  addOnTechName.substring(1)
    root['name'] = addOnTechName
    addMessage("Set technical name to $addOnTechName")
}
else if (!addOnTechName.charAt(0).isLowerCase()) {
    ui.errorMessage("'name' attribute '$addOnTechName' (the technical add-on name) does not start with an lower case letter")
    return
}

//
// ============ root ============
//
if (!root.link) {
    addMessage("Please add a hyperlink to the add-on homepage to the root note!\nYou will not be able to release otherwise!")
}

if (!root.style.backgroundColorCode || root.style.backgroundColorCode.toLowerCase() == '#ffffff') {
    root.style.backgroundColorCode = '#97c7dc'
    root.style.font.italic = true
    root.style.font.bold = true
    root.style.font.size = 16
    addMessage("Set root node style")
}

root.note = withBody '''
    <p>
      The homepage of this add-on should be set as the link of the root node.
    </p>
    <p>
      The basic properties of this add-on. They can be used in script names 
      and other attributes, e.g. &quot;${name}.groovy&quot;.
    </p>
    <ul>
      <li>
        <b>name</b>: The name of the add-on, normally a technically one (no 
        spaces, no special characters except _.-).
      </li>
      <li>
        <b>author</b>: Author's name(s) and (optionally) email adresses.
      </li>
      <li>
        <b>version</b>: Since it's difficult to protect numbers like 1.0 from 
        Freeplane's number parser it's advised to prepend a 'v' to the number, 
        e.g. 'v1.0'.
      </li>
      <li>
        <b>freeplane-version-from</b>: The oldest compatible Freeplane version. 
        The add-on will not be installed if the Freeplane version is too old.
      </li>
      <li>
        <b>freeplane-version-to</b>: Normally empty: The newest compatible 
        Freeplane version. The add-on will not be installed if the Freeplane 
        version is too new.
      </li>
      <li>
        <b>updateUrl</b>: URL of the file containing information (version, 
        download url) on the latest version of this add-on.<br>By default: 
        &quot;${homepage}/version.properties&quot;<br>Examples:

        <ul>
          <li>
            <b>For GitHub releases</b>: 
            &quot;${homepage}/releases/latest/download/version.properties&quot;
          </li>
          <li>
            <b>For Github in a folder named like the add-on&nbsp;(in the main 
            repository branch)</b>: 
            &quot;${homepage}/raw/main/${name}/version.properties
          </li>
          <li>
            <b>For Github in a folder named like the add-on&nbsp;(in a repository 
            branch named as the add-on version)</b>: 
            &quot;${homepage}/raw/${version}/${name}/version.properties&quot;
          </li>
        </ul>
      </li>
      <li>
        <b>addonsMenu</b>: Defines the addon's main menu location, defaults menu 
        'main_menu_scripting'.<br>Use developer tool menuItemInfo to inspect 
        menu location keys.<br>This attribute is mandatory.<br>Example: 
        '/menu_bar/myAddons'
      </li>
      <li>
        <b>downloadUrl</b>: URL from the place where the AddOn file will be 
        available for downloading.<br>By default is the same as the homepage.<br>You 
        can define a different place or a subfolder of the homepage.<br>Examples:

        <ul>
          <li>
            <b>homepage subfolder 'files'</b>: &quot;${homepage}/files/&quot;
          </li>
          <li>
            <b>For GitHub releases (release named as the add-on version)</b>: 
            ${homepage}/releases/download/${version}/
          </li>
        </ul>
      </li>
      <li>
        <b>changelogUrl</b>: URL from the place where the history file will be 
        available for downloading.<br>By default is &quot;${homepage}/history.md&quot;<br>You 
        can define a different place or a subfolder of the homepage and a 
        different file name and extension if wanted.<br>Examples:

        <ul>
          <li>
            <b>txt file</b>: &quot;${homepage}/history.md&quot;
          </li>
          <li>
            <b>For GitHub releases as Markdown file</b>: 
            &quot;${homepage}/releases/latest/download/history.md&quot;
          </li>
        </ul>
      </li>
    </ul>
  </body>
</html>
'''


createMissingAttributes(root, [
    'name',
    'version',
    'author',
    'freeplaneVersionFrom',
    'freeplaneVersionTo',
    ['updateUrl'   ,  config.getProperty('devtools_updateUrl'   , '${homepage}/version.properties' ).replace('S{','${') ],
    ['downloadUrl' ,  config.getProperty('devtools_downloadUrl' , '${homepage}/'                   ).replace('S{','${') ],
    ['changelogUrl',  config.getProperty('devtools_changelogUrl', '${homepage}/history.txt'        ).replace('S{','${') ],
    ['addonsMenu'  ,  config.getProperty('devtools_addonsMenu'  , 'main_menu_scripting'            ).replace('S{','${') ]
])


//
// ============ description ============
//
findOrCreate(root, 'description', LEFT).note = withBody '''
    <p>
      Description would be awkward to edit as an attribute.
    </p>
    <p>
      So you have to put the add-on description as a child of the <i>'description'</i>&#160;node.
    </p>
    <p>
      To translate the description you have to define a translation for the key 'addons.${name}.description'.
    </p>
'''

//
// ============ changes ============
//
findOrCreate(root, 'changes', LEFT).note = withBody '''
    <p>
      Change log of this add-on: append one node for each noteworthy version and put the details for each version into a child node.
    </p>
'''

//
// ============ license ============
//
def licenseNode = findOrCreate(root, 'license', LEFT)
licenseNode.note = withBody '''
    <p>
      The add-ons's license that the user has to accept before she can install it.
    </p>
    <p>
    </p>
    <p>
      The License text has to be entered as a child of the <i>'license'</i>&#160;node, either as plain text or as HTML.
    </p>
'''

if (licenseNode.isLeaf()) {
    licenseNode.createChild '''
This add-on is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.
'''
    addMessage('''Missing license! - Added the default GNU v2+ license (like Freeplane).
Change the license if needed.''')
    licenseNode.folded = true
}

//
// ============ preferences.xml ============
//
def preferencesNode = findOrCreate(root, 'preferences.xml', LEFT)
preferencesNode.note = withBody '''
    <p>
      <font color="#000000" face="SansSerif, sans-serif">The child node contains the add-on configuration as an extension to 
      mindmapmodemenu.xml (in Tools-&gt;Preferences-&gt;Add-ons). </font>
    </p>
    <p>
      <font color="#000000" face="SansSerif, sans-serif">&nbsp;</font>
    </p>
    <p>
      <font color="#000000" face="SansSerif, sans-serif">Every property in the configuration should receive a default value in <i>default.properties</i>&nbsp;node. 
      </font>
    </p>
    <p>
      
    </p>
    <p>
      <b>Automatic way (new since v0.9.30): </b>
    </p>
    <p>
      you can add the preferences parameters as attributes to this node and 
      then, by checking AddOn it will:
    </p>
    <ul>
      <li>
        create the child node containing <font color="#000000" face="SansSerif, sans-serif">the add-on configuration as an 
        extension to mindmapmodemenu.xml</font>
      </li>
      <li>
        add the properties to the <font color="#000000" face="SansSerif, sans-serif"><i>default.properties</i>&nbsp;node</font>
      </li>
      <li>
        add the properties to the <i>translations</i><font color="#000000" face="SansSerif, sans-serif">&nbsp;node</font>
      </li>
    </ul>
    <p>
      
    </p>
    <p>
      <b>How? </b>
    </p>
    <ul>
      <li>
        Add an attribute for each preference.
      </li>
      <li>
        the attribute name should be the preference name.
      </li>
      <li>
        as attribute value you should specify if it is a <b>boolean</b>, <b>string</b>&nbsp;or 
        <b>number</b>&nbsp;preference
      </li>
      <li>
        if it is a <b>number</b>&nbsp;preference. you should add the min and max 
        value for it (separed by comma)
      </li>
    </ul>
    <p>
      
    </p>
    <p>
      <b>Example: </b>
    </p>
    <p>
      
    </p>
    <p>
      Attributes:
    </p>
    <table border="0" style="width: 80%; border-top-style: solid; border-right-style: solid; border-bottom-style: solid; border-left-style: solid; border-top-width: 0; border-right-width: 0; border-bottom-width: 0; border-left-width: 0">
      <tr>
        <td valign="top" style="width: 50%; border-top-style: solid; border-right-style: solid; border-bottom-style: solid; border-left-style: solid; border-top-width: 1; border-right-width: 1; border-bottom-width: 1; border-left-width: 1">
          <p style="margin-top: 1; margin-right: 1; margin-bottom: 1; margin-left: 1">
            isStudent
          </p>
        </td>
        <td valign="top" style="width: 50%; border-top-style: solid; border-right-style: solid; border-bottom-style: solid; border-left-style: solid; border-top-width: 1; border-right-width: 1; border-bottom-width: 1; border-left-width: 1">
          <p style="margin-top: 1; margin-right: 1; margin-bottom: 1; margin-left: 1">
            boolean
          </p>
        </td>
      </tr>
      <tr>
        <td valign="top" style="width: 50%; border-top-style: solid; border-right-style: solid; border-bottom-style: solid; border-left-style: solid; border-top-width: 1; border-right-width: 1; border-bottom-width: 1; border-left-width: 1">
          <p style="margin-top: 1; margin-right: 1; margin-bottom: 1; margin-left: 1">
            userName
          </p>
        </td>
        <td valign="top" style="width: 50%; border-top-style: solid; border-right-style: solid; border-bottom-style: solid; border-left-style: solid; border-top-width: 1; border-right-width: 1; border-bottom-width: 1; border-left-width: 1">
          <p style="margin-top: 1; margin-right: 1; margin-bottom: 1; margin-left: 1">
            string
          </p>
        </td>
      </tr>
      <tr>
        <td valign="top" style="width: 50%; border-top-style: solid; border-right-style: solid; border-bottom-style: solid; border-left-style: solid; border-top-width: 1; border-right-width: 1; border-bottom-width: 1; border-left-width: 1">
          <p style="margin-top: 1; margin-right: 1; margin-bottom: 1; margin-left: 1">
            birthMonth
          </p>
        </td>
        <td valign="top" style="width: 50%; border-top-style: solid; border-right-style: solid; border-bottom-style: solid; border-left-style: solid; border-top-width: 1; border-right-width: 1; border-bottom-width: 1; border-left-width: 1">
          <p style="margin-top: 1; margin-right: 1; margin-bottom: 1; margin-left: 1">
            number,1,12
          </p>
        </td>
      </tr>
    </table>
'''

//if preferencesNode has attributes, it creates the preferences xml text node
def checkPreferences = false
if(preferencesNode.attributes){
    def texto = createPreferencesXmlText(preferencesNode,'\${name}')
    def nodo  = preferencesNode.children[0]?:preferencesNode.createChild()
    if ( texto != nodo.plainText ){
        nodo.text = texto
        nodo.style.setMaxNodeWidth('20 cm')
        addMessage('Updated node with preferences XML text. Please check if it\'s right and repeat Build AddOn command.')
    } else {
        checkPreferences = true
    } 
}


//
// ============ default.properties ============
//
def defaultPropsNode = findOrCreate(root, 'default.properties', LEFT)
defaultPropsNode.note = withBody '''
    <p>
      These properties are used for:
    </p>
    <ul>
      <li>
        Each property defined in the preferences should have a default value in the attributes of this node.
      </li>
      <li>
        For each menu item with an icon add an attribute with the icon key (use developer tool menuItemInfo) as key and the icon path as value. Example: '${name}.icon': '/images/${name}-icon.png'
      </li>
    </ul>
'''

def preferencesNames = preferencesNode.attributes.names.collect{ '${name}_' + it.replace(' ' ,'') }
if( checkPreferences && preferencesNode.attributes ){
    createMissingAttributes(defaultPropsNode, preferencesNames)
}

//
// ============ translations ============
//
def translationsNode = findOrCreate(root, 'translations', LEFT)
translationsNode.note = withBody '''
    <p>
      The translation keys that this script uses. Define one child node per 
      supported locale. The attributes contain the translations. Define at 
      least
    </p>
    <ul>
      <li>
        'addons.${name}' for the add-on's name
      </li>
      <li>
        'addons.${name}.description' for the description, e.g. in the add-on 
        overview dialog (not necessary for English)
      </li>
      <li>
        'addons.${name}.&lt;scriptname&gt;' for each script since it will be the 
        menu title.
      </li>
      <li>
        'OptionPanel.separator.${name}' for the add-on's name in the 
        preferences panel
      </li>
      <li>
        'OptionPanel.&lt;property&gt;' for the label of the property in the 
        preferences panel
      </li>
      <li>
        'OptionPanel.&lt;property&gt;.tooltip' for the tooltip message for the 
        property in the preferences panel (whwn hovering on it with the mouse)
      </li>
    </ul>
'''
def englishTranslationsNode = findOrCreate(translationsNode, 'en', LEFT)
createMissingAttributes(englishTranslationsNode, [
    [ 'addons.${name}', addOnName ]
])
// englishTranslationsNode will be accessed later for script name translations

if( checkPreferences && preferencesNode.attributes ){
    def preferencesTranslationKeys = [ [ 'OptionPanel.separator.${name}' , addOnName ] ]
    preferencesNames.each{nom ->
        def t = 'OptionPanel.' + nom
        preferencesTranslationKeys << t << t + '.tooltip'
    }
    createMissingAttributes( englishTranslationsNode, preferencesTranslationKeys )
}

//
// ============ uninstall ============
//
def uninstallNode = findOrCreate(root, 'deinstall', LEFT)
uninstallNode.note = withBody '''
    <p>
      List of files and/or directories to remove on uninstall
    </p>
'''
if (uninstallNode.attributes.size() == 0) {
    uninstallNode.attributes.add('delete', '${installationbase}/addons/${name}.script.xml')
}

//
// ============ scripts ============
//
def scriptsNode = findOrCreate(root, 'scripts', RIGHT)
scriptsNode.note = withBody '''
    <p>
      An add-on may contain multiple scripts. The node text defines the script name (e.g. insertInlineImage.groovy). The name must have a suffix of a supported script language like .groovy or .js and may only consist of letters and digits. The script properties have to be configured via attributes:
    </p>
    <p>
    </p>
    <p>
      * menuLocation: &lt;locationkey&gt;
    </p>
    <p>
      &#160;&#160;&#160;- Defines the menu location, defaults a sub menu 'main_menu_scripting/addons.${name}'.
    </p>
    <p>
      &#160;&#160;&#160;-&#160;Use developer tool menuItemInfo to inspect menu location keys.
    </p>
    <p>
      &#160;&#160;&#160;- This attribute is mandatory
    </p>
    <p>
    </p>
    <p>
      * menuTitleKey: &lt;key&gt;
    </p>
    <p>
      &#160;&#160;&#160;- The menu item title will be looked up under the translation key &lt;key&gt; - don't forget to define its translation.
    </p>
    <p>
      &#160;&#160;&#160;- This attribute is mandatory
    </p>
    <p>
    </p>
    <p>
      * executionMode: &lt;mode&gt;
    </p>
    <p>
      &#160;&#160;&#160;- The execution mode as described in the Freeplane wiki (http://freeplane.sourceforge.net/wiki/index.php/Scripting)
    </p>
    <p>
      &#160;&#160;&#160;- ON_SINGLE_NODE: Execute the script once. The <i>node</i>&#160;variable is set to the selected node.
    </p>
    <p>
      &#160;&#160;&#160;- ON_SELECTED_NODE: Execute the script n times for n selected nodes, once for each node.
    </p>
    <p>
      &#160;&#160;&#160;- ON_SELECTED_NODE_RECURSIVELY: Execute the script on every selected node and recursively on all of its children.
    </p>
    <p>
      &#160;&#160;&#160;- In doubt use ON_SINGLE_NODE.
    </p>
    <p>
      &#160;&#160;&#160;- This attribute is mandatory
    </p>
    <p>
    </p>
    <p>
      * keyboardShortcut: &lt;shortcut&gt;
    </p>
    <p>
      &#160;&#160;&#160;- Optional: keyboard combination / accelerator for this script, e.g. control alt I
    </p>
    <p>
      &#160;&#160;&#160;- Use lowercase letters for modifiers and uppercase for letters. Use no + signs.
    </p>
    <p>
      &#160;&#160;&#160;- The available key names are listed at http://download.oracle.com/javase/1.4.2/docs/api/java/awt/event/KeyEvent.html#VK_0
    </p>
    <p>
      &#160;&#160;&#160;&#160;&#160;In the list only entries with a 'VK_' prefix count. Omit the prefix in the shortcut definition.
    </p>
    <p>
    </p>
    <p>
      * Permissions&#160;that the script(s) require, each either false or true:
    </p>
    <p>
      &#160;&#160;&#160;- execute_scripts_without_asking
    </p>
    <p>
      &#160;&#160;&#160;- execute_scripts_without_file_restriction: permission to read files
    </p>
    <p>
      &#160;&#160;&#160;- execute_scripts_without_write_restriction: permission to create/change/delete files
    </p>
    <p>
      &#160;&#160;&#160;- execute_scripts_without_exec_restriction: permission to execute other programs
    </p>
    <p>
      &#160;&#160;&#160;- execute_scripts_without_network_restriction: permission to access the network
    </p>
    <p>
      &#160;&#160;Notes:
    </p>
    <p>
      &#160;&#160;- The set of permissions is fixed.
    </p>
    <p>
      &#160;&#160;- Don't change the attribute names, don't omit one.
    </p>
    <p>
      &#160;&#160;- Set the values either to true or to false
    </p>
    <p>
      &#160;&#160;- In any case set execute_scripts_without_asking to true unless you want to annoy users.
    </p>
'''
if (node.map.file != null) {
    def filesToExclude = ['.classpath', '.project', 'freeplane.dsld', 'freeplane.gdsl']
    def scriptsDirs = []
    scriptsDirs << new File(node.map.file.parent, 'scripts')
    // includes scripts locations in case of Gradle plugin
    try {
        if (node.map.file.parentFile.parentFile.name == 'src') {
            scriptsDirs << new File(node.map.file.parentFile.parent, 'scripts')
        }
    } catch (Exception e) {
        logger.warn('Why do you store your add-on definition mind map in a root directory?\n', e)
    }
    scriptsDirs.each {
        if (it.exists()) {
            it.eachFileRecurse(FileType.FILES) { file ->
            def fileName = file.name
            if (filesToExclude.indexOf(fileName) == -1
                && scriptsNode.children.find { it.text.contains(fileName) } == null)
            {
                    scriptsNode.createChild(fileName)
            }
        }
    }
}
}

def scriptsNodesWithErrors = scriptsNode.children.findAll{ !it.plainText.matches('^\\w+\\.\\w+') }*.plainText
if (scriptsNodesWithErrors) {
    ui.errorMessage("Error: script(s) ${scriptsNodesWithErrors}\n don't end in a suffix like '.groovy' or contain illegal characters.")
    return
}
def scriptsNodesWithUnknownSuffixes = scriptsNode.children.findAll{ !it.plainText.matches('^\\w+\\.(groovy|js)') }*.plainText
if (scriptsNodesWithUnknownSuffixes) {
    ui.informationMessage("Error: script(s) ${scriptsNodesWithUnknownSuffixes} may not work\n since only '.groovy' and '.js' are guaranteed to work.")
}

scriptsNode.children.each {
    def scriptBaseName = it.plainText.replaceFirst('\\.\\w+$', '')
    def existingMenuTitleKey = it.attributes.getFirst('menuTitleKey')
    def menuTitleKey = existingMenuTitleKey ?: "addons.\${name}.${scriptBaseName}"
    createMissingAttributes(it, [
        [ 'menuTitleKey', menuTitleKey ]
        , ['menuLocation', '\${addonsMenu}/addons.\${name}']
        , ['executionMode', 'on_single_node']
        , 'keyboardShortcut'
        , ['execute_scripts_without_asking', 'true']
        , ['execute_scripts_without_file_restriction', 'true']
        , ['execute_scripts_without_write_restriction', 'false']
        , ['execute_scripts_without_exec_restriction', 'false']
        , ['execute_scripts_without_network_restriction', 'false']
    ])
    createMissingAttributes(englishTranslationsNode, [ [menuTitleKey, toPhrase(scriptBaseName)] ] )
}

filesToUninstall.addAll(scriptsNode.children.collect { "addons/\${name}/scripts/${it.plainText}" })

//
// ============ lib ============
//
def libNode = findOrCreate(root, 'lib', RIGHT)
libNode.note = withBody '''
    <p>
      An add-on may contain any number of nodes containing binary files (normally .jar files) to be added to the add-on's classpath.
    </p>
    <p>
      
    </p>
    <p>
      &#160;- The immediate child nodes contain the name of the file, e.g. 'mysql-connector-java-5.1.25.jar'). Put the file into a 'lib' subdirectory of the add-on base directory.
    </p>
    <p>
      
    </p>
    <p>
      &#160;- The child nodes of these nodes contain the actual files.
    </p>
    <p>
      
    </p>
    <p>
      &#160;- Any lib file will be extracted in &lt;installationbase&gt;/&lt;addonname&gt;/lib.
    </p>
    <p>
      
    </p>
    <p>
      &#160;- The files will be processed in the sequence as seen in the map.
    </p>
'''
if (node.map.file != null) {
    def libDir = new File(node.map.file.parent, 'lib')
    if (libDir.exists()) {
        libDir.eachFile(FileType.ANY) { file ->
            def libContentNode = libNode.children.find { it.text == file.name }
            if (file.isDirectory()) {
                addMessage("lib contains a directory ($file) - that's not supported!")
                if (libContentNode)
                    libContentNode.delete()
            } else if (libContentNode == null) {
                libNode.createChild(file.name)
            }
        }
    }
}
filesToUninstall.addAll(libNode.children.collect { "addons/\${name}/lib/${it.plainText}" })


//
// ============ zips ============
//
def zipsNode = findOrCreate(root, 'zips', RIGHT)
zipsNode.note = withBody '''
<p>
An add-on may contain any number of nodes containing zip files.
</p>
<p>

</p>
<p>
&#160;- The immediate child nodes contain a description of the zip. The devtools script releaseAddOn.groovy allows automatic zip creation if the name of this node matches a directory in the current directory.
</p>
<p>

</p>
<p>
&#160;- The child nodes of these nodes contain the actual zip files.
</p>
<p>

</p>
<p>
&#160;- Any zip file will be extracted in the &lt;installationbase&gt;. Currently, &lt;installationbase&gt; is always Freeplane's &lt;userhome&gt;, e.g. ~/.freeplane/1.3.
</p>
<p>

</p>
<p>
&#160;- The files will be processed in the sequence as seen in the map.
</p>
'''
if (node.map.file != null) {
    def zipsDir = new File(node.map.file.parent, 'zips')
    if (zipsDir.exists()) {
        zipsDir.eachFile(FileType.DIRECTORIES) { file ->
            if (zipsNode.children.find { it.text == file.name } == null)
                zipsNode.createChild(file.name)
        }
        zipsDir.eachFileRecurse(FileType.FILES) { file ->
            def fileName = file.path.substring(zipsDir.path.length() + 1)
            filesToUninstall << fileName
        }
    }
}

//
// ============ images ============
//
def imagesNode = findOrCreate(root, 'images', RIGHT)
imagesNode.note = withBody '''
    <p>
      An add-on may define any number of images as child nodes of the images node. The actual image data has to be placed as base64 encoded binary data into the text of a subnode.
    </p>
    <p>
      The images are saved to the <i>${installationbase}/resources/images</i>&#160;directory.
    </p>
    <p>
      
    </p>
    <p>
      The following images should be present:
    </p>
    <ul>
      <li>
        <i>${name}-icon.png</i>, like <i>oldicons-theme-icon.png</i>. This will be used in the app-on overview.
      </li>
      <li>
        <i>${name}-screenshot-1.png</i>, like <i>oldicons-theme-screenshot-1.png</i>. This will be used in the app-on details dialog. Further images can be included but they are not used yet.
      </li>
    </ul>
    <p>
      Images can be added automatically by releaseAddOn.groovy or must be uploaded into the map via the script <i>Tools-&gt;Scripts-&gt;Insert Binary</i>&#160;since they have to be (base64) encoded as simple strings.
    </p>
'''
filesToUninstall.addAll(
    imagesNode.children.collect {
        def image = it.plainText.replace('${name}', node.map.root['name'])
        "resources/images/${image}"
    }
)

//
// ============ actions ============
//
def actionsNode = findOrCreate(root, 'actions', RIGHT)
actionsNode.note = withBody '''
 Direct links to menu commands
'''
def actions = ['addons.devtools.checkAddOn','addons.devtools.releaseAddOn','addons.devtools.exportTranslations','addons.devtools.importTranslations']
actions.each{ acc ->
    def labelText = textUtils.getText(acc)
    if (!actionsNode.children.any{it.text == labelText}) {        
        def n = actionsNode.createChild(labelText)
        n.link.text = "menuitem:_${acc}_on_single_node"
    }
}

//
// ============ uninstallation rules ============
//
filesToUninstall = filesToUninstall.collect { '${installationbase}/' + it.replace('\\', '/') }
def actual = uninstallNode.attributes.values.collect{ it.trim() }
// ${name} might occur in current uninstallation rules and/or in the list of scripts
actual += actual*.replace('${name}', node.map.root['name'])
def missing = filesToUninstall - actual
if (missing) {
    def movedScriptsWarning = '<p><em>Note that scripts are installed now to addons/${name}/scripts/' +
            ' instead of scripts/. You may want to remove old entries.</em>.'
    def message = '<html><body><b>Add these files to the uninstallation rules?:</b><ul><li>' +
        missing.join('</li><li>') +'</li></ul>' +
        (actual.join().contains('${installationbase}/scripts') ? movedScriptsWarning : '') +
        '</body></html>'
    if (showConfirmDialog(message, "Uninstallation Rules") == JOptionPane.YES_OPTION) {
        missing.each { uninstallNode.attributes.add('delete', it) }
    }
}

//
// ============ inform about the outcome ============
//
def messagesString = messages.collect{ htmlUtils.htmlToPlain(it).replace('\n', '<br>') }.join('</li><li>')
ui.informationMessage('<html><body><b>Please review this changes carefully:</b><ul><li>' + messagesString +'</li></ul></body></html>')

