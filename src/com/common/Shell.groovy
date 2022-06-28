package com.common

class Shell implements Serializable {

    def ctx
    Echo echo

    Shell(def ctx) {
        this.ctx = ctx
        this.echo = new Echo(ctx)
    }

    def env() {
        echo.hr()
        ctx.sh 'env | sort'
    }

    String cwd() {
        String dir = this.exec('pwd')
        echo.info "CWD: 📂 ${dir}"
        return dir.trim()
    }

    String hostname() {
        return exec("hostname")
    }

    def exec(String script, boolean suppressOutput = false) {
        def stdout = ctx.sh script: script, returnStdout: true
        echo.info "script: ${script}"
        if (!suppressOutput) {
            echo.info "${stdout}"
        }
        return stdout?.trim()
    }

    int execForStatus(String script) {
        echo.enter "Shell::execForStatus"
        int status = ctx.sh script: script, returnStatus: true
        echo.exit msg: "Shell::execForStatus", exitcode: status
        return status
    }

    /**
     * executes script by writing it to a local directory and then executing it
     * this is required so the output can be teed to both a log file and the Jenkins stdout
     *   and also to retrieve the status of the executed script (and not the tee)
     * both the log and the status will be archived in Jenkins
     */
    int execWithLog(String script, String scriptName) {
        echo.enter "Shell::execWithLog: ${scriptName}" // \n${script}"
        String key = "jnkns-${scriptName}"
        String fileName = "${key}.sh"
        // bash should be on first line, no escaping. do not change this for pretty formatting
        String script0 = """#!/bin/bash
        set -euxo pipefail
        ${script.trim().stripIndent()}
        """.stripIndent()
        ctx.writeFile file: fileName, text: script0
        // the given script should be executed, teed to both console stdout and to .log file for easier debugging
        // if script | tee is run directly via ctx.sh, the returnStatus will always be 0, the result of tee, which is always successful
        // This status is not desirable, as what we need is the status of the script not that of tee
        // Jenkins by default runs only sh, so PIPESTATUS is not available, resulting in Bad Substituion errors
        // Solution is to wrap the original script with a bash to tee the log and get the PIPESTATUS[0] - this will be the given script's exitcode
        // This may be avoided if Jenkins > Configure System > Shell is set to /bin/bash, but thats a global change for all builds
        String bashWrapper = """#!/bin/bash
        ./${fileName} 2>&1 | tee ${key}.log && echo \${PIPESTATUS[0]} > ${key}.exitcode.log
        """
        ctx.sh "chmod +x ${fileName}"
        // the exitcode of the bashScript is irrelevant, but capture it just in case
        int exitcodeWrapper = ctx.sh script: bashWrapper, returnStatus: true
        // read the given script's exitcode back
        if (ctx.fileExists("${key}.exitcode.log")) {
            int exitcode = ctx.readFile("${key}.exitcode.log").toInteger()
            echo.exit msg: "Shell:execWithLog: ${fileName}", exitcode: exitcode
            return exitcode
        }
        echo.warn "Shell:execWithLog: file ${key}.exitcode.log not found; wrapper cmd exitcode=${exitcodeWrapper}"
        return exitcodeWrapper
    }

    def execFile(String scriptName, log) {
        def stdout = ctx.sh script: scriptName, returnStdout: true
        return stdout
    }

    def copyFiles(String src, String dest) {
        this.exec("cp -r -H ${src} ${dest}")
    }

    def ls(String dir=".") {
        return this.exec("ls -lart ${dir}")
    }
}
