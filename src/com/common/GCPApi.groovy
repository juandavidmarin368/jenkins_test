package com.common

import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException

import com.common.Echo
import com.common.Shell
import com.common.Security

import groovy.json.JsonSlurperClassic

class GCPApi implements Serializable {

    private Echo echo
    private Shell shell
    private Security security


    def ctx

    /**
    * Creates an AWS object with specified credentials
    * @param accessKey AWS access key
    * @param secretKey AWS secret key
    */
    GCPApi(ctx) {
        this.ctx = ctx
        this.echo = new Echo(ctx)
        this.shell = new Shell(ctx)
        this.security = new Security(ctx)
    }

    /**
     * Initialize container/host with AWS credentials
     * @return Map of a result[exitcode, message]
    */




    Map containerRegistryLogin(path) {
       
        echo.enter "GCPApi::containerRegistryLogin"
        int exitcode = shell.execWithLog("""
            docker login -u _json_key --password-stdin https://gcr.io < ${path}/gcp-key.json
        """, "dev")
        echo.exit "GCPApi::containerRegistryLogin -- "
        return [exitcode: exitcode, message: "Container Registry Login"]
    }



    Map tagAndPushEcrImage(String environment, String localTag, String remoteTag) {
        echo.enter "AWSApi::tagAndPushEcrImage"
     
        String repository = "129271465998.dkr.ecr.us-east-1.amazonaws.com/backend_cargador_pedidos"
        String taggedImageName = "${repository}:${remoteTag}"
        int exitcode = shell.execForStatus("docker tag ${localTag} ${taggedImageName}")
        if(exitcode != 0) {
             echo.exit "AWSApi::tagAndPushEcrImage"
             return [exitcode: 1, message: "failed to tag ${taggedImageName} "]
        }
        
        exitcode = shell.execForStatus("docker push ${taggedImageName}")
        echo.exit "AWSApi::tagAndPushEcrImage"
        return [exitcode: exitcode, message: "successfully pushed image to ECR for ${repository} with tag ${remoteTag}"]
    }


    Map pushImageToContainerRegistry(String taggedImageName) {
        echo.enter "GCPApi::tagAndPushEcrImage"
           
        int exitcode = shell.execForStatus("docker push ${taggedImageName}")
        echo.exit "GCPApi::PushEcrImage"
        return [exitcode: exitcode, message: "successfully pushed image to CONTAINER REGISTRY for ${taggedImageName}"]
    }


    def findContainerRegistryImage(String dockerRepository, String serviVersion) {
        echo.enter "GCPApi::findContainerRegistryImage"
        boolean isImage = false
        String stdout = ctx.sh(returnStdout: true, script: """
            #!/bin/bash
            set +x
            value=\$(gcloud container images list-tags """+dockerRepository+""" | awk '\$2 != "TAGS" {print \$2}')
            echo \$value
            set -x
        """).trim()


        if(!stdout.equals("")){
            
            def values = stdout.split(',')
            for(imageVersion in values) {

                if(serviVersion.equals(imageVersion)){

                    return true;
                }
        
            }
            echo.exit "GCPApi::findEcrImageTags"
            return false;
           
        }else{
            return false;
            //ctx.sh("echo 'empty...' ")
          
        }
        
    }


    Map findPlainTextSecretFromSecretsManager(String gcloud_service_account, String environment,String path, String version_secret_manager) {
        echo.enter "GCLOUDApi::findPlainTextSecretFromSecretsManager"
        def stdout = shell.exec("""
                    set +x
                    gcloud auth activate-service-account ${gcloud_service_account} --key-file=${path}/gcp-key.json
                    gcloud config set project dev-test-29-${environment}
                    set -x
                """, true)

            stdout = shell.exec("""
                    set +x
                    gcloud secrets versions access ${version_secret_manager} --secret=${environment}-fitomedics_github_ssh_key
                    set -x
                """, true).trim()         
       
        if(!stdout) {
            return [exitcode: 1,  message: "Failed to find secret for ${environment}-fitomedics_github_ssh_key"]
        }
        echo.exit "GCLOUDApi::findPlainTextSecretFromSecretsManager"
        //ctx.sh("echo '${stdout}'")
        return [exitcode: 0, secret : "${stdout}"]
    }

    Map findPlainSystemManagerParameterStore_ssm(String ssmName, String environment) {
        echo.enter "AWSApi::findPlainSystemManagerParameterStore_ssm"
        
        //dev/githubtest
        def stdout = shell.exec("""
            set +x
            aws --profile ${environment} ssm get-parameter --name "${ssmName}" --with-decryption
            set -x
        """, true)
        def json = new JsonSlurperClassic().parseText(stdout)
       
        def secret = json['Parameter']
        if(!secret) {
            return [exitcode: 1,  message: "Failed to find secret for id ${secretArn}"]
        }
        echo.exit "AWSApi::findPlainSystemManagerParameterStore_ssm"
        return [exitcode: 0, secret : secret.get('Value')]
    }    

    Map findPlainTextDBCredentials(String secretArn, String environment) {
        echo.enter "AWSApi::findPlainTextDBCredentials"
        def stdout = shell.exec("""
            set +x
            aws --profile ${environment} secretsmanager get-secret-value --secret-id ${secretArn}
            set -x
        """, true)
        def json = new JsonSlurperClassic().parseText(stdout)
       
        def secret = json['SecretString']
        if(!secret) {
            return [exitcode: 1,  message: "Failed to find secret for id ${secretArn}"]
        }
        echo.exit "AWSApi::findPlainTextDBCredentials"
        return [exitcode: 0, secret : secret]
    }

    String getServiceVersion(String path){

                def script_output = ctx.sh(returnStdout: true, script: """
                    #!/bin/bash
                    set -e
                    set +x
                    value=`cat ${path}/published.json`
                    echo \$value
                    """)
                script_output = script_output.trim()
                String VAR_NAME = script_output
                def json = new JsonSlurperClassic().parseText(VAR_NAME)   
                ctx.println(path) 
                ctx.println(json['application'].version)         
        return json['application'].version    
    }
}
