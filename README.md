# EBNF IntelliJ Plugin

A comprehensive IntelliJ IDEA plugin for working with [Extended Backus-Naur Form (EBNF)](https://en.wikipedia.org/wiki/Extended_Backus%E2%80%93Naur_form) grammars. This plugin provides rich language support for EBNF files, making it easier to create, edit, and analyze formal language specifications.

[![Kotlin](https://img.shields.io/badge/kotlin-1.9.0-blue.svg)](https://kotlinlang.org)
[![IntelliJ Platform](https://img.shields.io/badge/intellij%20platform-2023.2-blue.svg)](https://www.jetbrains.org/intellij/sdk/docs/basics/getting_started.html)

![Build](https://github.com/itworks99/ebnf-intellij/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

## Features

The EBNF IntelliJ Plugin provides a rich set of features for working with EBNF grammar files:

### Core Language Support
- **Syntax Highlighting**: Colorful highlighting of EBNF syntax elements
- **Error Detection**: Real-time validation of EBNF syntax
- **Code Completion**: Smart completion for rule references and EBNF operators
- **Code Folding**: Collapse rule definitions and comment blocks for better readability

### Navigation and Structure
- **Structure View**: Hierarchical view of grammar rules for easy navigation
- **Find Usages**: Quickly locate all references to a specific rule
- **Rename Refactoring**: Safely rename rules with automatic updates to all references

### Analysis and Validation
- **Syntax Validation**: Detect syntax errors and undefined rules
- **Semantic Validation**: Identify recursive rules without base cases and unreachable rules
- **Inspections**: Detect common issues like redundant parentheses and unused rules
- **Quick Fixes**: Automatically fix common problems with a single click

### Visualization
- **Railroad Diagrams**: Visualize grammar rules as railroad diagrams
- **Syntax Tree Viewer**: Explore the parse tree of your grammar
- **Test Data Generator**: Generate valid strings from your grammar for testing

### Project Integration
- **Build Integration**: Validate EBNF files during project build
- **Settings**: Configure validation rules and visualization preferences

<!-- Plugin description -->
The EBNF IntelliJ Plugin provides comprehensive support for Extended Backus-Naur Form (EBNF) grammar files in IntelliJ IDEA. EBNF is a notation technique for context-free grammars, commonly used to describe the syntax of languages, protocols, and file formats.

This plugin enhances your EBNF editing experience with:
- Syntax highlighting and error detection
- Code completion for rule references and EBNF operators
- Structure view for easy navigation
- Find usages and rename refactoring
- Code folding for rules and comments
- Syntax and semantic validation
- Railroad diagram visualization
- Syntax tree viewer
- Test data generation
- Build integration and customizable settings

Whether you're designing a new programming language, documenting a file format, or teaching formal language theory, this plugin makes working with EBNF grammars more productive and enjoyable.
<!-- Plugin description end -->

## Usage Examples

### Creating an EBNF Grammar

Create a new file with the `.ebnf` extension and start defining your grammar rules:

```ebnf
(* Simple arithmetic expression grammar *)
expression = term, { ("+" | "-"), term } ;
term = factor, { ("*" | "/"), factor } ;
factor = number | identifier | "(", expression, ")" ;
number = digit, { digit } ;
digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
identifier = letter, { letter | digit } ;
letter = "A" | "B" | "C" | "D" | "E" | "F" | "G"
       | "H" | "I" | "J" | "K" | "L" | "M" | "N"
       | "O" | "P" | "Q" | "R" | "S" | "T" | "U"
       | "V" | "W" | "X" | "Y" | "Z" 
       | "a" | "b" | "c" | "d" | "e" | "f" | "g"
       | "h" | "i" | "j" | "k" | "l" | "m" | "n"
       | "o" | "p" | "q" | "r" | "s" | "t" | "u"
       | "v" | "w" | "x" | "y" | "z" ;
```

### Visualizing Grammar Rules

1. Open an EBNF file in the editor
2. Click on a rule name
3. Open the "EbnfRailroadDiagrams" tool window to see a railroad diagram of the selected rule

### Generating Test Data

1. Open an EBNF file in the editor
2. Open the "EbnfTestDataGenerator" tool window
3. Select a rule from the dropdown
4. Click "Generate" to create sample strings that match the selected rule

### Validating Your Grammar

The plugin automatically validates your grammar as you type, highlighting:
- Syntax errors
- Undefined rule references
- Unused rules
- Recursive rules without base cases

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "ebnf-intellij"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

- Manually:

  Download the [latest release](https://github.com/itworks99/ebnf-intellij/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
