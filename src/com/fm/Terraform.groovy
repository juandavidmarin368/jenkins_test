package com.fm

import com.common.Security
import com.common.AWSApi
import com.common.Ref
import com.common.DockerBuild
import groovy.json.JsonSlurperClassic
import com.cloudbees.groovy.cps.NonCPS
import org.jenkinsci.plugins.docker.workflow.*
import groovy.json.JsonSlurper
import com.common.Shell
import com.common.Echo

class Terraform implements Serializable{

    String temporal = "JUANDAMALO."
    def ctx
    Security security
    AWSApi awsApi
    String accessKey
    String secretKey
    private Shell shell
    private Echo echo
    String githubSecretKey
    String gitRepoUrl
    String environment


    Terraform(ctx,environment,gitRepoUrl) {
        this.ctx = ctx
        this.environment = environment
        this.security = new Security(ctx)
        this.gitRepoUrl = gitRepoUrl
        this.shell = new Shell(ctx)
        this.echo = new Echo(ctx)
        
        this.accessKey = security.findCredentialSecret("FM_ACCESS_KEY") //CREATE A SECRET TEXT IN JENKINS CREDENTIALS, AND GIVE IT AN ID TO GET IT, IN THIS CASE THE ID WAS FM_ACCESS_KEY
        this.secretKey = security.findCredentialSecret("FM_SECRET_KEY")
        

        this.awsApi = new AWSApi(ctx,accessKey,secretKey)
        
    }

    Map initialize() {
        def credentialsFile = """\
        [default]
        aws_access_key_id = ${this.accessKey}
        aws_secret_access_key = ${this.secretKey}
        region = us-east-1
        
        [dev]
        aws_access_key_id = ${this.accessKey}
        aws_secret_access_key = ${this.secretKey}
        region = us-east-1

        [terraform]
        aws_access_key_id = ${this.accessKey}
        aws_secret_access_key = ${this.secretKey}
        region = us-east-2
        """.stripIndent().trim()

        def profileFile = """\
        [profile uat]
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


    def checkoutBranch(){


            ctx.cleanWs()   
            String temp = """\
            #!/bin/bash -ex
            set +x
            mkdir -p ~/.ssh     
            echo "Host *" > ~/.ssh/config     
            echo " StrictHostKeyChecking no" >> ~/.ssh/config
            echo "${this.githubSecretKey}" > aws_terraform.pem
            chmod 400 aws_terraform.pem       
            eval "\$(ssh-agent -s)" && ssh-add aws_terraform.pem
            git clone ${this.gitRepoUrl}
            pwd
            ls   
            """.stripIndent() 
            ctx.sh temp
            
    }    


    def gettingSSMSecretKey(){

            ctx.cleanWs()    
            
            
            String options = "-u 0 -v /var/run/docker.sock:/var/run/docker.sock -v /opt/jenkins_home:/root"

            def buildDocker = ctx.docker.image("builder:v1")
            buildDocker.inside(options) {

                Map result = awsApi.initialize()
                
                result = awsApi.findPlainSystemManagerParameterStore_ssm(Ref.GITHUB_AWS_ARN_SECRETS."${environment}".arn,this.environment)         
     
                this.githubSecretKey = result.get('secret')    
                //ctx.println(result.get('secret'))    

          }  

    }

    def deployTerraform(){

            String options = "-u 0 -v /var/run/docker.sock:/var/run/docker.sock -v /opt/jenkins_home:/root"

            def buildDocker = ctx.docker.image("terraform:1.1.7")
            buildDocker.inside(options) {

                this.accessKey = security.findCredentialSecret("CLOUDGURU_ACCESS_KEY") //CREATE A SECRET TEXT IN JENKINS CREDENTIALS, AND GIVE IT AN ID TO GET IT, IN THIS CASE THE ID WAS FM_ACCESS_KEY
                this.secretKey = security.findCredentialSecret("CLOUDGURU_SECRET_KEY")
                this.awsApi = new AWSApi(ctx,accessKey,secretKey)    
                Map result = initialize()

                /*ctx.sh"""
                cat ~/.aws/credentials
                aws sts get-caller-identity
                """*/
                ctx.sh"""
                      export environment=development
                      cd terraform_infrastructure/ && ./deploy_terrform.sh
                    
                      """        
                       

            }  

    }

}    