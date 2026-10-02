package com.easycode.ide.engine

import com.easycode.ide.data.model.ExternalResource

object CodeCompiler {

    fun compile(
        html: String,
        css: String,
        js: String,
        resources: List<ExternalResource> = emptyList()
    ): String {
        val cssLinks = StringBuilder()
        val jsScripts = StringBuilder()
        var hasBabel = false

        resources.filter { it.isEnabled }.forEach { res ->
            if (res.isCss) {
                cssLinks.append("  <link rel=\"stylesheet\" href=\"${escapeUrl(res.url)}\" />\n")
            } else {
                if (res.url.contains("babel", ignoreCase = true)) {
                    hasBabel = true
                }
                jsScripts.append("  <script src=\"${escapeUrl(res.url)}\"></script>\n")
            }
        }

        val scriptTagOpen = if (hasBabel) {
            "<script type=\"text/babel\">"
        } else {
            "<script>"
        }

        return """<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes" />
  <title>EasyCode Playground</title>
$cssLinks
  <style>
$css
  </style>

  <script>
  (function() {
    function serialize(arg) {
      if (arg === null) return "null";
      if (arg === undefined) return "undefined";
      if (typeof arg === "object") {
        try {
          return JSON.stringify(arg);
        } catch(e) {
          return Object.prototype.toString.call(arg);
        }
      }
      return String(arg);
    }

    function send(level, args, line) {
      try {
        var msg = Array.prototype.slice.call(args).map(serialize).join(" ");
        if (window.EasyCodeBridge && window.EasyCodeBridge.postMessage) {
          window.EasyCodeBridge.postMessage(level, msg, line || 0);
        }
      } catch(e) {}
    }

    var _log = console.log;
    var _info = console.info;
    var _warn = console.warn;
    var _error = console.error;

    console.log = function() { send("LOG", arguments); _log && _log.apply(console, arguments); };
    console.info = function() { send("INFO", arguments); _info && _info.apply(console, arguments); };
    console.warn = function() { send("WARN", arguments); _warn && _warn.apply(console, arguments); };
    console.error = function() { send("ERROR", arguments); _error && _error.apply(console, arguments); };

    window.onerror = function(message, source, lineno, colno, error) {
      var cleanMsg = (error && error.message) ? error.message : String(message);
      send("ERROR", [cleanMsg], lineno || 0);
      return false;
    };

    window.addEventListener('unhandledrejection', function(event) {
      var reason = event.reason ? (event.reason.message || event.reason) : "Unhandled Promise Rejection";
      send("ERROR", [String(reason)], 0);
    });
  })();
  </script>
$jsScripts
</head>
<body>
$html

$scriptTagOpen
try {
$js
} catch(err) {
  if (window.EasyCodeBridge && window.EasyCodeBridge.postMessage) {
    window.EasyCodeBridge.postMessage("ERROR", err.message || String(err), 0);
  }
  console.error(err);
}
</script>
</body>
</html>"""
    }

    private fun escapeUrl(url: String): String {
        return url.replace("\"", "&quot;")
    }
}
