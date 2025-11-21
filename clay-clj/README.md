# Clay Clojure

A Clojure/ClojureScript port of the [Clay UI layout library](https://github.com/nicbarker/clay).

## Features

- 🎨 **Visual-first development**: Generate render commands → JSON → see results in browser immediately
- 🔄 **Hot reload**: Auto-reload on file changes with 1-second polling
- 🎯 **Test-driven**: Malli schemas for all data structures, RCF inline tests
- 🚀 **REPL-driven workflow**: Babashka tasks for instant feedback

## Quick Start

```bash
# Start REPL (Terminal 1)
bb repl

# Generate hello world and open browser (Terminal 2)
bb visual

# Or just generate JSON
bb hello

# Then serve files
bb serve
# Open http://localhost:8080/clay-visual-test.html
```

## Visual Test Keyboard Shortcuts

When viewing `clay-visual-test.html`:
- **R** - Reload render commands manually
- **A** - Toggle auto-reload (enabled by default)
- **C** - Clear cache and reload

## BB Tasks

```bash
bb repl              # Start nREPL on port 1234
bb gen               # Generate JSON from examples
bb serve             # Serve visual-test on :8080
bb visual            # Generate + serve + open browser
bb test              # Run all tests
bb test-watch        # Watch and run tests
bb sync-commands     # Update .claude/commands/
bb hello             # Run hello world example
bb layout-demo       # Run layout demo
bb clean             # Clean generated files
bb init-project      # Initialize project structure
```

## Project Structure

```
clay-clj/
├── deps.edn                  - Clojure dependencies
├── bb.edn                    - Babashka tasks
├── src/clay/
│   ├── core.cljc             - Main API
│   ├── schema.cljc           - Malli schemas
│   ├── element.cljc          - Element tree
│   ├── layout.cljc           - Layout algorithm
│   ├── render.cljc           - Render commands
│   └── json.clj              - JSON export
├── examples/
│   ├── hello.clj             - Hello world
│   └── layout_demo.clj       - Layout examples
├── visual-test/
│   ├── clay-visual-test.html - HTML renderer
│   └── render-commands.json  - Generated
└── scripts/
    └── sync_claude_commands.clj
```

## Development Workflow

1. **Start REPL**: `bb repl` (connect your editor to port 1234)
2. **Start server**: `bb serve` (in another terminal)
3. **Develop**: Edit code → eval in REPL → see results in browser
4. **Or use tasks**: `bb hello` / `bb visual` for quick testing

## Render Command Format

```json
{
  "renderCommands": [
    {
      "id": 1,
      "commandType": 1,
      "boundingBox": {"x": 50, "y": 50, "width": 400, "height": 80},
      "config": {
        "color": {"r": 168, "g": 66, "b": 28, "a": 255},
        "cornerRadius": {"topLeft": 12, "topRight": 12,
                        "bottomLeft": 12, "bottomRight": 12}
      }
    },
    {
      "id": 2,
      "commandType": 3,
      "boundingBox": {"x": 70, "y": 75, "width": 360, "height": 30},
      "text": "Hello from Clojure Clay!",
      "config": {
        "textColor": {"r": 255, "g": 255, "b": 255, "a": 255},
        "fontSize": 24
      }
    }
  ]
}
```

## License

Same as original Clay (Zlib License)
