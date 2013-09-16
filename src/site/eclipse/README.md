# Eclipse Preferences

If you use Eclipse as your IDE, these settings and files can be imported to apply
this project's coding conventions to your workspace.

### Under Java -> Code Style:

1. Check "Add @Override ...", and leave other checkboxes unchecked.
2. Make sure "Exception variable name" is e.
3. Import and apply the Clean-up, Code Templates and Formatter preference files from fcrepo4/etc.

### Under Java -> Editor -> Save Actions:

1. Check "Perform the selected actions on save".
2. If you check "Format source code", also choose to "Format edited lines" only.
2. Check "Additional actions".
3. Click "Configure...".
   + Under the "Code Organizing" tab, check "Remove trailing whitespace" and "All lines", as well as "Correct indentation".
   + Under the "Unnecessary Code" tab, check "Remove unused imports". 
   + Under the "Missing Code" tab, ensure that "Add missing Annotations" and '@Override" are checked.
   + Under the "Code Style" tab, check "Use blocks in if/while/for/do statements" and "Always", as well as "Use modifier 'final' where possible and 'Parameter' and 'Local variable'.

### Under XML -> XML Files -> Editor:

1. Select to "Indent using spaces" with "Indentation size" of 2.
2. Set line width to 80 characters.

### Use hints

In order to autocreate Javadocs on types, ensure that "Generate comments" is checked in the new type dialog.
