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
        ctx.echo "âšī¸  INFO: ${msg}"
    }

    void failed(def msg) {
        ctx.echo "đ  FAILED: ${msg}"
    }

    void error(def msg) {
        ctx.echo "â  ERROR: ${msg}"
    }

    void error(Exception x, boolean stackTrace=true) {
        ctx.echo "â  ERROR: ${x.getMessage()}"
        if (stackTrace) {
            def sw = new StringWriter()
            def pw = new PrintWriter(sw)
            x.printStackTrace(pw)
            ctx.echo sw.toString()
        }
    }

    void warn(def msg) {
        ctx.echo "â ī¸  WARN: ${msg}"
    }

    void success(def msg) {
        ctx.echo "â  SUCCESS: ${msg}"
    }

    void found(def msg) {
        ctx.echo "đ  FOUND: ${msg}"
    }

    void debug(def msg) {
        ctx.echo "đ  DEBUG: ${msg}"
    }

    void enter(def msg) {
        ctx.echo "âĄī¸  ENTER: ${msg}"
    }

    void exit(def msg) {
        ctx.echo "âŠī¸  EXIT: ${msg}"
    }

    void exit(Map p) {
        if (p.exitcode == null) {
            ctx.echo "âŠī¸  EXIT: ${p.msg}"
        } else if (p.exitcode == 0) {
            ctx.echo "âŠī¸  EXIT: ${p.msg} - exitcode=${p.exitcode} â"
        } else {
            ctx.echo "âŠī¸  EXIT: ${p.msg} - exitcode=${p.exitcode} â"
        }
    }

    void hr(int length=60) {
        ctx.echo "="*length
    }

    void table(String csvHeader, List<Integer> w, Map<String,Map> data) {
        ctx.echo ListUtils.formatAsTable(csvHeader, w, data)
    }
}
