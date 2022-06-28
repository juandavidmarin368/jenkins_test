package com.common

class Echo implements Serializable {

    private def ctx

    Echo(def ctx) {
        this.ctx = ctx
    }

    void println(def msg) {
        ctx.echo msg
    }

    void info(def msg) {
        ctx.echo "ℹ️  INFO: ${msg}"
    }

    void failed(def msg) {
        ctx.echo "🛑  FAILED: ${msg}"
    }

    void error(def msg) {
        ctx.echo "❌  ERROR: ${msg}"
    }

    void error(Exception x, boolean stackTrace=true) {
        ctx.echo "❌  ERROR: ${x.getMessage()}"
        if (stackTrace) {
            def sw = new StringWriter()
            def pw = new PrintWriter(sw)
            x.printStackTrace(pw)
            ctx.echo sw.toString()
        }
    }

    void warn(def msg) {
        ctx.echo "⚠️  WARN: ${msg}"
    }

    void success(def msg) {
        ctx.echo "✅  SUCCESS: ${msg}"
    }

    void found(def msg) {
        ctx.echo "🔍  FOUND: ${msg}"
    }

    void debug(def msg) {
        ctx.echo "🐞  DEBUG: ${msg}"
    }

    void enter(def msg) {
        ctx.echo "➡️  ENTER: ${msg}"
    }

    void exit(def msg) {
        ctx.echo "↩️  EXIT: ${msg}"
    }

    void exit(Map p) {
        if (p.exitcode == null) {
            ctx.echo "↩️  EXIT: ${p.msg}"
        } else if (p.exitcode == 0) {
            ctx.echo "↩️  EXIT: ${p.msg} - exitcode=${p.exitcode} ✅"
        } else {
            ctx.echo "↩️  EXIT: ${p.msg} - exitcode=${p.exitcode} ❌"
        }
    }

    void hr(int length=60) {
        ctx.echo "="*length
    }

    void table(String csvHeader, List<Integer> w, Map<String,Map> data) {
        ctx.echo ListUtils.formatAsTable(csvHeader, w, data)
    }
}
