package com.fm

import com.common.Security
import com.common.AWSApi
import com.common.Ref
import com.common.DockerBuild
import groovy.json.*
import com.cloudbees.groovy.cps.NonCPS
import org.jenkinsci.plugins.docker.workflow.*
import groovy.json.JsonSlurper
import com.common.Shell
import com.common.Echo
import com.fm.Task1


class Kubernetes implements Serializable{

    private static final long serialVersionUID
    private Shell shell
    private Echo echo
    def ctx

    Task1 task1
    String environment
    String serviceName
    String serviceType
    String dbname
    String dbhost
    String dbusername
    String dbpassword
    String dbport
    String githubSecretKey
    String kubeconfig

    Security security
    AWSApi awsApi

    String accessKey
    String secretKey

    Kubernetes(ctx,environment,serviceName,serviceType,dbname) {

        this.ctx = ctx
        this.shell = new Shell(ctx)
        this.echo = new Echo(ctx)

        this.environment = environment
        this.serviceName = serviceName
        this.serviceType = serviceType
        this.dbname = dbname

        this.task1 = new Task1(ctx,this.environment)

        this.security = new Security(ctx)
        this.accessKey = security.findCredentialSecret("FM_ACCESS_KEY") //CREATE A SECRET TEXT IN JENKINS CREDENTIALS, AND GIVE IT AN ID TO GET IT, IN THIS CASE THE ID WAS FM_ACCESS_KEY
        this.secretKey = security.findCredentialSecret("FM_SECRET_KEY")
        this.awsApi = new AWSApi(ctx,accessKey,secretKey)

    }

    def gettingDBCredentials(){

        def json = task1.getDBCredentials(this.environment)        
        this.dbhost     = json['host']
        this.dbusername = json['username']
        this.dbpassword = json['password']
        this.dbport     = json['port']

    }

    def getSSMSecretKey(){

        this.githubSecretKey = task1.gettingSSMSecretKey();    
        //ctx.println("${this.githubSecretKey}")

    }

    def getKubernetesConfig(){

        this.kubeconfig = task1.getKubernetesConfig()
        //ctx.println("${this.kubeconfig}")
        
    }

    def updateKustomizeManifests(){

        String githubRepo          = Ref.GITHUB_REPOSITORIES_KUSTOMIZE."${this.environment}".reponame
        String kustomizeRepoFolder = "kustomize_manifests"
        String githubUsername      = Ref.GITHUB_SSH_KEY_SECRET_MANAGER."${this.environment}".username
        String branchName          = Ref.GITHUB_REPOSITORIES_KUSTOMIZE."${this.environment}".branchName    
        

        String options = "-u 0 -v /var/run/docker.sock:/var/run/docker.sock"

            def buildDocker = ctx.docker.image("ansible:v3")
            buildDocker.inside(options) {


            Map result = awsApi.initialize()
            ctx.println(result)        

            int exitcode = shell.execForStatus("""
                    #!/bin/bash -ex
                    set +x
                    mkdir -p ~/.ssh     
                    echo "Host *" > ~/.ssh/config     
                    echo "StrictHostKeyChecking no" >> ~/.ssh/config
                    echo "${this.githubSecretKey}" > aws_terraform.pem
                    chmod 400 aws_terraform.pem
                    echo "${this.kubeconfig}" > /root/kubeconfig 
                    eval "\$(ssh-agent -s)" && ssh-add aws_terraform.pem
                    git clone ${githubRepo}
                    pwd
                    ls 

                    cd kustomize_manifests/cargador_pedidos 
                    kustomize build overlays/dev > overlays/dev/dev.yaml
                    
                    export AWS_PROFILE=dev
                    export KUBECONFIG=/root/kubeconfig
                    export PATH=/root/bin:/usr/local/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/root/bin
                    
                    aws sts get-caller-identity
                    kubectl apply -f overlays/dev/dev.yaml

                    if [ "${this.serviceType}" = "frontend" ]; then
                        echo \$(jq .deployed overlays/dev/ssl-tls/status.json)
                        
                    fi
                    

            """)
            
        
            String nginxController = getServiceStatus("kustomize_manifests/cargador_pedidos/overlays/dev/ssl-tls")
            if(nginxController!="yes"){

                ctx.sh"""

                    export AWS_PROFILE=dev
                    export KUBECONFIG=/root/kubeconfig
                    export PATH=/root/bin:/usr/local/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/root/bin

                    
                    echo "updating nginx ingress controller"
                    cd kustomize_manifests/cargador_pedidos
                    kustomize build overlays/dev/ssl-tls > overlays/dev/dev.yaml
                    kubectl apply -f overlays/dev/dev.yaml
                    cat > overlays/dev/ssl-tls/status.json <<EOF
                    {
                        "deployed":"yes"
                    }
                    EOF
                """

            }


            ctx.sh"""
                #!/bin/bash -ex
                set +x
                mkdir -p ~/.ssh     
                echo "Host *" > ~/.ssh/config     
                echo "StrictHostKeyChecking no" >> ~/.ssh/config
                echo "${this.githubSecretKey}" > aws_terraform.pem
                chmod 400 aws_terraform.pem
                echo "${this.kubeconfig}" > /root/kubeconfig 
                eval "\$(ssh-agent -s)" && ssh-add aws_terraform.pem
                cd kustomize_manifests
                git add .
                git config user.email "${githubUsername}"
                git commit -m "updated - ${this.serviceType} - \$(date +"%Y-%m-%d %T")"
                git push origin ${branchName} 
            """
            
            
          } 

    }


    String getServiceStatus(String path){

                def script_output = ctx.sh(returnStdout: true, script: """
                    #!/bin/bash
                    set -e
                    set +x
                    value=`cat ${path}/status.json`
                    echo \$value
                    """)
                script_output = script_output.trim()
                String VAR_NAME = script_output
                def json = new JsonSlurperClassic().parseText(VAR_NAME)   
                //ctx.println(json['application'].version)         
        return json.deployed
    }

}