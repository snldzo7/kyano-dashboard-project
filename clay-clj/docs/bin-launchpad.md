Great question! Here's exactly what you'd need to teach Claude Code to convert any project into a Launchpad-based setup:

## **Project Analysis Phase**

### **1. Detect Project Structure**

```clojure
;; What to scan for:
- Existing deps.edn location(s)
- Shadow-cljs.edn presence
- Source directories (src/, test/, dev/)
- Is it monorepo or single project?
- Are there subprojects? (multiple deps.edn files)
- Backend vs frontend vs full-stack
- Existing .gitignore
```

### **2. Detect Current Tooling**

```clojure
;; Check for:
- Existing nREPL configuration
- Editor-specific configs (.dir-locals.el, .vscode/, .calva/)
- Existing startup scripts (bin/, scripts/)
- Environment files (.env*)
- Docker/docker-compose
- Package managers (npm, yarn, pnpm)
```

---

## **Decision Tree**

### **Monorepo Detection:**

```
IF multiple deps.edn files exist in subdirectories:
  → Create top-level orchestration deps.edn
  → Use :local/root for each subproject
ELSE:
  → Single project setup
```

### **Shadow-CLJS Detection:**

```
IF shadow-cljs.edn exists:
  → Extract build IDs
  → Configure :launchpad/shadow-build-ids
  → Determine which builds to auto-watch
ELSE:
  → Skip shadow configuration
```

### **Editor Detection:**

```
IF .dir-locals.el exists:
  → User likely uses Emacs
  → Suggest --emacs flag in deps.local.edn
IF .vscode/ exists:
  → User likely uses VS Code/Calva
  → Suggest --cider-nrepl flag
ELSE:
  → Generic setup
```

---

## **File Creation Rules**

### **1. bb.edn** (Always Create)

```clojure
{:deps {com.lambdaisland/launchpad {:mvn/version "0.34.152-alpha"}}}
```

### **2. bin/launchpad** (Always Create)

```bash
#!/usr/bin/env bb
(require '[lambdaisland.launchpad :as launchpad])

;; OPTIONAL CUSTOMIZATIONS:
;; - Java version check if project specifies minimum
;; - Docker compose startup if docker-compose.yml exists
;; - npm install if package.json exists

(launchpad/main {})
```

**Make executable:**

```bash
chmod +x bin/launchpad
```

### **3. deps.edn Modification**

**For Single Project:**

```clojure
;; Read existing deps.edn
;; Add to it (don't replace):

{:deps {<existing deps>}
 :paths [<existing paths>]
 
 ;; PRESERVE existing aliases
 :aliases {<existing aliases>
         
           ;; ADD new ones if needed:
           ;; :dev {:extra-paths ["dev"]
           ;;       :extra-deps {...dev tools...}}
           }}
```

**For Monorepo (NEW top-level deps.edn):**

```clojure
{:deps 
 {;; Recommended dev tools (OPTIONAL, ask user):
  djblue/portal {:mvn/version "RELEASE"}
  ;; com.github.jpmonettas/flow-storm-dbg {:mvn/version "RELEASE"}
  }
 
 :aliases
 {;; Create alias for each subproject found:
  :backend {:deps {<org>/<backend> {:local/root "backend"}}}
  :frontend {:deps {<org>/<frontend> {:local/root "frontend"}}}
  ;; etc...
  }}
```

### **4. deps.local.edn Template**

```clojure
;; Create template, DON'T commit actual file

;; Template content for docs/README:
{;; Personal dependency overrides
 :deps {}
 
 ;; Personal aliases
 :aliases {}
 
 ;; Default aliases to always include
 ;; :launchpad/aliases [:backend :frontend]
 
 ;; Personal preferences (uncomment and customize)
 ;; :launchpad/main-opts ["--emacs"]  ; or ["--cider-nrepl"] for VS Code
 
 ;; Tool options
 ;; :launchpad/options {:portal true
 ;;                     :nrepl-port 7888}
 
 ;; Shadow builds to start automatically
 ;; :launchpad/shadow-build-ids [:app]
 
 ;; Shadow builds to auto-connect
 ;; :launchpad/shadow-connect-ids [:app]
 }
```

### **5. .gitignore Updates**

```bash
# Add if not present:
deps.local.edn
.env.local
.nrepl-port
.shadow-cljs/
```

### **6. .env Template**

```bash
# Create if doesn't exist
# Shared environment variables (committed)

# Example:
# API_URL=http://localhost:3000
# LOG_LEVEL=info
```

### **7. dev/user.clj** (Optional)

```clojure
(ns user
  "Development namespace for REPL-driven development"
  (:require
    ;; Add requires based on project type
    [clojure.tools.namespace.repl :as repl]))

;; System management (if project has components/state)
;; (defonce system (atom nil))

;; (defn go []
;;   "Start the system"
;;   (reset! system (start-system!))
;;   :started)

;; (defn reset []
;;   "Reset the system"
;;   (repl/refresh :after 'user/go))

(comment
  ;; REPL scratchpad
  
  )
```

---

## **Configuration Extraction Logic**

### **From shadow-cljs.edn:**

```clojure
;; Parse shadow-cljs.edn
;; Extract: [:builds :app :build-id]
;; For each build, determine if it should auto-start:

:launchpad/shadow-build-ids 
  ;; Usually :app, :test, :admin, etc.
  [:app]

:launchpad/shadow-connect-ids
  ;; Usually just main app
  [:app]
```

### **From Existing nREPL Config:**

```clojure
;; If user has .nrepl-port file or middleware configured:
;; - Extract port number → :launchpad/options {:nrepl-port N}
;; - Extract middleware → likely already handled by Launchpad
```

---

## **Project Type Templates**

### **Backend Only (CLJ)**

```bash
bin/launchpad backend
```

deps.edn:

```clojure
{:aliases {:backend {:deps {...}}}}
```

### **Frontend Only (CLJS + Shadow)**

```bash
bin/launchpad frontend
```

Needs shadow-cljs.edn:

```clojure
{:launchpad/shadow-build-ids [:app]}
```

### **Full-Stack (CLJ + CLJS)**

```bash
bin/launchpad backend frontend
```

Both aliases + shadow config.

### **Monorepo Multi-Service**

```
project/
├── service-a/deps.edn
├── service-b/deps.edn
├── service-c/deps.edn
└── shared/deps.edn
```

Top-level:

```clojure
{:aliases
 {:svc-a {:deps {my.app/service-a {:local/root "service-a"}}}
  :svc-b {:deps {my.app/service-b {:local/root "service-b"}}}
  :svc-c {:deps {my.app/service-c {:local/root "service-c"}}}
  :shared {:deps {my.app/shared {:local/root "shared"}}}}}
```

---

## **Validation Checklist**

After conversion, Claude Code should verify:

```bash
✓ bb.edn exists with launchpad dep
✓ bin/launchpad exists and is executable
✓ deps.edn has proper structure
✓ deps.local.edn is in .gitignore
✓ If monorepo: all subprojects referenced
✓ If shadow: build IDs extracted
✓ .gitignore updated
✓ Can run: bb bin/launchpad --help
```

---

## **Interactive Questions to Ask**

Claude Code should ask user:

1. **"Is this a monorepo or single project?"**
   * Auto-detect but confirm
2. **"Which subprojects should be included by default?"**
   * For monorepo setups
3. **"Do you want to include Portal for data inspection?"**
   * Add to top-level deps
4. **"Which editor do you use?"**
   * Emacs → suggest `--emacs`
   * VS Code → suggest `--cider-nrepl`
   * Other → generic setup
5. **"Should we create a dev/user.clj namespace?"**
   * For system management
6. **"Do you want environment variable support?"**
   * Create .env template
7. **"Which Shadow builds should auto-start?"**
   * If shadow-cljs.edn detected

---

## **Edge Cases to Handle**

### **Existing user.clj**

* Don't overwrite
* Suggest additions in comments

### **Existing bin/ directory**

* Don't overwrite scripts
* Create bin/launchpad only

### **Complex deps.edn**

* Preserve all existing config
* Only add/modify launchpad-specific parts

### **Multiple Shadow configs**

* Handle multiple shadow-cljs.edn files
* Ask which to use

### **No deps.edn**

* Create from scratch
* Ask for group/artifact naming

---

## **Example Conversion Script Structure**

```clojure
(defn convert-to-launchpad [project-root]
  (let [analysis (analyze-project project-root)
        decisions (ask-user-decisions analysis)
        config (generate-config analysis decisions)]
  
    ;; 1. Create bb.edn
    (create-bb-edn project-root)
  
    ;; 2. Create bin/launchpad
    (create-launchpad-script project-root config)
  
    ;; 3. Update/create deps.edn
    (if (:monorepo? analysis)
      (create-top-level-deps-edn project-root config)
      (update-existing-deps-edn project-root config))
  
    ;; 4. Create deps.local.edn template
    (create-deps-local-template project-root config)
  
    ;; 5. Update .gitignore
    (update-gitignore project-root)
  
    ;; 6. Optionally create .env
    (when (:env-support? decisions)
      (create-env-template project-root))
  
    ;; 7. Optionally create dev/user.clj
    (when (:create-user-ns? decisions)
      (create-user-namespace project-root config))
  
    ;; 8. Validate
    (validate-setup project-root)
  
    ;; 9. Show usage instructions
    (print-instructions config)))
```

---

## **Final Output**

After conversion, generate README snippet:

```markdown
## Launchpad Setup

Start the REPL:
```bash
bin/launchpad <aliases>
```

Examples:

```bash
# Backend only
bin/launchpad backend

# Full-stack
bin/launchpad backend frontend

# With Emacs
bin/launchpad backend frontend --emacs
```

Personal configuration:

```bash
# Copy template and customize
cp deps.local.edn.template deps.local.edn
# Edit deps.local.edn (gitignored)
```

Available flags:

- `--emacs` - Emacs/CIDER support
- `--cider-nrepl` - VS Code/Calva support
- `--portal` - Enable Portal
- `--verbose` - Debug output

```


---


**TL;DR** - Claude Code needs:


1. **Project structure detection** (files, dirs, configs)
1. **Decision tree** (monorepo vs single, editor, features)
1. **File templates** (bb.edn, bin/launchpad, deps configs)
1. **Merge strategies** (don't clobber existing configs)
1. **Validation rules** (verify it works)
1. **User guidance** (what to do next)


Would you like me to create an actual conversion script/tool for Claude Code to use?
```
