package com.common

import com.cloudbees.groovy.cps.NonCPS
import com.cloudbees.plugins.credentials.Credentials
import jenkins.model.Jenkins
import com.cloudbees.plugins.credentials.CredentialsProvider

class Security {

    private def ctx
    private Echo echo
    String VAULT_SERVICE_URL = "http://vault-client.k8s.lab.vi.local"

    Security(ctx) {
        this.ctx = ctx
        this.echo = new Echo(ctx)
        
    }

    @NonCPS
    def getJenkinsSecret(username) {
        def creds = CredentialsProvider.lookupCredentials(
            com.cloudbees.plugins.credentials.common.StandardUsernameCredentials.class,
            Jenkins.instance
        );
        def password = ''

        for (c in creds) {
            if (c.username == "${username}") {
                password = c.password.getPlainText()
                return password
            }
        }
        echo.failed "getJenkinsSecret: failed to find password secret for ${username}"
        return ""
    }

    // get VW PEM Secret from vault service
    // perhaps bug in httpRequest. if consoleLogResponseBody is set to false (to hide return data), resp is null
    String getVwPemSecret() {
        def resp = ctx.httpRequest httpMode: 'GET',
                url: "${VAULT_SERVICE_URL}/verificationToken",
                acceptType: 'TEXT_PLAIN', contentType: 'TEXT_PLAIN',
                consoleLogResponseBody: false,
                ignoreSslErrors: true,
                responseHandle: 'STRING', validResponseCodes: '200:599'
        if (resp.status != 200) {
            echo.failed "getVwPemSecret: failed to retrieve verification token"
            return ""
        }
        def verificationToken = resp.content

        resp = ctx.httpRequest httpMode: 'GET',
                url: "${VAULT_SERVICE_URL}/secret?policy=vwpem&token=${verificationToken}&secretName=pem",
                acceptType: 'TEXT_PLAIN', contentType: 'TEXT_PLAIN',
                consoleLogResponseBody: false,
                ignoreSslErrors: true,
                responseHandle: 'STRING', validResponseCodes: '200:599'
        if (resp.status != 200) {
            echo.failed "getVwPemSecret: failed to retrieve pem secret"
            return ""
        }

        return resp.content
    }


    String findRootPassword(){
        def creds = CredentialsProvider.lookupCredentials(
            Credentials.class,
            Jenkins.instance
        )
        for (int i = 0; i < creds.size(); i++){
            def c = creds[i]
            if (c instanceof org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl && c.id == 'aruba-root-password'){
                return "${c.secret}"
            }
        }
        echo.failed "could not find credential secret for aruba-root-password"
        return ""
    }

    @NonCPS
    def findCredentialSecret(key) {
        def creds = CredentialsProvider.lookupCredentials(
            Credentials.class,
            Jenkins.instance
        )
        for (int i = 0; i < creds.size(); i++){
            def c = creds[i]
            if (c instanceof org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl && c.id == key){
                return "${c.secret}"
            }
        }
        echo.failed "could not find credential secret for ${key}"
        return ""
    }
    
}
