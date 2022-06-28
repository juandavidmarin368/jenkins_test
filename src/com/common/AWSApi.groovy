package com.common

import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException

import com.common.Echo
import com.common.Shell
import com.common.Security

import groovy.json.JsonSlurperClassic

class AWSApi implements Serializable {

    private Echo echo
    private Shell shell
    private Security security


    def ctx
    String accessKey
    String secretKey

    /**
    * Creates an AWS object with specified credentials
    * @param accessKey AWS access key
    * @param secretKey AWS secret key
    */
    AWSApi(ctx, String accessKey, String secretKey) {
        this.ctx = ctx
        this.echo = new Echo(ctx)
        this.shell = new Shell(ctx)
        this.security = new Security(ctx)
        this.accessKey = accessKey
        this.secretKey = secretKey
    }

    /**
     * Initialize container/host with AWS credentials
     * @return Map of a result[exitcode, message]
    */
    Map initialize() {
        def credentialsFile = """\
        [prod]
        aws_access_key_id = ${this.accessKey}
        aws_secret_access_key = ${this.secretKey}
        region = us-east-1
        
        [dev]
        aws_access_key_id = ${this.accessKey}
        aws_secret_access_key = ${this.secretKey}
        region = us-east-1

        [uat]
        aws_access_key_id = ${this.accessKey}
        aws_secret_access_key = ${this.secretKey}
        region = us-east-1

        [terraform]
        aws_access_key_id = ${this.accessKey}
        aws_secret_access_key = ${this.secretKey}
        region = us-east-2
        """.stripIndent().trim()

        def profileFile = """\
        [profile app-uk]
        region = us-east-2
        role_arn = arn:aws:iam::3971114ds519:role/role1
        source_profile = terraform

        [profile app]
        region = us-east-2
        role_arn = arn:aws:iam::270852171d95:role/role2
        source_profile = terraform

        """.stripIndent().trim()


       int exitcode = shell.execForStatus("""
            set +x
            mkdir -p /root/.aws
            touch /root/.aws/credentials
            echo "${credentialsFile}" > /root/.aws/credentials
            echo "${profileFile}" > /root/.aws/config
            set -x
        """)
        return [exitcode: exitcode, message: "AWS access initialization"]
    }



    Map ecrLogin(String environment) {
        //String repository = Ref.REPOSITORIES[environment].repository
        String repository = "129271465998.dkr.ecr.us-east-1.amazonaws.com/backend_cargador_pedidos"
        echo.enter "AWSApi::ecrLogin"
        int exitcode = shell.execWithLog("""
            aws ecr --profile ${environment} get-login-password --region us-east-1 | docker login --username AWS --password-stdin ${repository}
        """, "${environment}-ecr-login")
        echo.exit "AWSApi::ecrLogin"
        return [exitcode: exitcode, message: "ECR login to ${repository} for environment: ${environment}"]
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


    Map pushEcrImage(String environment, String taggedImageName) {
        echo.enter "AWSApi::tagAndPushEcrImage"
           
        int exitcode = shell.execForStatus("docker push ${taggedImageName}")
        echo.exit "AWSApi::tagAndPushEcrImage"
        return [exitcode: exitcode, message: "successfully pushed image to ECR for ${taggedImageName}"]
    }


    def findEcrImageTags(String environment, String service, String imageQuery) {
        echo.enter "AWSApi::findEcrImageTags"
        String stdout = shell.exec("""
            aws --profile ${environment} ecr describe-images --repository-name ${service}
        """)
        def json = new JsonSlurperClassic().parseText(stdout)
        def imageDetails = json['imageDetails']
        boolean isImage = false
        String imageTagPrefix = environment
        for(imageDetail in imageDetails) {
            if(imageDetail['imageTags']) {
                for(imageTag in imageDetail['imageTags']) {
                    if(imageTag==imageQuery){
                        isImage = true
                        ctx.println("IMAGE FOUND!!!")    
                    }    
                    /*if(imageTag && imageTag.contains(imageTagPrefix)) {
                        imageTags << imageTag
                    }*/
                }
            }
        }
        
        echo.exit "AWSApi::findEcrImageTags"
        return isImage
    }


    Map findPlainTextSecretFromSecretsManager(String secretArn, String environment) {
        echo.enter "AWSApi::findPlainTextSecretFromSecretsManager"
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
        echo.exit "AWSApi::findPlainTextSecretFromSecretsManager"
        return [exitcode: 0, secret : secret]
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
                //ctx.println(json['application'].version)         
        return json['application'].version    
    }
}
